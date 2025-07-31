package nemosofts.streambox.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.metrics.PlaybackStateEvent;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.CaptioningManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import androidx.nemosofts.AppCompatActivity;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.Locale;

import nemosofts.streambox.R;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.helper.SPHelper;


@UnstableApi
public class MultipleScreenActivity extends AppCompatActivity {

    private static boolean isTvBox;
    private SPHelper spHelper;
    private String stream_id = "0";
    private DefaultBandwidthMeter BANDWIDTH_METER;
    private DataSource.Factory mediaDataSourceFactory;
    private Boolean is_player = false;

    private SimpleExoPlayer exoPlayer_one, exoPlayer_two, exoPlayer_three, exoPlayer_four;
    private ImageView add_btn_one, iv_volume_one, add_btn_two, iv_volume_two, add_btn_three, iv_volume_three, add_btn_four, iv_volume_four;
    private ProgressBar pb_one, pb_two, pb_three, pb_four;

    DefaultExtractorsFactory extractorsFactory;
    DefaultRenderersFactory renderersFactory;


    private static final CookieManager DEFAULT_COOKIE_MANAGER;
    static {
        DEFAULT_COOKIE_MANAGER = new CookieManager();
        DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);
        isTvBox = ApplicationUtil.isTvBox(this);

        findViewById(R.id.theme_bg).setBackgroundResource(ApplicationUtil.openThemeBg(this));

        spHelper = new SPHelper(this);

        Intent intent = getIntent();
        is_player = intent.getBooleanExtra("is_player", false);
        if (Boolean.TRUE.equals(is_player)){
            stream_id = intent.getStringExtra("stream_id");
        }

