package nemosofts.streambox.activity.setting;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.nemosofts.AppCompatActivity;

import java.util.ArrayList;

import nemosofts.streambox.R;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.item.ItemRadioButton;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.helper.JSHelper;
import nemosofts.streambox.util.helper.SPHelper;

public class SettingGeneralActivity extends AppCompatActivity {

    private JSHelper jsHelper;
    private SPHelper spHelper;
    private TextView tv_recently_movie, tv_recently_live;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);

        findViewById(R.id.theme_bg).setBackgroundResource(ApplicationUtil.openThemeBg(this));

        Boolean isTvBox  = ApplicationUtil.isTvBox(this);

        findViewById(R.id.iv_back_page).setOnClickListener(view -> finish());
        if (Boolean.TRUE.equals(isTvBox)){
            findViewById(R.id.iv_back_page).setVisibility(View.GONE);
        }

        spHelper = new SPHelper(this);
        jsHelper = new JSHelper(this);

        tv_recently_movie = findViewById(R.id.tv_add_recently_movie);
        tv_recently_live = findViewById(R.id.tv_add_recently_live);

        CheckBox autoplay = findViewById(R.id.cbox_autoplay_episode);
        autoplay.setChecked(spHelper.getIsAutoplayEpisode());

        CheckBox cat_reverse = findViewById(R.id.cbox_cat_reverse);
        cat_reverse.setChecked(jsHelper.getIsCategoriesOrder());

        CheckBox splash_audio = findViewById(R.id.cbox_splash_audio);
        splash_audio.setChecked(spHelper.getIsSplashAudio());

        EditText et_agent = findViewById(R.id.et_agent);
        et_agent.setText(spHelper.getAgentName());

        findViewById(R.id.ll_recently_movie).setOnClickListener(v -> {
            ArrayList<ItemRadioButton> arrayList = new ArrayList<>();
            arrayList.add(new ItemRadioButton(10,"10 Lists"));
            arrayList.add(new ItemRadioButton(20,"20 Lists"));
            arrayList.add(new ItemRadioButton(30,"30 Lists"));
            arrayList.add(new ItemRadioButton(40,"40 Lists"));
            arrayList.add(new ItemRadioButton(50,"50 Lists"));
            arrayList.add(new ItemRadioButton(55,"55 Lists"));
            DialogUtil.RadioBtnDialog(this, arrayList, spHelper.getMovieLimit(), getString(R.string.select_recently_limit), update -> {
                spHelper.setMovieLimit(update);
                getRecently();
            });
        });
        findViewById(R.id.ll_recently_live).setOnClickListener(v -> {
            ArrayList<ItemRadioButton> arrayList = new ArrayList<>();
            arrayList.add(new ItemRadioButton(10,"10 Lists"));
            arrayList.add(new ItemRadioButton(20,"20 Lists"));
            arrayList.add(new ItemRadioButton(30,"30 Lists"));
            arrayList.add(new ItemRadioButton(40,"40 Lists"));
            arrayList.add(new ItemRadioButton(50,"50 Lists"));
            arrayList.add(new ItemRadioButton(55,"55 Lists"));
            DialogUtil.RadioBtnDialog(this, arrayList, spHelper.getLiveLimit(), getString(R.string.select_recently_limit), update -> {
                spHelper.setLiveLimit(update);
                getRecently();
            });
        });
        findViewById(R.id.ll_btn_save).setOnClickListener(v -> {
            spHelper.setAgentName(et_agent.getText().toString());
            if (!spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
                spHelper.setIsAutoplayEpisode(autoplay.isChecked());
            }
            jsHelper.setIsCategoriesOrder(cat_reverse.isChecked());
            spHelper.setIsAudio(splash_audio.isChecked());
            findViewById(R.id.tv_save).setVisibility(View.GONE);
            findViewById(R.id.pb_save).setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                findViewById(R.id.tv_save).setVisibility(View.VISIBLE);
                findViewById(R.id.pb_save).setVisibility(View.GONE);
                Toasty.makeText(SettingGeneralActivity.this, "Save Data", Toasty.SUCCESS);
            }, 500);
        });

        getRecently();

        if (spHelper.getLoginType().equals(Callback.TAG_LOGIN_PLAYLIST)){
            findViewById(R.id.ll_recently_movie).setVisibility(View.GONE);
            findViewById(R.id.ll_recently_live).setVisibility(View.GONE);
            autoplay.setVisibility(View.GONE);
        }

        if (Boolean.TRUE.equals(isTvBox)){
            findViewById(R.id.cbox_autoplay_episode).requestFocus();
        }
    }

    @SuppressLint("SetTextI18n")
    private void getRecently() {
        tv_recently_movie.setText(String.valueOf(spHelper.getMovieLimit()));
        tv_recently_live.setText(String.valueOf(spHelper.getLiveLimit()));
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_setting_general;
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