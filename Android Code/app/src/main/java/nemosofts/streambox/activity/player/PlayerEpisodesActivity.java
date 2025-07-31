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
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.SubtitleView;
import androidx.nemosofts.AppCompatActivity;

import java.lang.reflect.Field;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Locale;

import nemosofts.streambox.R;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.dialog.PlayerEpisodesListDialog;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.item.ItemEpisodes;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.NetworkUtils;
import nemosofts.streambox.util.helper.DBHelper;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;
import nemosofts.streambox.util.player.BrightnessVolumeControl;
import nemosofts.streambox.util.player.CustomDefaultTrackNameProvider;
import nemosofts.streambox.util.player.CustomPlayerView;

@UnstableApi
public class PlayerEpisodesActivity extends AppCompatActivity {

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

    private TextView tv_player_title;
    private LinearLayout ll_skip_next;
    private PlayerEpisodesListDialog listDialog;
    private ImageView exo_resize;

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

        listDialog = new PlayerEpisodesListDialog(this, position -> {
            Callback.playPosEpisodes = position;
            setMediaSource(dbHelper.getSeek(DBHelper.TABLE_SEEK_EPISODES,
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId(),
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle()
            ));
        });

        loadingProgressBar = findViewById(R.id.pb_player);
        tv_player_title = findViewById(R.id.tv_player_title);
        ll_skip_next = findViewById(R.id.ll_skip_next);

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
        playerView.setShowVrButton(spHelper.getIsVR());
        playerView.setShowSubtitleButton(spHelper.getIsSubtitle());
        playerView.setShowFastForwardButton(true);
        playerView.setShowRewindButton(true);
        playerView.setShowNextButton(false);
        playerView.setShowPreviousButton(false);
        playerView.setControllerHideOnTouch(false);
        playerView.setControllerAutoShow(true);
        playerView.setBrightnessControl(new BrightnessVolumeControl(PlayerEpisodesActivity.this));

        // Set controller visibility listener
        playerView.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            controllerVisible = visibility == View.VISIBLE;
            controllerVisibleFully = playerView.isControllerFullyVisible();

            findViewById(R.id.rl_player_top).setVisibility(visibility);
            if (Callback.playPosEpisodes < (Callback.arrayListEpisodes.size())) {
                ll_skip_next.setVisibility(visibility);
            }