        BANDWIDTH_METER = new DefaultBandwidthMeter.Builder(this).build();
        mediaDataSourceFactory = buildDataSourceFactory(true);
        if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
            CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
        }

        // https://github.com/google/ExoPlayer/issues/8571
        extractorsFactory = ApplicationUtil.getDefaultExtractorsFactory();
        renderersFactory = ApplicationUtil.getDefaultRenderersFactory(this);

        initializeViews();

        if (Boolean.TRUE.equals(is_player)){
            setScreen(spHelper.getScreen());
            setPlayerOne(getChannelUrl(stream_id));
        } else {
            if (Boolean.TRUE.equals(spHelper.getIsScreen())){
                DialogUtil.ScreenDialog(MultipleScreenActivity.this, new DialogUtil.ScreenDialogListener() {
                    @Override
                    public void onSubmit(int screen) {
                        setScreen(screen);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
            } else {
                setScreen(spHelper.getScreen());
            }
            setOnClickListeners();
        }
    }

    private void initializeViews() {
        add_btn_one = findViewById(R.id.iv_add_btn_one);
        add_btn_two = findViewById(R.id.iv_add_btn_tow);
        add_btn_three = findViewById(R.id.iv_add_btn_three);
        add_btn_four = findViewById(R.id.iv_add_btn_four);
        iv_volume_one = findViewById(R.id.iv_volume_one);
        iv_volume_two = findViewById(R.id.iv_volume_tow);
        iv_volume_three = findViewById(R.id.iv_volume_three);
        iv_volume_four = findViewById(R.id.iv_volume_four);
        pb_one = findViewById(R.id.pb_one);
        pb_two = findViewById(R.id.pb_tow);
        pb_three = findViewById(R.id.pb_three);
        pb_four = findViewById(R.id.pb_four);
    }

    private void setOnClickListeners(){
        add_btn_one.setOnClickListener(v -> openFilterActivity(101));
        add_btn_two.setOnClickListener(v -> openFilterActivity(102));
        add_btn_three.setOnClickListener(v -> openFilterActivity(103));
        add_btn_four.setOnClickListener(v -> openFilterActivity(104));

        findViewById(R.id.vw_player_one).setOnClickListener(v -> setVolume(1));
        findViewById(R.id.vw_player_tow).setOnClickListener(v -> setVolume(2));
        findViewById(R.id.vw_player_three).setOnClickListener(v -> setVolume(3));
        findViewById(R.id.vw_player_four).setOnClickListener(v -> setVolume(4));

        findViewById(R.id.vw_player_one).setOnLongClickListener(v -> {
            releasePlayer(exoPlayer_one, pb_one, add_btn_one, R.id.vw_player_one, R.id.player_one, iv_volume_one);
            return true;
        });
        findViewById(R.id.vw_player_tow).setOnLongClickListener(v -> {
            releasePlayer(exoPlayer_two, pb_two, add_btn_two, R.id.vw_player_tow, R.id.player_tow, iv_volume_two);
            return true;
        });
        findViewById(R.id.vw_player_three).setOnLongClickListener(v -> {
            releasePlayer(exoPlayer_three, pb_three, add_btn_three, R.id.vw_player_three, R.id.player_three, iv_volume_three);
            return true;
        });
        findViewById(R.id.vw_player_four).setOnLongClickListener(v -> {
            releasePlayer(exoPlayer_four, pb_four, add_btn_four, R.id.vw_player_four, R.id.player_four, iv_volume_four);
            return true;
        });
    }

    private void openFilterActivity(int requestCode) {
        Intent result;
        if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
            result = new Intent(MultipleScreenActivity.this, FilterPlaylistActivity.class);
        } else {
            result = new Intent(MultipleScreenActivity.this, FilterActivity.class);
        }
        startActivityForResult(result, requestCode);
    }

    private void setVolume(int player) {
        if (exoPlayer_one != null) exoPlayer_one.setVolume(player == 1 ? 1 : 0);
        if (exoPlayer_two != null) exoPlayer_two.setVolume(player == 2 ? 1 : 0);
        if (exoPlayer_three != null) exoPlayer_three.setVolume(player == 3 ? 1 : 0);
        if (exoPlayer_four != null) exoPlayer_four.setVolume(player == 4 ? 1 : 0);

        setVolumeIcon();
    }

    private void setVolumeIcon() {
        iv_volume_one.setImageResource(exoPlayer_one != null && exoPlayer_one.getVolume() == 1f ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        iv_volume_two.setImageResource(exoPlayer_two != null && exoPlayer_two.getVolume() == 1f ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        iv_volume_three.setImageResource(exoPlayer_three != null && exoPlayer_three.getVolume() == 1f ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        iv_volume_four.setImageResource(exoPlayer_four != null && exoPlayer_four.getVolume() == 1f ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
    }

    private void setScreen(int screen) {
        if (screen == 1){
            findViewById(R.id.ll_screen_one_two).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_screen_three_four).setVisibility(View.GONE);
        } else if (screen == 2){
            findViewById(R.id.ll_screen_one_two).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_screen_three_four).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_two).setVisibility(View.GONE);
            findViewById(R.id.rl_player_four).setVisibility(View.GONE);
        } else if (screen == 3){
            findViewById(R.id.ll_screen_one_two).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_screen_three_four).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_two).setVisibility(View.GONE);
            findViewById(R.id.rl_player_four).setVisibility(View.VISIBLE);
        } else if (screen == 4){
            findViewById(R.id.ll_screen_one_two).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_screen_three_four).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_two).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_four).setVisibility(View.GONE);
        } else {
            // Default case: show all screens and players
            findViewById(R.id.ll_screen_one_two).setVisibility(View.VISIBLE);
            findViewById(R.id.ll_screen_three_four).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_two).setVisibility(View.VISIBLE);
            findViewById(R.id.rl_player_four).setVisibility(View.VISIBLE);
        }

        // Request focus for player one if running on TV Box
        if (ApplicationUtil.isTvBox(this)){
            findViewById(R.id.vw_player_one).requestFocus();
        }
    }

    @NonNull
    private String getChannelUrl(String streamId) {
        if (streamId != null && !streamId.isEmpty()) {
            if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)) {
                return streamId;
            } else {
                return spHelper.getServerURL() + (Boolean.TRUE.equals(spHelper.getIsXuiUser()) ? "" : "live/") + spHelper.getUserName() + "/" + spHelper.getPassword() + "/" + streamId + ".m3u8";
            }
        } else {
            return "";
        }
    }

    private void setPlayerOne(String channelUrl) {
        if (channelUrl != null && !channelUrl.isEmpty()){
            // Hide add button
            add_btn_one.setVisibility(View.GONE);

            // Release existing player if it exists
            if (exoPlayer_one != null) {
                exoPlayer_one.setPlayWhenReady(false);
                exoPlayer_one.stop();
                exoPlayer_one.release();
                exoPlayer_one = null; // Set to null after release
            }

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

            // Build ExoPlayer instance
            exoPlayer_one = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory))
                    .build();

            exoPlayer_one.setHandleAudioBecomingNoisy(!isTvBox);

            // Set up PlayerView for the ExoPlayer
            PlayerView playerView = findViewById(R.id.player_one);
            playerView.setPlayer(exoPlayer_one);
            playerView.setUseController(true);
            playerView.requestFocus();

            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer_one.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);

            // Add listener to ExoPlayer to handle player state changes
            exoPlayer_one.addListener(new Player.Listener(){
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);
                    if (playbackState == Player.STATE_READY) {
                        findViewById(R.id.vw_player_one).setVisibility(View.VISIBLE);
                        findViewById(R.id.player_one).setVisibility(View.VISIBLE);
                        iv_volume_one.setVisibility(View.VISIBLE);
                        is_player = false;
                    }

                    // Handle buffering state
                    if (playbackState == PlaybackStateEvent.STATE_PLAYING) {
                        pb_one.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        pb_one.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (exoPlayer_one != null) {
                        exoPlayer_one.setPlayWhenReady(false);
                        exoPlayer_one.stop();
                        exoPlayer_one.release();
                        exoPlayer_one = null; // Set to null after release
                    }
                    pb_one.setVisibility(View.GONE);
                    add_btn_one.setVisibility(View.VISIBLE);
                    findViewById(R.id.vw_player_one).setVisibility(View.INVISIBLE);
                    findViewById(R.id.player_one).setVisibility(View.GONE);
                    Toast.makeText(MultipleScreenActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Player.Listener.super.onPlayerError(error);
                }
            });

            // Prepare media source and start playing
            Uri uri = Uri.parse(channelUrl);
            MediaSource mediaSource = buildMediaSource(uri);
            exoPlayer_one.setMediaSource(mediaSource);
            exoPlayer_one.prepare();
            exoPlayer_one.setPlayWhenReady(true);
            exoPlayer_one.setVolume(0); // Mute player initially

            // If is_player is true, set volume to 1 (unmute)
            if (Boolean.TRUE.equals(is_player)){
                setVolume(1);
            }
        }
    }
    private void setPlayerTow(String channelUrl) {
        if (channelUrl != null && !channelUrl.isEmpty()){
            // Hide add button
            add_btn_two.setVisibility(View.GONE);

            // Release existing player if it exists
            if (exoPlayer_two != null) {
                exoPlayer_two.setPlayWhenReady(false);
                exoPlayer_two.stop();
                exoPlayer_two.release();
                exoPlayer_two = null; // Set to null after release
            }

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

            // Build ExoPlayer instance
            exoPlayer_two = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory))
                    .build();

            exoPlayer_two.setHandleAudioBecomingNoisy(!isTvBox);

            // Set up PlayerView for the ExoPlayer
            PlayerView playerView = findViewById(R.id.player_tow);
            playerView.setPlayer(exoPlayer_two);
            playerView.setUseController(true);
            playerView.requestFocus();

            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer_two.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);

            // Add listener to ExoPlayer to handle player state changes
            exoPlayer_two.addListener(new Player.Listener(){
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);
                    if (playbackState == Player.STATE_READY) {
                        findViewById(R.id.vw_player_tow).setVisibility(View.VISIBLE);
                        findViewById(R.id.player_tow).setVisibility(View.VISIBLE);
                        iv_volume_two.setVisibility(View.VISIBLE);
                    }

                    // Handle buffering state
                    if (playbackState == PlaybackStateEvent.STATE_PLAYING) {
                        pb_two.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        pb_two.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (exoPlayer_two != null) {
                        exoPlayer_two.setPlayWhenReady(false);
                        exoPlayer_two.stop();
                        exoPlayer_two.release();
                        exoPlayer_two = null; // Set to null after release
                    }
                    pb_two.setVisibility(View.GONE);
                    add_btn_two.setVisibility(View.VISIBLE);
                    findViewById(R.id.vw_player_tow).setVisibility(View.INVISIBLE);
                    findViewById(R.id.player_tow).setVisibility(View.GONE);
                    Toast.makeText(MultipleScreenActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Player.Listener.super.onPlayerError(error);
                }
            });

            // Prepare media source and start playing
            Uri uri = Uri.parse(channelUrl);
            MediaSource mediaSource = buildMediaSource(uri);
            exoPlayer_two.setMediaSource(mediaSource);
            exoPlayer_two.prepare();
            exoPlayer_two.setPlayWhenReady(true);
            exoPlayer_two.setVolume(0); // Mute player initially
        }
    }
    private void setPlayerThree(String channelUrl) {
        if (channelUrl != null && !channelUrl.isEmpty()){
            // Hide add button
            add_btn_three.setVisibility(View.GONE);

            // Release existing player if it exists
            if (exoPlayer_three != null) {
                exoPlayer_three.setPlayWhenReady(false);
                exoPlayer_three.stop();
                exoPlayer_three.release();
                exoPlayer_three = null; // Set to null after release
            }

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

            // Build ExoPlayer instance
            exoPlayer_three = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory))
                    .build();

            exoPlayer_three.setHandleAudioBecomingNoisy(!isTvBox);

            // Set up PlayerView for the ExoPlayer
            PlayerView playerView = findViewById(R.id.player_three);
            playerView.setPlayer(exoPlayer_three);
            playerView.setUseController(true);
            playerView.requestFocus();

            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer_three.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);

            // Add listener to ExoPlayer to handle player state changes
            exoPlayer_three.addListener(new Player.Listener(){
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);
                    if (playbackState == Player.STATE_READY) {
                        findViewById(R.id.vw_player_three).setVisibility(View.VISIBLE);
                        iv_volume_three.setVisibility(View.VISIBLE);
                        findViewById(R.id.player_three).setVisibility(View.VISIBLE);
                    }

                    // Handle buffering state
                    if (playbackState == PlaybackStateEvent.STATE_PLAYING) {
                        pb_three.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        pb_three.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (exoPlayer_three != null) {
                        exoPlayer_three.setPlayWhenReady(false);
                        exoPlayer_three.stop();
                        exoPlayer_three.release();
                        exoPlayer_three = null; // Set to null after release
                    }
                    pb_three.setVisibility(View.GONE);
                    add_btn_three.setVisibility(View.VISIBLE);
                    findViewById(R.id.vw_player_three).setVisibility(View.INVISIBLE);
                    findViewById(R.id.player_three).setVisibility(View.GONE);
                    Toast.makeText(MultipleScreenActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Player.Listener.super.onPlayerError(error);
                }
            });

            // Prepare media source and start playing
            Uri uri = Uri.parse(channelUrl);
            MediaSource mediaSource = buildMediaSource(uri);
            exoPlayer_three.setMediaSource(mediaSource);
            exoPlayer_three.prepare();
            exoPlayer_three.setPlayWhenReady(true);
            exoPlayer_three.setVolume(0); // Mute player initially
        }
    }
    private void setPlayerFour(String channelUrl) {
        if (channelUrl != null && !channelUrl.isEmpty()){
            // Hide add button
            add_btn_four.setVisibility(View.GONE);

            // Release existing player if it exists
            if (exoPlayer_four != null) {
                exoPlayer_four.setPlayWhenReady(false);
                exoPlayer_four.stop();
                exoPlayer_four.release();
                exoPlayer_four = null; // Set to null after release
            }

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

            // Build ExoPlayer instance
            exoPlayer_four = new SimpleExoPlayer.Builder(this, renderersFactory)
                    .setTrackSelector(trackSelector)
                    .setMediaSourceFactory(new DefaultMediaSourceFactory(this, extractorsFactory))
                    .build();

            exoPlayer_four.setHandleAudioBecomingNoisy(!isTvBox);

            // Set up PlayerView for the ExoPlayer
            PlayerView playerView = findViewById(R.id.player_four);
            playerView.setPlayer(exoPlayer_four);
            playerView.setUseController(true);
            playerView.requestFocus();

            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            exoPlayer_four.setVideoScalingMode(C.VIDEO_SCALING_MODE_DEFAULT);

            // Add listener to ExoPlayer to handle player state changes
            exoPlayer_four.addListener(new Player.Listener(){
                @Override
                public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                    Player.Listener.super.onPlayerStateChanged(playWhenReady, playbackState);

                    if (playbackState == Player.STATE_READY) {
                        findViewById(R.id.vw_player_four).setVisibility(View.VISIBLE);
                        iv_volume_four.setVisibility(View.VISIBLE);
                        findViewById(R.id.player_four).setVisibility(View.VISIBLE);
                    }

                    // Handle buffering state
                    if (playbackState == PlaybackStateEvent.STATE_PLAYING) {
                        pb_four.setVisibility(View.GONE);
                    } else if (playbackState == Player.STATE_BUFFERING) {
                        pb_four.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onPlayerError(@NonNull PlaybackException error) {
                    if (exoPlayer_four != null) {
                        exoPlayer_four.setPlayWhenReady(false);
                        exoPlayer_four.stop();
                        exoPlayer_four.release();
                        exoPlayer_four = null; // Set to null after release
                    }
                    pb_four.setVisibility(View.GONE);
                    add_btn_four.setVisibility(View.VISIBLE);
                    findViewById(R.id.vw_player_four).setVisibility(View.INVISIBLE);
                    findViewById(R.id.player_four).setVisibility(View.GONE);
                    Toast.makeText(MultipleScreenActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
                    Player.Listener.super.onPlayerError(error);
                }
            });

            // Prepare media source and start playing
            Uri uri = Uri.parse(channelUrl);
            MediaSource mediaSource = buildMediaSource(uri);
            exoPlayer_four.setMediaSource(mediaSource);
            exoPlayer_four.prepare();
            exoPlayer_four.setPlayWhenReady(true);
            exoPlayer_four.setVolume(0); // Mute player initially
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
        return new DefaultDataSourceFactory(MultipleScreenActivity.this, bandwidthMeter,
                buildHttpDataSourceFactory(bandwidthMeter));
    }

    public HttpDataSource.Factory buildHttpDataSourceFactory(DefaultBandwidthMeter bandwidthMeter) {
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        return new DefaultHttpDataSource.Factory().setUserAgent(spHelper.getAgentName().isEmpty() ? Util.getUserAgent(MultipleScreenActivity.this, "ExoPlayerDemo") : spHelper.getAgentName())
                .setTransferListener(bandwidthMeter)
                .setAllowCrossProtocolRedirects(true)
                .setKeepPostFor302Redirects(true);
    }

    private void releasePlayer(SimpleExoPlayer exoPlayer, ProgressBar progressBar, ImageView addButton, int volumeView, int playerView, ImageView volumeIcon) {
        try {
            if (exoPlayer != null) {
                // Stop and release the ExoPlayer instance
                exoPlayer.setPlayWhenReady(false);
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update UI elements after releasing player
        progressBar.setVisibility(View.GONE);
        addButton.setVisibility(View.VISIBLE);
        findViewById(volumeView).setVisibility(View.INVISIBLE);
        findViewById(playerView).setVisibility(View.GONE);
        volumeIcon.setVisibility(View.GONE);

        // Show a toast message indicating player release
        Toast.makeText(MultipleScreenActivity.this,"Removed", Toast.LENGTH_LONG).show();
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_multiple_screen;
    }

    @Override
    public void onStop() {
        super.onStop();
        if (exoPlayer_one != null && exoPlayer_one.getPlayWhenReady()) {
            exoPlayer_one.setPlayWhenReady(false);
            exoPlayer_one.getPlaybackState();
        }
        if (exoPlayer_two != null && exoPlayer_two.getPlayWhenReady()) {
            exoPlayer_two.setPlayWhenReady(false);
            exoPlayer_two.getPlaybackState();
        }
        if (exoPlayer_three != null && exoPlayer_three.getPlayWhenReady()) {
            exoPlayer_three.setPlayWhenReady(false);
            exoPlayer_three.getPlaybackState();
        }
        if (exoPlayer_four != null && exoPlayer_four.getPlayWhenReady()) {
            exoPlayer_four.setPlayWhenReady(false);
            exoPlayer_four.getPlaybackState();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (exoPlayer_one != null && exoPlayer_one.getPlayWhenReady()) {
            exoPlayer_one.setPlayWhenReady(false);
            exoPlayer_one.getPlaybackState();
        }
        if (exoPlayer_two != null && exoPlayer_two.getPlayWhenReady()) {
            exoPlayer_two.setPlayWhenReady(false);
            exoPlayer_two.getPlaybackState();
        }
        if (exoPlayer_three != null && exoPlayer_three.getPlayWhenReady()) {
            exoPlayer_three.setPlayWhenReady(false);
            exoPlayer_three.getPlaybackState();
        }
        if (exoPlayer_four != null && exoPlayer_four.getPlayWhenReady()) {
            exoPlayer_four.setPlayWhenReady(false);
            exoPlayer_four.getPlaybackState();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (exoPlayer_one != null) {
            exoPlayer_one.setPlayWhenReady(true);
            exoPlayer_one.getPlaybackState();
        }
        if (exoPlayer_two != null) {
            exoPlayer_two.setPlayWhenReady(true);
            exoPlayer_two.getPlaybackState();
        }
        if (exoPlayer_three != null) {
            exoPlayer_three.setPlayWhenReady(true);
            exoPlayer_three.getPlaybackState();
        }
        if (exoPlayer_four != null) {
            exoPlayer_four.setPlayWhenReady(true);
            exoPlayer_four.getPlaybackState();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (exoPlayer_one != null) {
            exoPlayer_one.setPlayWhenReady(false);
            exoPlayer_one.stop();
            exoPlayer_one.release();
        }
        if (exoPlayer_two != null) {
            exoPlayer_two.setPlayWhenReady(false);
            exoPlayer_two.stop();
            exoPlayer_two.release();
        }
        if (exoPlayer_three != null) {
            exoPlayer_three.setPlayWhenReady(false);
            exoPlayer_three.stop();
            exoPlayer_three.release();
        }
        if (exoPlayer_four != null) {
            exoPlayer_four.setPlayWhenReady(false);
            exoPlayer_four.stop();
            exoPlayer_four.release();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            String streamID = data.getStringExtra("stream_id");
            if (streamID != null && !streamID.isEmpty()){
                setPlayerOne(getChannelUrl(streamID));
            }
        } else if (requestCode == 102 && resultCode == Activity.RESULT_OK && data != null) {
            String streamID = data.getStringExtra("stream_id");
            if (streamID != null && !streamID.isEmpty()){
                setPlayerTow(getChannelUrl(streamID));
            }
        } else if (requestCode == 103 && resultCode == Activity.RESULT_OK && data != null) {
            String streamID = data.getStringExtra("stream_id");
            if (streamID != null && !streamID.isEmpty()){
                setPlayerThree(getChannelUrl(streamID));
            }
        } else if (requestCode == 104 && resultCode == Activity.RESULT_OK && data != null) {
            String streamID = data.getStringExtra("stream_id");
            if (streamID != null && !streamID.isEmpty()){
                setPlayerFour(getChannelUrl(streamID));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK){
                finish();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_HOME){
                ApplicationUtil.openHomeActivity(this);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}