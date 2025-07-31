package nemosofts.streambox.activity.setting;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.nemosofts.AppCompatActivity;

import nemosofts.streambox.R;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.helper.SPHelper;

public class SettingTimeFormatActivity extends AppCompatActivity {

    boolean is_12H = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);

        findViewById(R.id.theme_bg).setBackgroundResource(ApplicationUtil.openThemeBg(this));

        findViewById(R.id.iv_back_page).setOnClickListener(view -> finish());
        if (ApplicationUtil.isTvBox(this)){
            findViewById(R.id.iv_back_page).setVisibility(View.GONE);
        }

        SPHelper spHelper = new SPHelper(this);

        RadioGroup rg = findViewById(R.id.rg);
        is_12H = spHelper.getIs12Format();
        if (is_12H){
            rg.check(R.id.rd_2);
        } else {
            rg.check(R.id.rd_1);
        }

        findViewById(R.id.rd_1).setOnClickListener(view -> is_12H = false);
        findViewById(R.id.rd_2).setOnClickListener(view -> is_12H = true);
        findViewById(R.id.ll_btn_save).setOnClickListener(v -> {
            spHelper.setIs12Format(is_12H);
            findViewById(R.id.tv_save).setVisibility(View.GONE);
            findViewById(R.id.pb_save).setVisibility(View.VISIBLE);
            new Handler().postDelayed(() -> {
                findViewById(R.id.tv_save).setVisibility(View.VISIBLE);
                findViewById(R.id.pb_save).setVisibility(View.GONE);
                Toasty.makeText(SettingTimeFormatActivity.this, "Save Data", Toasty.SUCCESS);
            }, 500);
        });
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_setting_time_format;
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