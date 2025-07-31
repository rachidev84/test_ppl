package nemosofts.streambox.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.nemosofts.AppCompatActivity;

import nemosofts.streambox.R;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;

public class WebActivity extends AppCompatActivity {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);

        findViewById(R.id.theme_bg).setBackgroundResource(ApplicationUtil.openThemeBg(this));

        Intent intent = getIntent();
        String webWRL = intent.getStringExtra("web_url");
        String page_title = intent.getStringExtra("page_title");

        findViewById(R.id.iv_back_page).setOnClickListener(view -> onBackPressed());
        if (ApplicationUtil.isTvBox(this)){
            findViewById(R.id.iv_back_page).setVisibility(View.GONE);
        }

        TextView tv_page_title = findViewById(R.id.tv_page_title);
        tv_page_title.setText(page_title);

        final ProgressBar pb = findViewById(R.id.pb_web);
        webView = findViewById(R.id.web);
        webView.setWebViewClient(new MyWebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                pb.setProgress(progress);
                if (progress == 100) {
                    pb.setVisibility(View.GONE);
                } else {
                    pb.setVisibility(View.VISIBLE);
                }
            }
        });
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        assert webWRL != null;
        webView.loadUrl(webWRL);

        if (ApplicationUtil.isTvBox(this)){
            webView.requestFocus();
        }
    }

    private static class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(@NonNull WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, @NonNull KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_BACK)) {
            onBackPressed();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_web;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}