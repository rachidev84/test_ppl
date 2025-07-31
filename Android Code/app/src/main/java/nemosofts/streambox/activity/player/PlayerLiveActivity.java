package nemosofts.streambox.activity.player;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.media.audiofx.LoudnessEnhancer;
import android.media.metrics.PlaybackStateEvent;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.HttpDataSource;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.media3.exoplayer.dash.DashMediaSource;
import androidx.media3.exoplayer.dash.DefaultDashChunkSource;
import androidx.media3.exoplayer.hls.HlsMediaSource;
import androidx.media3.exoplayer.rtsp.RtspMediaSource;
import androidx.media3.exoplayer.smoothstreaming.DefaultSsChunkSource;
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter;
import androidx.media3.extractor.DefaultExtractorsFactory;
import androidx.media3.session.MediaSession;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.CaptionStyleCompat;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.nemosofts.AppCompatActivity;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.ArrayList;
import java.util.Locale;

import nemosofts.streambox.R;
import nemosofts.streambox.activity.MultipleScreenActivity;
import nemosofts.streambox.asyncTask.LoadEpg;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.dialog.PlayerLiveListDialog;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.interfaces.EpgListener;
import nemosofts.streambox.item.ItemEpg;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.NetworkUtils;
import nemosofts.streambox.util.helper.DBHelper;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;
import nemosofts.streambox.util.player.BrightnessVolumeControl;
import nemosofts.streambox.util.player.CustomPlayerView;

@UnstableApi
public class PlayerLiveActivity extends AppCompatActivity {

    private Helper helper;
    private DBHelper dbHelper;
    private SPHelper spHelper;

    private PlayerListener playerListener;
    private MediaSession mediaSession;
    public static LoudnessEnhancer loudnessEnhancer;
    private BroadcastReceiver batteryReceiver;
    private AudioManager mAudioManager;
    public static int boostLevel = 0;
    private static boolean isTvBox;

    public CustomPlayerView playerView;
    public static SimpleExoPlayer exoPlayer;
    private DefaultBandwidthMeter BANDWIDTH_METER;
    private DataSource.Factory mediaDataSourceFactory;
    private ProgressBar loadingProgressBar;

    private int playback = 0;

    private ImageView iv_play, iv_player_fav, btnTryAgain;
    private TextView tv_player_title;
    private PlayerLiveListDialog listDialog;
    private LinearLayout exo_resize;
    private RelativeLayout rl_player_epg;
    private TextView tv_epg_title, tv_epg_time;
    private ImageView iv_previous, iv_next;

    // RewardAd
    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;

    public static boolean controllerVisible;
    public static boolean controllerVisibleFully;

    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideBottomBar(this);
        IfSupported.statusBarBlackColor(this);
        isTvBox = ApplicationUtil.isTvBox(this);

        timeLeftInMillis = Callback.reward_minutes * 60 * 1000; // minutes in milliseconds

        helper = new Helper(this);
        spHelper = new SPHelper(this);
        dbHelper = new DBHelper(this);

        listDialog = new PlayerLiveListDialog(this, position -> {
            Callback.playPosLive = position;
            setMediaSource();
        });

        tv_player_title = findViewById(R.id.tv_player_title);
        loadingProgressBar = findViewById(R.id.pb_player);
        btnTryAgain = findViewById(R.id.iv_reset);
        iv_play = findViewById(R.id.iv_play);
        iv_player_fav = findViewById(R.id.iv_player_fav);
        rl_player_epg = findViewById(R.id.rl_player_epg);
        tv_epg_title = findViewById(R.id.tv_epg_title);
        tv_epg_time = findViewById(R.id.tv_epg_time);

        iv_previous = findViewById(R.id.iv_previous);
        iv_next = findViewById(R.id.iv_next);

        BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(this).build();
        mediaDataSourceFactory = buildDataSourceFactory(true);