            // https://developer.android.com/training/system-ui/immersive
            IfSupported.toggleSystemUi(PlayerEpisodesActivity.this, playerView, visibility == View.VISIBLE);
            if (isTvBox && visibility == View.VISIBLE) {
                // Because when using dpad controls, focus resets to first item in bottom controls bar
                findViewById(R.id.exo_play_pause).requestFocus();
            }
        });

        // Set custom track name provider for control view
        try {
            PlayerControlView controlView = playerView.findViewById(R.id.exo_controller);
            CustomDefaultTrackNameProvider customDefaultTrackNameProvider = new CustomDefaultTrackNameProvider(getResources());
            final Field field = PlayerControlView.class.getDeclaredField("trackNameProvider");
            field.setAccessible(true);
            field.set(controlView, customDefaultTrackNameProvider);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

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
        if (!Callback.arrayListEpisodes.isEmpty() && Callback.playPosEpisodes >= 0 && Callback.playPosEpisodes < Callback.arrayListEpisodes.size()) {
            setMediaSource(dbHelper.getSeek(DBHelper.TABLE_SEEK_EPISODES,
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId(),
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle()
            ));
        } else {
            Toasty.makeText(PlayerEpisodesActivity.this, "Invalid playPosEpisodes index: " + Callback.playPosEpisodes, Toasty.ERROR);
        }

        if (!Callback.arrayListEpisodes.isEmpty()){
            setMediaSource(dbHelper.getSeek(DBHelper.TABLE_SEEK_EPISODES,
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId(),
                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle()
            ));
        }

        // Set player event listeners
        playerListener = new PlayerListener();
        exoPlayer.addListener(playerListener);

        exo_resize = findViewById(R.id.exo_resize);
        exo_resize.setOnClickListener(firstListener);
        ll_skip_next.setOnClickListener(v -> next());

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
                updateLoading(false);
                playback = 1;
                startTimer();
            } else if (state == Player.STATE_BUFFERING) {
                updateLoading(true);
            } else if (state == Player.STATE_ENDED) {
                onCompletion();
            }
        }
        @Override
        public void onPlayerError(@NonNull PlaybackException error) {
            Player.Listener.super.onPlayerError(error);
            if (!isFinishing()){
                if (playback < 5){
                    playback = playback + 1;
                    Toast.makeText(PlayerEpisodesActivity.this,"Playback error - "+ String.valueOf(playback)+"/5 ", Toast.LENGTH_SHORT).show();
                    new Handler().postDelayed(() -> {
                        if (!isFinishing()){
                            setMediaSource(dbHelper.getSeek(DBHelper.TABLE_SEEK_EPISODES,
                                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId(),
                                    Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle()
                            ));
                        }
                    }, 600);
                } else {
                    playback = 1;
                    exoPlayer.stop();
                    updateLoading(false);
                    Toast.makeText(PlayerEpisodesActivity.this,"Failed : " + error.getErrorCodeName(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void startTimer() {
        if (!isTvBox && countDownTimer == null && Callback.reward_ad_on_episodes){
            countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {

                @Override
                public void onTick(long millisUntilFinished) {
                    timeLeftInMillis = millisUntilFinished;
                }

                @Override
                public void onFinish() {
                    if (!isFinishing()){
                        helper.showRewardAds(Callback.reward_ad_on_episodes,exoPlayer != null && exoPlayer.isPlaying(), playWhenReady1 -> {
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

    private void onCompletion() {
        removeSeekMovie();
        if (spHelper.getIsAutoplayEpisode()){
            new Handler().postDelayed(this::next, 30);
        }
    }

    private void removeSeekMovie() {
        try {
            ItemEpisodes episodes = Callback.arrayListEpisodes.get(Callback.playPosEpisodes);
            dbHelper.removeSeekID(DBHelper.TABLE_SEEK_EPISODES,episodes.getId(), episodes.getTitle());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void next() {
        if (Callback.playPosEpisodes < (Callback.arrayListEpisodes.size() - 1)) {
            Callback.playPosEpisodes = Callback.playPosEpisodes + 1;

            ItemEpisodes episodes = Callback.arrayListEpisodes.get(Callback.playPosEpisodes);
            setMediaSource(dbHelper.getSeek(DBHelper.TABLE_SEEK_EPISODES, episodes.getId(), episodes.getTitle()));
        } else {
            ll_skip_next.setVisibility(View.GONE);
        }
    }

    private void setMediaSource(int currentPosition) {
        if (NetworkUtils.isConnected(this)){
            if (!Callback.arrayListEpisodes.isEmpty() && spHelper.isLogged()){

                findViewById(R.id.exo_episodes).setOnClickListener(view -> {
                    playerView.hideController();
                    ll_skip_next.setVisibility(View.GONE);
                    listDialog.showDialog();
                });

                findViewById(R.id.iv_media_info).setOnClickListener(v -> {
                    if (exoPlayer != null && exoPlayer.getPlayWhenReady() && exoPlayer.getVideoFormat() != null){
                        playerView.hideController();
                        DialogUtil.DialogPlayerInfo(this, exoPlayer, false);
                    } else {
                        Toasty.makeText(this,getString(R.string.please_wait_a_minute), Toasty.ERROR);
                    }
                });

                tv_player_title.setText(Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle());
                String episodeUrl = spHelper.getServerURL()+"series/"+ spHelper.getUserName()+"/"+ spHelper.getPassword()+"/"+Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId()+"."+Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getContainerExtension();
                Uri uri = Uri.parse(episodeUrl);
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

                exoPlayer.seekTo(currentPosition);
                exoPlayer.prepare();
                exoPlayer.setPlayWhenReady(true);

                if (Callback.playPosEpisodes < (Callback.arrayListEpisodes.size())) {
                    ll_skip_next.setVisibility(View.VISIBLE);
                } else {
                    ll_skip_next.setVisibility(View.GONE);
                }
            }
        } else {
            Toasty.makeText(PlayerEpisodesActivity.this, getString(R.string.err_internet_not_connected), Toasty.ERROR);
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
        return new DefaultDataSourceFactory(this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        return new DefaultHttpDataSource.Factory().setUserAgent(spHelper.getAgentName().isEmpty() ? Util.getUserAgent(PlayerEpisodesActivity.this, "ExoPlayerDemo") : spHelper.getAgentName())
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

    private long getCurrentSeekPosition() {
        return exoPlayer.getCurrentPosition();
    }

    private long getFullSeekPosition() {
        int progress = 0;
        if (exoPlayer != null) {
            long currentPosition = exoPlayer.getCurrentPosition();
            long duration = exoPlayer.getDuration();
            if (duration > 0) {
                progress = (int) ((currentPosition * 100) / duration);
            }
        }
        return progress;
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_player_episodes;
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
        releasePlayer(true);
    }

    private void releasePlayer(boolean save) {
        try {
            if (batteryReceiver != null){
                unregisterReceiver(batteryReceiver);
            }
            if (exoPlayer != null) {
                notifyAudioSessionUpdate(false);

                if (mediaSession != null) {
                    mediaSession.release();
                }

                if (save) {
                    dbHelper.addToSeek(DBHelper.TABLE_SEEK_EPISODES,String.valueOf(getCurrentSeekPosition()),String.valueOf(getFullSeekPosition()),
                            Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getId(),
                            Callback.arrayListEpisodes.get(Callback.playPosEpisodes).getTitle()
                    );
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
                    // Play when ready
                    exoPlayer.setPlayWhenReady(true);
                } else {
                    // Pause if playing
                    if (exoPlayer.getPlayWhenReady()) {
                        exoPlayer.setPlayWhenReady(false);
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
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_BUTTON_L2:
                if (!controllerVisibleFully ) {
                    if (exoPlayer == null)
                        break;
                    // Seek backward by 10 seconds or go to previous track if already at start
                    if (exoPlayer.hasPrevious()) {
                        exoPlayer.seekToPrevious();
                    } else {
                        seekBy(-10 * 1000L); // Seek backward by 10 seconds
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (!controllerVisibleFully) {
                    if (exoPlayer == null)
                        break;
                    seekBy((long) -10 * 1000);
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
            case KeyEvent.KEYCODE_BUTTON_R2:
                if (!controllerVisibleFully ) {
                    if (exoPlayer == null)
                        break;
                    // Seek forward by 10 seconds or go to next track if available
                    if (exoPlayer.hasNext()) {
                        exoPlayer.seekToNext();
                    } else {
                        seekBy(10 * 1000L); // Seek forward by 10 seconds
                    }
                    return true;
                }
                break;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (!controllerVisibleFully) {
                    if (exoPlayer == null)
                        break;
                    seekBy((long) 10 * 1000);
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

    private void seekBy(long positionMs) {
        try {
            if (exoPlayer != null) {
                long currentPosition = exoPlayer.getCurrentPosition();
                long newPosition = currentPosition + positionMs;
                // Ensure the new position is within the bounds of the media duration
                long duration = exoPlayer.getDuration();
                newPosition = Math.max(0, Math.min(newPosition, duration));
                // Seek to the new position
                exoPlayer.seekTo(newPosition);
            }
        } catch (Exception e) {
            e.printStackTrace();
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