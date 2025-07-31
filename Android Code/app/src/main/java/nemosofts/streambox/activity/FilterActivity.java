package nemosofts.streambox.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.nemosofts.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Objects;

import nemosofts.streambox.R;
import nemosofts.streambox.adapter.AdapterCategory;
import nemosofts.streambox.adapter.AdapterLiveTV;
import nemosofts.streambox.asyncTask.GetCategory;
import nemosofts.streambox.asyncTask.GetLive;
import nemosofts.streambox.dialog.FilterDialog;
import nemosofts.streambox.interfaces.GetCategoryListener;
import nemosofts.streambox.interfaces.GetLiveListener;
import nemosofts.streambox.item.ItemCat;
import nemosofts.streambox.item.ItemLive;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.recycler.EndlessRecyclerViewScrollListener;
import nemosofts.streambox.view.NSoftsProgressDialog;

public class FilterActivity extends AppCompatActivity {

    private FrameLayout frameLayout;
    private NSoftsProgressDialog progressDialog;
    // Category
    private AdapterCategory adapter_category;
    private RecyclerView rv_cat;
    private ArrayList<ItemCat> arrayListCat;
    // Live
    private Boolean isOver = false, isScroll = false, isLoading = false;
    private int page = 1;
    private String cat_id = "0";
    private AdapterLiveTV adapter;
    private ArrayList<ItemLive> arrayList;
    private RecyclerView rv;
    private ProgressBar pb;
    private int is_page = 0;
    private GetLive loadLive;
    private int pos = 1;

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

        progressDialog = new NSoftsProgressDialog(FilterActivity.this);

        arrayList = new ArrayList<>();
        arrayListCat = new ArrayList<>();

        TextView page_title = findViewById(R.id.tv_page_title);
        page_title.setText(getString(R.string.live_tv_home));

        pb = findViewById(R.id.pb);
        frameLayout = findViewById(R.id.fl_empty);
        rv = findViewById(R.id.rv);
        GridLayoutManager grid = new GridLayoutManager(this, 1);
        grid.setSpanCount(ApplicationUtil.isTvBox(this) ? 6 : 5);
        rv.setLayoutManager(grid);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setHasFixedSize(true);
        rv.addOnScrollListener(new EndlessRecyclerViewScrollListener(grid) {
            @Override
            public void onLoadMore(int p, int totalItemsCount) {
                if (Boolean.TRUE.equals(is_page == 0) && (Boolean.FALSE.equals(isOver) && (Boolean.FALSE.equals(isLoading)))) {
                    isLoading = true;
                    new Handler().postDelayed(() -> {
                        isScroll = true;
                        getData();
                    }, 0);
                }
            }
        });

        rv_cat = findViewById(R.id.rv_cat);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rv_cat.setLayoutManager(llm);
        rv_cat.setItemAnimator(new DefaultItemAnimator());
        rv_cat.setHasFixedSize(true);

        findViewById(R.id.iv_filter).setOnClickListener(v -> new FilterDialog(this, 1, () -> recreate_data(pos)));

        new Handler().postDelayed(this::getDataCat, 0);

