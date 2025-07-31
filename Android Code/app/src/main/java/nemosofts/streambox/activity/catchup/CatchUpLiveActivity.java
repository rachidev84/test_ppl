package nemosofts.streambox.activity.catchup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.nemosofts.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;

import nemosofts.streambox.R;
import nemosofts.streambox.adapter.catchup.AdapterLiveCatchUp;
import nemosofts.streambox.item.ItemLive;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.JSHelper;
import nemosofts.streambox.view.NSoftsProgressDialog;

public class CatchUpLiveActivity extends AppCompatActivity {

    private Helper helper;
    private JSHelper jsHelper;
    private RecyclerView rv;
    private ArrayList<ItemLive> arrayList;
    private FrameLayout frameLayout;
    private NSoftsProgressDialog progressDialog;
    private AdapterLiveCatchUp adapter;

    private String cat_id = "0";

    @SuppressLint("SetTextI18n")
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

        cat_id = getIntent().getStringExtra("cat_id");
        String cat_name = getIntent().getStringExtra("cat_name");

        TextView tv_page_title = findViewById(R.id.tv_page_title);
        tv_page_title.setText(getString(R.string.catch_up_home)+" | " + cat_name);

        progressDialog = new NSoftsProgressDialog(CatchUpLiveActivity.this);

        jsHelper = new JSHelper(this);
        helper = new Helper(this, (position, type) -> {
            Intent intent = new Intent(CatchUpLiveActivity.this, CatchUpEpgActivity.class);
            intent.putExtra("stream_id", arrayList.get(position).getStreamID());
            intent.putExtra("stream_name", arrayList.get(position).getName());
            intent.putExtra("stream_icon", arrayList.get(position).getStreamIcon());
            startActivity(intent);
        });

        arrayList = new ArrayList<>();

        frameLayout = findViewById(R.id.fl_empty);
        rv = findViewById(R.id.rv);

        GridLayoutManager grid = new GridLayoutManager(this, 2);
        grid.setSpanCount(2);
        rv.setLayoutManager(grid);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setHasFixedSize(true);

        getData();
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_catch_up;
    }

    @SuppressLint("StaticFieldLeak")
    private void getData() {
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                progressDialog.show();
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    arrayList.addAll(jsHelper.getLiveCatchUpLive(cat_id));
                    if (!arrayList.isEmpty() && Boolean.TRUE.equals(jsHelper.getIsCategoriesOrder())) {
                        Collections.reverse(arrayList);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                progressDialog.dismiss();
                if (!isFinishing()){
                    if (!arrayList.isEmpty()){
                        setAdapterToListview();
                    } else {
                        setEmpty();
                    }
                }
            }
        }.execute();
    }

    public void setAdapterToListview() {
        adapter = new AdapterLiveCatchUp(arrayList, position -> helper.showInterAd(position,""));
        rv.setAdapter(adapter);

        // Set up search functionality
        setupSearchFunctionality();
    }

    private void setupSearchFunctionality() {
        EditText edt_search = findViewById(R.id.edt_search);
        edt_search.setVisibility(View.VISIBLE);
        edt_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                View currentFocus = this.getCurrentFocus();
                if (currentFocus != null) {
                    inputManager.hideSoftInputFromWindow(currentFocus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
            return true;
        });
        edt_search.addTextChangedListener(searchWatcher);
        setEmpty();
    }

    TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Not used, can be left empty
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (adapter != null) {
                adapter.getFilter().filter(s.toString());
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // Not used, can be left empty
        }
    };

    private void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
            if (ApplicationUtil.isTvBox(this)){
                rv.requestFocus();
            }
        } else {
            rv.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);

            frameLayout.removeAllViews();

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            myView.findViewById(R.id.tv_empty_msg_sub).setVisibility(View.GONE);

            frameLayout.addView(myView);
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

    @Override
    public void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()){
            progressDialog.cancel();
        }
        super.onDestroy();
    }
}