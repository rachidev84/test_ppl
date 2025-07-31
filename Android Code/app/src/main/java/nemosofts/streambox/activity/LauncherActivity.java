package nemosofts.streambox.activity;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSourceFactory;
import androidx.media3.datasource.RawResourceDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.nemosofts.EnvatoProduct;
import androidx.nemosofts.LauncherListener;
import androidx.nemosofts.theme.ThemeEngine;

import nemosofts.streambox.BuildConfig;
import nemosofts.streambox.R;
import nemosofts.streambox.activity.ui.LocalStorageActivity;
import nemosofts.streambox.activity.ui.PlaylistActivity;
import nemosofts.streambox.activity.ui.SingleStreamActivity;
import nemosofts.streambox.asyncTask.LoadAbout;
import nemosofts.streambox.asyncTask.LoadData;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.interfaces.AboutListener;
import nemosofts.streambox.interfaces.DataListener;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.NetworkUtils;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;

public class LauncherActivity extends AppCompatActivity implements LauncherListener {

    Helper helper;
    SPHelper spHelper;
    private ProgressBar pb;
    private ExoPlayer exoPlayer = null;
    private int delayMillis = 3500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);
        IfSupported.keepScreenOn(this);

        helper = new Helper(this);
        spHelper = new SPHelper(this);

        int theme = spHelper.getIsTheme();
        if (theme == 2){
            findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_ui_glossy);
        } else if (theme == 3){
            findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_dark_panther);
        } else {
            int themePage = new ThemeEngine(this).getThemePage();
            if (themePage == 0){
                findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_dark);
            } else if (themePage == 1){
                findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_classic);
            } else if (themePage == 2){
                findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_grey);
            } else if (themePage == 3){
                findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_blue);
            } else {
                findViewById(R.id.theme_bg).setBackgroundResource(R.drawable.bg_dark);
            }
        }

        pb = findViewById(R.id.pb_splash);

        prepareAudio();
        loadAboutData();
    }

    private void loadAboutData() {
        if (NetworkUtils.isConnected(this)){
            LoadAbout loadAbout = new LoadAbout(LauncherActivity.this, new AboutListener() {
                @Override
                public void onStart() {
                    pb.setVisibility(View.VISIBLE);
                }

                @Override
                public void onEnd(String success, String verifyStatus, String message){
                    pb.setVisibility(View.GONE);
                    if (success.equals("1")){
                        setSaveData();
                    } else {
                        if (Boolean.TRUE.equals(spHelper.getIsAboutDetails())){
                            setSaveData();
                        } else {
                            errorDialog(getString(R.string.err_server_error), getString(R.string.err_server_not_connected));
                        }
                    }
                }
            });
            loadAbout.execute();
        } else {
            if (Boolean.TRUE.equals(spHelper.getIsAboutDetails())){
                setSaveData();
            } else {
                errorDialog(getString(R.string.err_internet_not_connected), getString(R.string.err_connect_net_try));
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void prepareAudio() {
        if (Boolean.TRUE.equals(spHelper.getIsSplashAudio())){
            exoPlayer = new ExoPlayer.Builder(this).build();
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "nemosofts_rc"));
            Uri fileUri = RawResourceDataSource.buildRawResourceUri(R.raw.opener_logo);
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(fileUri));
            exoPlayer.setMediaSource(mediaSource);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(false);
        } else {
            delayMillis = 2000;
        }
    }

    private void playAudio() {
        if (Boolean.TRUE.equals(spHelper.getIsSplashAudio()) && exoPlayer != null){
            exoPlayer.play();
        }
    }

    private void loadSettings() {
        if (Boolean.FALSE.equals(spHelper.getIsAboutDetails())){
            spHelper.setAboutDetails(true);
        }
        if (Boolean.TRUE.equals(Callback.isAppUpdate) && Callback.app_new_version != BuildConfig.VERSION_CODE){
            openDialogActivity(Callback.DIALOG_TYPE_UPDATE);
        } else if(Boolean.TRUE.equals(spHelper.getIsMaintenance())){
            openDialogActivity(Callback.DIALOG_TYPE_MAINTENANCE);
        } else {
            if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_SINGLE_STREAM)){
                playAudio();
                new Handler().postDelayed(this::openSingleStream, delayMillis);
            } else if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_VIDEOS)){
                playAudio();
                new Handler().postDelayed(this::openVideos, delayMillis);
            }
            else if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
                playAudio();
                new Handler().postDelayed(this::openPlaylistActivity, delayMillis);
            }
            else if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_ONE_UI) || spHelper.getLoginType().equals(Callback.TAG_LOGIN_STREAM)){
                if (Boolean.TRUE.equals(spHelper.getIsFirst())) {
                    playAudio();
                    new Handler().postDelayed(this::openSelectPlayer, delayMillis);
                } else {
                    if (Boolean.FALSE.equals(spHelper.getIsAutoLogin())) {
                        playAudio();
                        new Handler().postDelayed(this::openSelectPlayer, delayMillis);
                    } else {
                        get_data();
                    }
                }
            } else {
                playAudio();
                new Handler().postDelayed(this::openSelectPlayer, delayMillis);
            }
        }
    }

    private void get_data() {
        if (NetworkUtils.isConnected(this)){
            LoadData loadData = new LoadData(this, new DataListener() {
                @Override
                public void onStart() {
                    pb.setVisibility(View.VISIBLE);
                }

                @Override
                public void onEnd(String success) {
                    if (!isFinishing()){
                        pb.setVisibility(View.GONE);
                        if (Boolean.TRUE.equals(spHelper.getIsSplashAudio())){
                            playAudio();
                            new Handler().postDelayed(()-> ApplicationUtil.openThemeActivity(LauncherActivity.this), delayMillis);
                        } else {
                            ApplicationUtil.openThemeActivity(LauncherActivity.this);
                        }
                    }
                }
            });
            loadData.execute();
        } else {
            if (Boolean.TRUE.equals(spHelper.getIsSplashAudio())){
                playAudio();
                new Handler().postDelayed(()-> ApplicationUtil.openThemeActivity(LauncherActivity.this), delayMillis);
            } else {
                ApplicationUtil.openThemeActivity(LauncherActivity.this);
            }
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    private void openSelectPlayer() {
        Intent intent = new Intent(LauncherActivity.this, SelectPlayerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", "");
        startActivity(intent);
        finish();
    }

    @OptIn(markerClass = UnstableApi.class)
    private void openSingleStream() {
        Intent intent = new Intent(LauncherActivity.this, SingleStreamActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void openVideos() {
        Intent intent = new Intent(LauncherActivity.this, LocalStorageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void openPlaylistActivity() {
        Intent intent = new Intent(LauncherActivity.this, PlaylistActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }

    private void openDialogActivity(String type) {
        Intent intent = new Intent(LauncherActivity.this, DialogActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("from", type);
        startActivity(intent);
        finish();
    }

    @Override
    public void onStartPairing() {
        pb.setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnected() {
        pb.setVisibility(View.GONE);
        loadSettings();
    }

    @Override
    public void onUnauthorized(String message) {
        pb.setVisibility(View.GONE);
        errorDialog(getString(R.string.err_unauthorized_access), message);
    }

    @Override
    public void onError() {
        pb.setVisibility(View.GONE);
        errorDialog(getString(R.string.err_server_error), getString(R.string.err_server_not_connected));
    }

    private void errorDialog(String title, String message) {
        if (isFinishing()) {
            // Activity is finishing, no need to show dialog
            return;
        }

        runOnUiThread(() -> {
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(LauncherActivity.this, R.style.ThemeDialog);
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.setCancelable(false);

            boolean is_internet;
            if (title.equals(getString(R.string.err_internet_not_connected))) {
                is_internet = true;
                alertDialog.setNegativeButton(getString(R.string.retry), (dialog, which) -> loadSettings());
            } else {
                is_internet = false;
            }

            alertDialog.setOnKeyListener((dialog1, keyCode, event) -> {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK){
                        finish();
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER){
                        if (is_internet) {
                            loadSettings();
                        } else {
                            finish();
                        }
                    }
                    return true; // Returning true to consume the event
                }
                return false;
            });

            alertDialog.setPositiveButton(getString(R.string.exit), (dialog, which) -> finish());
            alertDialog.show();
        });
    }

    private void setSaveData() {
        new EnvatoProduct(LauncherActivity.this, LauncherActivity.this).execute();
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_BACK)){
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}