        findViewById(R.id.iv_search).setVisibility(View.INVISIBLE);
    }

    private void getDataCat() {
        GetCategory category = new GetCategory(this, 1, new GetCategoryListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(boolean success, ArrayList<ItemCat> itemCat) {
                progressDialog.dismiss();
                if (!isFinishing()){
                    if (success) {
                        if (itemCat.isEmpty()) {
                            setEmpty();
                        } else {
                            arrayListCat.add(new ItemCat("01",getString(R.string.favourite),""));
                            arrayListCat.add(new ItemCat("02",getString(R.string.recently),""));
                            arrayListCat.add(new ItemCat("03",getString(R.string.recently_add),""));
                            arrayListCat.addAll(itemCat);
                            cat_id = itemCat.get(0).getId();
                            setAdapterToCatListview();
                        }
                    } else {
                        setEmpty();
                    }
                }
            }
        });
        category.execute();
    }

    private void getData() {
        loadLive = new GetLive(this, page, cat_id, is_page, new GetLiveListener() {
            @Override
            public void onStart() {
                if (arrayList.isEmpty()){
                    pb.setVisibility(View.VISIBLE);
                    frameLayout.setVisibility(View.GONE);
                }
            }

            @Override
            public void onEnd(String success, ArrayList<ItemLive> arrayListLive) {
                if (!isFinishing()){
                    if (Boolean.FALSE.equals(isOver)){
                        pb.setVisibility(View.GONE);
                        if (success.equals("1")) {
                            if (arrayListLive.isEmpty()) {
                                isOver = true;
                                setEmpty();
                            } else {
                                arrayList.addAll(arrayListLive);
                                page = page + 1;
                                setAdapterToListview();
                            }
                        } else {
                            setEmpty();
                        }
                        isLoading = false;
                    }
                }
            }
        });
        loadLive.execute();
    }

    public void setAdapterToCatListview() {
        adapter_category = new AdapterCategory(this, arrayListCat, position -> {
            if (pos != position){
                recreate_data(position);
            }
        });
        rv_cat.setAdapter(adapter_category);
        adapter_category.select(3);
        pos = 3;
        getData();

        EditText edt_search = findViewById(R.id.edt_search);
        edt_search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH){
                InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.hideSoftInputFromWindow(Objects.requireNonNull(this.getCurrentFocus()).getWindowToken(),InputMethodManager.HIDE_NOT_ALWAYS);
            }
            return true;
        });
        edt_search.addTextChangedListener(searchWatcher);
    }

    TextWatcher searchWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (adapter_category != null) {
                adapter_category.getFilter().filter(s.toString());
                adapter_category.notifyDataSetChanged();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    };

    @SuppressLint("NotifyDataSetChanged")
    private void recreate_data(int position) {
        if (position >= 0 && position < arrayListCat.size()) {
            pos = position;
            cat_id = arrayListCat.get(position).getId();
            adapter_category.select(position);
            if (loadLive != null){
                loadLive.cancel(true);
            }
            if (!arrayList.isEmpty()){
                arrayList.clear();
            }
            if (adapter != null){
                adapter.notifyDataSetChanged();
            }
            isOver = true;
            new Handler().postDelayed(() -> {
                if (!arrayList.isEmpty()){
                    arrayList.clear();
                }
                if (arrayListCat.get(position).getName().equals(getString(R.string.favourite)) && arrayListCat.get(position).getId().equals("01")){
                    is_page = 1;
                } else  if (arrayListCat.get(position).getName().equals(getString(R.string.recently)) && arrayListCat.get(position).getId().equals("02")){
                    is_page = 2;
                } else  if (arrayListCat.get(position).getName().equals(getString(R.string.recently_add)) && arrayListCat.get(position).getId().equals("03")){
                    is_page = 3;
                } else {
                    is_page = 0;
                }
                isOver = false;
                isScroll = false;
                isLoading = false;
                page = 1;
                getData();
            }, 0);
        }
    }

    public void setAdapterToListview() {
        if(Boolean.FALSE.equals(isScroll)) {
            adapter = new AdapterLiveTV(this, arrayList, (itemCat, position) -> {
                Intent intent = new Intent();
                intent.putExtra("stream_id", arrayList.get(position).getStreamID());
                setResult(RESULT_OK, intent);
                finish();
            });
            rv.setAdapter(adapter);
            setEmpty();
        } else {
            adapter.notifyItemInserted(arrayList.size()-1);
        }
    }

    private void setEmpty() {
        if (!arrayList.isEmpty()) {
            rv.setVisibility(View.VISIBLE);
            frameLayout.setVisibility(View.GONE);
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
    public int setContentViewID() {
        return R.layout.activity_live_tv;
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