        // Set default cookie manager if not already set
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        // https://github.com/google/ExoPlayer/issues/8571
        DefaultExtractorsFactory extractorsFactory = ApplicationUtil.getDefaultExtractorsFactory();
        DefaultRenderersFactory renderersFactory = ApplicationUtil.getDefaultRenderersFactory(this);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
        // Set captioning parameters if enabled
        final CaptioningManager captioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
        if (!captioningManager.isEnabled()) {
            trackSelector.setParameters(trackSelector.buildUponParameters().setIgnoredTextSelectionFlags(C.SELECTION_FLAG_DEFAULT));
        }
        Locale locale = captioningManager.getLocale();
        if (locale != null) {
            trackSelector.setParameters(trackSelector.buildUponParameters().setPreferredTextLanguage(locale.getISO3Language()));
        }

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // Build ExoPlayer instance
        exoPlayer = new SimpleExoPlayer.Builder(this, renderersFactory)
                .setTrackSelector(trackSelector)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory))
                .build();

        // Set audio attributes for the player
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build();
        exoPlayer.setAudioAttributes(audioAttributes, true);

        exoPlayer.setHandleAudioBecomingNoisy(!isTvBox);

        if (mediaSession != null) {
            mediaSession.release();
        }
        if (exoPlayer.canAdvertiseSession()) {
            try {
                mediaSession = new MediaSession.Builder(this, exoPlayer).build();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }

        // Attach ExoPlayer to the player view
        playerView = findViewById(R.id.nSoftsPlayerView);
        playerView.setPlayer(exoPlayer);
        playerView.setUseController(true);
        playerView.requestFocus();
        playerView.setControllerHideOnTouch(false);
        playerView.setControllerAutoShow(true);
        playerView.setBrightnessControl(new BrightnessVolumeControl(this));

        // Set controller visibility listener
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            controllerVisible = visibility == View.VISIBLE;
            controllerVisibleFully = playerView.isControllerFullyVisible();

            // https://developer.android.com/training/system-ui/immersive
            IfSupported.toggleSystemUi(PlayerLiveActivity.this, playerView, visibility == View.VISIBLE);
            if (isTvBox && visibility == View.VISIBLE) {
                // Because when using dpad controls, focus resets to first item in bottom controls bar
                iv_play.requestFocus();
            }
        });

        // Set custom subtitle view style
        try {
            final CaptioningManager mCaptioningManager = (CaptioningManager) getSystemService(Context.CAPTIONING_SERVICE);
            final SubtitleView subtitleView = playerView.getSubtitleView();
            final boolean isTablet = ApplicationUtil.isTablet(this);
            float subtitlesScale = ApplicationUtil.normalizeFontScale(captioningManager.getFontScale(), isTvBox || isTablet);
            if (subtitleView != null) {
                final CaptioningManager.CaptionStyle userStyle = mCaptioningManager.getUserStyle();
                final CaptionStyleCompat userStyleCompat = CaptionStyleCompat.createFromCaptionStyle(userStyle);
                final CaptionStyleCompat captionStyle = new CaptionStyleCompat(
                        userStyle.hasForegroundColor() ? userStyleCompat.foregroundColor : Color.WHITE,
                        userStyle.hasBackgroundColor() ? userStyleCompat.backgroundColor : Color.TRANSPARENT,
                        userStyle.hasWindowColor() ? userStyleCompat.windowColor : Color.TRANSPARENT,
                        userStyle.hasEdgeType() ? userStyleCompat.edgeType : CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                        userStyle.hasEdgeColor() ? userStyleCompat.edgeColor : Color.BLACK,
                        Typeface.create(userStyleCompat.typeface != null ? userStyleCompat.typeface : Typeface.DEFAULT, Typeface.NORMAL));
                subtitleView.setStyle(captionStyle);
                subtitleView.setApplyEmbeddedStyles(false);
                subtitleView.setBottomPaddingFraction(SubtitleView.DEFAULT_BOTTOM_PADDING_FRACTION * 2f / 3f);

                // Tweak text size as fraction size doesn't work well in portrait
                final float size = ApplicationUtil.getaFloat(this, subtitlesScale);
                subtitleView.setFractionalTextSize(size);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Set media source
        setMediaSource();

        // Set player event listeners
        playerListener = new PlayerListener();
        exoPlayer.addListener(playerListener);

        btnTryAgain.setOnClickListener(v -> {
            btnTryAgain.setVisibility(View.GONE);
            iv_previous.setVisibility(View.VISIBLE);
            iv_next.setVisibility(View.VISIBLE);
            updateLoading(true);
            setMediaSource();
        });

        exo_resize = findViewById(R.id.ll_aspect_ratio);
        exo_resize.setOnClickListener(firstListener);

        iv_play.setOnClickListener(v -> {
            exoPlayer.setPlayWhenReady(!exoPlayer.getPlayWhenReady());
            iv_play.setImageResource(Boolean.TRUE.equals(exoPlayer.getPlayWhenReady()) ? R.drawable.ic_pause : R.drawable.ic_play);
        });
        if (isTvBox){
            iv_play.requestFocus();
        }

        iv_player_fav.setOnClickListener(v -> {
            if (Boolean.TRUE.equals(dbHelper.checkLive(DBHelper.TABLE_FAV_LIVE, Callback.arrayListLive.get(Callback.playPosLive).getStreamID()))){
                dbHelper.removeLive(DBHelper.TABLE_FAV_LIVE, Callback.arrayListLive.get(Callback.playPosLive).getStreamID());
                iv_player_fav.setImageResource(R.drawable.ic_favorite_border);
                Toast.makeText(PlayerLiveActivity.this, getString(R.string.fav_remove_success), Toast.LENGTH_SHORT).show();
            } else {
                dbHelper.addToLive(DBHelper.TABLE_FAV_LIVE, Callback.arrayListLive.get(Callback.playPosLive), 0);
                iv_player_fav.setImageResource(R.drawable.ic_favorite);
                Toast.makeText(PlayerLiveActivity.this, getString(R.string.fav_success), Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.iv_media_info).setOnClickListener(v -> {
            if (exoPlayer != null && exoPlayer.getPlayWhenReady() && exoPlayer.getVideoFormat() != null){
                playerView.hideController();
                DialogUtil.DialogPlayerInfo(this, exoPlayer, true);
            } else {
                Toasty.makeText(this,getString(R.string.please_wait_a_minute), Toasty.ERROR);
            }
        });

        if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
            iv_player_fav.setVisibility(View.INVISIBLE);
        }
        findViewById(R.id.ll_multiple_screen).setOnClickListener(v -> {
            Intent intent = new Intent(PlayerLiveActivity.this, MultipleScreenActivity.class);
            intent.putExtra("is_player", true);
            intent.putExtra("stream_id", Callback.arrayListLive.get(Callback.playPosLive).getStreamID());
            startActivity(intent);
            finish();
        });

        iv_previous.setOnClickListener(v -> previous());
        iv_next.setOnClickListener(v -> next());

        ImageView battery_info = findViewById(R.id.iv_battery_info);
        if (!isTvBox){
            batteryReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                    int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                    battery_info.setImageResource(ApplicationUtil.getBatteryDrawable(status,level,scale));
                }
            };
            IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            registerReceiver(batteryReceiver, filter);
        } else {
            battery_info.setVisibility(View.INVISIBLE);
        }

        findViewById(R.id.iv_back_player).setOnClickListener(v -> onBackPressed());
        if (isTvBox){
            findViewById(R.id.iv_back_player).setVisibility(View.GONE);
        }
    }

    private class PlayerListener implements Player.Listener {

        @Override
        public void onAudioSessionIdChanged(int audioSessionId) {
            Player.Listener.super.onAudioSessionIdChanged(audioSessionId);
            try {
                if (loudnessEnhancer != null) {
                    loudnessEnhancer.release();
                }
                loudnessEnhancer = new LoudnessEnhancer(audioSessionId);
            } catch (Exception e) {
                e.printStackTrace();
            }
            notifyAudioSessionUpdate(true);
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            Player.Listener.super.onIsPlayingChanged(isPlaying);
            playerView.setKeepScreenOn(isPlaying);
        }

        @Override
        public void onPlaybackStateChanged(int state) {
            Player.Listener.super.onPlaybackStateChanged(state);
            if (state == PlaybackStateEvent.STATE_PLAYING) {
                iv_play.setImageResource(R.drawable.ic_pause);
                updateLoading(false);
                playback = 1;
                startTimer();
            } else if (state == Player.STATE_BUFFERING) {
                updateLoading(true);
            }
        }
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            if (!isFinishing()){
                if (playback < 5){
                    playback = playback + 1;
                    Toast.makeText(PlayerLiveActivity.this,"Playback error - "+ String.valueOf(playback)+"/5 ", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        if (!isFinishing()){
                            setMediaSource();
                        }
                    }, 600);
                } else {
                    playback = 1;
                    exoPlayer.stop();
                    btnTryAgain.setVisibility(View.VISIBLE);
                    iv_previous.setVisibility(View.GONE);
                    iv_next.setVisibility(View.GONE);
                    iv_play.setImageResource(R.drawable.ic_play);
                    iv_play.setVisibility(View.GONE);
                    updateLoading(false);
                    Toast.makeText(PlayerLiveActivity.this,"Failed : " + error.getErrorCodeName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startTimer() {
        if (!isTvBox && countDownTimer == null && Callback.reward_ad_on_live){
            countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    if (!isFinishing()){
                        helper.showRewardAds(Callback.reward_ad_on_live,exoPlayer != null && exoPlayer.isPlaying(), playWhenReady1 -> {
                        });
                    }
                }
            }.start();
        }
    }

    void notifyAudioSessionUpdate(final boolean active) {
        final Intent intent = new Intent(active ? AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION : AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION);
        intent.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, exoPlayer.getAudioSessionId());
        intent.putExtra(AudioEffect.EXTRA_PACKAGE_NAME, getPackageName());
        if (active) {
            intent.putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MOVIE);
        }
        try {
            sendBroadcast(intent);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateLoading(final boolean enableLoading) {
        if (enableLoading) {
            loadingProgressBar.setVisibility(View.VISIBLE);
        } else {
            loadingProgressBar.setVisibility(View.GONE);
        }
    }

    public void next() {
        if (!Callback.arrayListLive.isEmpty()) {
            if (NetworkUtils.isConnected(this)) {
                if (Callback.playPosLive < (Callback.arrayListLive.size() - 1)) {
                    Callback.playPosLive = Callback.playPosLive + 1;
                } else {
                    Callback.playPosLive = 0;
                }
                setMediaSource();
            } else {
                Toasty.makeText(PlayerLiveActivity.this, getString(R.string.err_internet_not_connected), Toasty.ERROR);
            }
        }
    }

    public void previous() {
        if (!Callback.arrayListLive.isEmpty()) {
            if (NetworkUtils.isConnected(this)) {
                if (Callback.playPosLive > 0) {
                    Callback.playPosLive = Callback.playPosLive - 1;
                } else {
                    Callback.playPosLive = Callback.arrayListLive.size() - 1;
                }
                setMediaSource();
            } else {
                Toasty.makeText(PlayerLiveActivity.this, getString(R.string.err_internet_not_connected), Toasty.ERROR);
            }
        }
    }

    private void setMediaSource() {
        if (NetworkUtils.isConnected(this)){
            if (!Callback.arrayListLive.isEmpty() && spHelper.isLogged() || spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){

                findViewById(R.id.ll_channels_list).setOnClickListener(view -> {
                    playerView.hideController();
                    listDialog.showDialog();
                });

                if (btnTryAgain.getVisibility() == View.VISIBLE){
                    btnTryAgain.setVisibility(View.GONE);
                    iv_previous.setVisibility(View.VISIBLE);
                    iv_next.setVisibility(View.VISIBLE);
                }

                tv_player_title.setText(Callback.arrayListLive.get(Callback.playPosLive).getName());
                if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_ONE_UI) || spHelper.getLoginType().equals(Callback.TAG_LOGIN_STREAM)){
                    iv_player_fav.setImageResource(Boolean.TRUE.equals(dbHelper.checkLive(DBHelper.TABLE_FAV_LIVE, Callback.arrayListLive.get(Callback.playPosLive).getStreamID())) ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
                }

                String channelUrl;
                if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
                    channelUrl = Callback.arrayListLive.get(Callback.playPosLive).getStreamID();
                    rl_player_epg.setVisibility(View.GONE);
                } else {
                    String format = ".m3u8";
                    if (spHelper.getLiveFormat() == 1){
                        format = ".ts";
                    }
                    if (Boolean.TRUE.equals(spHelper.getIsXuiUser())){
                        channelUrl = spHelper.getServerURL()+ spHelper.getUserName()+"/"+ spHelper.getPassword()+"/"+Callback.arrayListLive.get(Callback.playPosLive).getStreamID()+format;
                    } else {
                        channelUrl = spHelper.getServerURL()+"live/"+ spHelper.getUserName()+"/"+ spHelper.getPassword()+"/"+Callback.arrayListLive.get(Callback.playPosLive).getStreamID()+format;
                    }
                    if (playback == 0 || playback == 1){
                        getEpgData();
                    }
                }

                Uri uri = Uri.parse(channelUrl);
                MediaSource mediaSource = buildMediaSource(uri);
                exoPlayer.setMediaSource(mediaSource);

                try {
                    if (loudnessEnhancer != null) {
                        loudnessEnhancer.release();
                    }
                    loudnessEnhancer = new LoudnessEnhancer(exoPlayer.getAudioSessionId());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                notifyAudioSessionUpdate(true);

                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(true);
                iv_play.setImageResource(R.drawable.ic_pause);
                iv_play.setVisibility(View.VISIBLE);

                if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_ONE_UI) || spHelper.getLoginType().equals(Callback.TAG_LOGIN_STREAM)){
                    try {
                        dbHelper.addToLive(DBHelper.TABLE_RECENT_LIVE, Callback.arrayListLive.get(Callback.playPosLive), spHelper.getLiveLimit());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Toasty.makeText(PlayerLiveActivity.this, getString(R.string.err_internet_not_connected), Toasty.ERROR);
        }
    }

    private void getEpgData() {
        if (NetworkUtils.isConnected(this)){
            LoadEpg loadSeriesID = new LoadEpg(this, new EpgListener() {
                @Override
                public void onStart() {
                    rl_player_epg.setVisibility(View.GONE);
                    tv_epg_title.setText("");
                    tv_epg_time.setText("");
                }

                @SuppressLint("SetTextI18n")
                @Override
                public void onEnd(String success, ArrayList<ItemEpg> epgArrayList) {
                    if (!isFinishing()){
                        if (!epgArrayList.isEmpty()){
                            tv_epg_title.setText(ApplicationUtil.decodeBase64(epgArrayList.get(0).getTitle()));
                            tv_epg_time.setText(ApplicationUtil.getTimestamp(epgArrayList.get(0).getStartTimestamp(), spHelper.getIs12Format()) + " - " + ApplicationUtil.getTimestamp(epgArrayList.get(0).getStopTimestamp(), spHelper.getIs12Format()));
                            rl_player_epg.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }, helper.getAPIRequestID("get_short_epg","stream_id", Callback.arrayListLive.get(Callback.playPosLive).getStreamID(), spHelper.getUserName(), spHelper.getPassword()));
            loadSeriesID.execute();
        }
    }

    @SuppressLint("SwitchIntDef")
    @NonNull
    private MediaSource buildMediaSource(Uri uri) {
        int contentType  = Util.inferContentType(uri);
        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(uri)
                .build();
        return switch (contentType) {
            case C.TYPE_DASH ->
                    new DashMediaSource.Factory(new DefaultDashChunkSource.Factory(mediaDataSourceFactory), buildDataSourceFactory(false))
                            .createMediaSource(mediaItem);
            case C.TYPE_SS ->
                    new SsMediaSource.Factory(new DefaultSsChunkSource.Factory(mediaDataSourceFactory), buildDataSourceFactory(false))
                            .createMediaSource(mediaItem);
            case C.TYPE_HLS ->
                    new HlsMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
            case C.TYPE_RTSP ->
                    new RtspMediaSource.Factory()
                            .createMediaSource(mediaItem);
            case C.TYPE_OTHER ->
                    new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
            default ->
                // This is the MediaSource representing the media to be played.
                    new ProgressiveMediaSource.Factory(mediaDataSourceFactory)
                            .createMediaSource(mediaItem);
        };
    }

    private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
        return buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
    }

    public DataSource.Factory buildDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        return new DefaultDataSourceFactory(PlayerLiveActivity.this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        return new DefaultHttpDataSource.Factory().setUserAgent(spHelper.getAgentName().isEmpty() ? Util.getUserAgent(PlayerLiveActivity.this, "ExoPlayerDemo") : spHelper.getAgentName())
                .setTransferListener(bandwidthMeter)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);
    }

    View.OnClickListener firstListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FILL);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Full Scree");
            exo_resize.setOnClickListener(secondListener);
        }
    };
    View.OnClickListener secondListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Zoom");
            exo_resize.setOnClickListener(thirdListener);
        }
    };
    View.OnClickListener thirdListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);
            playerView.showController();
            ApplicationUtil.showText(playerView, "Fit");
            exo_resize.setOnClickListener(firstListener);
        }
    };

    @Override
    public int setContentViewID() {
        return R.layout.activity_player_live;
    }

    @Override
    public void onStop() {
        super.onStop();
        playWhenReady(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();
        playWhenReady(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        playWhenReady(true);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        playWhenReady(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        releasePlayer();
    }

    private void releasePlayer() {
        try {
            if (batteryReceiver != null){
                unregisterReceiver(batteryReceiver);
            }
            if (exoPlayer != null) {
                notifyAudioSessionUpdate(false);

                if (mediaSession != null) {
                    mediaSession.release();
                }

                exoPlayer.removeListener(playerListener);
                exoPlayer.clearMediaItems();
                exoPlayer.release();
                exoPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playWhenReady(boolean setPlayWhenReady) {
        try {
            if (exoPlayer != null) {
                if (setPlayWhenReady){
                    exoPlayer.setPlayWhenReady(true);
                    exoPlayer.getPlaybackState();
                } else {
                    if (exoPlayer.getPlayWhenReady()){
                        exoPlayer.setPlayWhenReady(false);
                        exoPlayer.getPlaybackState();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                next();
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                previous();
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
            case KeyEvent.KEYCODE_BUTTON_SELECT:
                if (exoPlayer == null)
                    break;
                if (keyCode == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                    exoPlayer.pause();
                } else if (keyCode == KeyEvent.KEYCODE_MEDIA_PLAY) {
                    exoPlayer.play();
                } else if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                } else {
                    exoPlayer.play();
                }
                return true;
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_HEADSETHOOK:
                if (exoPlayer == null)
                    break;
                if (exoPlayer.isPlaying()) {
                    exoPlayer.pause();
                } else {
                    exoPlayer.play();
                }
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                adjustVolume(keyCode == KeyEvent.KEYCODE_VOLUME_UP, event.getRepeatCount() == 0, true);
                return true;
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_BUTTON_A:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
            case KeyEvent.KEYCODE_SPACE:
                if (exoPlayer == null)
                    break;
                if (!controllerVisibleFully) {
                    if (exoPlayer.isPlaying()) {
                        exoPlayer.pause();
                    } else {
                        exoPlayer.play();
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_BACK:
                if (ApplicationUtil.isTvBox(this)) {
                    if (controllerVisible && exoPlayer != null && exoPlayer.isPlaying()) {
                        playerView.hideController();
                        return true;
                    } else {
                        finish();
                    }
                }
                break;
            case KeyEvent.KEYCODE_UNKNOWN:
                return super.onKeyDown(keyCode, event);
            default:
                if (!controllerVisibleFully) {
                    playerView.showController();
                    return true;
                }
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                playerView.postDelayed(playerView.textClearRunnable, CustomPlayerView.MESSAGE_TIMEOUT_KEY);
                yield true;
            }
            default -> super.onKeyUp(keyCode, event);
        };
    }

    private void adjustVolume(final boolean raise, boolean canBoost, boolean clear) {
        playerView.removeCallbacks(playerView.textClearRunnable);

        final int volume = ApplicationUtil.getVolume(this,false, mAudioManager);
        final int volumeMax = ApplicationUtil.getVolume(this,true, mAudioManager);
        boolean volumeActive = volume != 0;

        // Handle volume changes outside the app (lose boost if volume is not maxed out)
        if (volume != volumeMax) {
            boostLevel = 0;
        }

        if (loudnessEnhancer == null)
            canBoost = false;

        if (volume != volumeMax || (boostLevel == 0 && !raise)) {
            if (loudnessEnhancer != null) {
                try {
                    loudnessEnhancer.setEnabled(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, raise ? AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
            final int volumeNew = ApplicationUtil.getVolume(this, false, mAudioManager);
            // Custom volume step on Samsung devices (Sound Assistant)
            if (raise && volume == volumeNew) {
                playerView.volumeUpsInRow++;
            } else {
                playerView.volumeUpsInRow = 0;
            }
            if (playerView.volumeUpsInRow > 4 && !ApplicationUtil.isVolumeMin(mAudioManager)) {
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE | AudioManager.FLAG_SHOW_UI);
            } else {
                volumeActive = volumeNew != 0;
                playerView.setCustomErrorMessage(volumeActive ? " " + volumeNew : "");
            }
        } else {
            if (canBoost && raise && boostLevel < 10){
                boostLevel++;
            } else if (!raise && boostLevel > 0){
                boostLevel--;
            }

            if (loudnessEnhancer != null) {
                try {
                    loudnessEnhancer.setTargetGain(boostLevel * 200);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            playerView.setCustomErrorMessage(" " + (volumeMax + boostLevel));
        }

        playerView.setIconVolume(volumeActive);
        if (loudnessEnhancer != null) {
            try {
                loudnessEnhancer.setEnabled(boostLevel > 0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        playerView.setHighlight(boostLevel > 0);

        if (clear) {
            playerView.postDelayed(playerView.textClearRunnable, CustomPlayerView.MESSAGE_TIMEOUT_KEY);
        }
    }

    @Override
    public void onBackPressed() {
        if (listDialog.isShowing()) {
            listDialog.dismissDialog();
        } else {
            super.onBackPressed();
        }
    }
}