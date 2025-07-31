package nemosofts.streambox.activity.catchup;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;
import androidx.nemosofts.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import nemosofts.streambox.R;
import nemosofts.streambox.activity.player.PlayerSingleURLActivity;
import nemosofts.streambox.adapter.catchup.AdapterCatchUpEpg;
import nemosofts.streambox.adapter.catchup.AdapterTabEpg;
import nemosofts.streambox.asyncTask.LoadEpgFull;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.interfaces.EpgFullListener;
import nemosofts.streambox.item.ItemEpgFull;
import nemosofts.streambox.item.ItemSeasons;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.NetworkUtils;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;
import nemosofts.streambox.view.NSoftsProgressDialog;

public class CatchUpEpgActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private String stream_id = "39493";

    private ArrayList<ItemSeasons> arraySeasons;
    private ArrayList<ItemEpgFull> arrayList;
    private ArrayList<ItemEpgFull> arrayListFilter;

    private FrameLayout frameLayout, frameLayoutEpg;
    private NSoftsProgressDialog progressDialog;

    private ProgressBar pb_epg;

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

        stream_id = getIntent().getStringExtra("stream_id");
        String stream_name = getIntent().getStringExtra("stream_name");
        String stream_icon= getIntent().getStringExtra("stream_icon");

        TextView tv_page_title = findViewById(R.id.tv_page_title);
        tv_page_title.setText(stream_name);

        try {
            ImageView iv_app_logo = findViewById(R.id.iv_app_logo);
            Picasso.get()
                    .load(stream_icon)
                    .placeholder(R.drawable.bg_card_item_load)
                    .into(iv_app_logo);
        } catch (Exception e) {
            e.printStackTrace();
        }

        progressDialog = new NSoftsProgressDialog(CatchUpEpgActivity.this);

        helper = new Helper(this);
        spHelper = new SPHelper(this);

        arrayList = new ArrayList<>();
        arraySeasons = new ArrayList<>();
        arrayListFilter = new ArrayList<>();

        frameLayout = findViewById(R.id.fl_empty);
        frameLayoutEpg = findViewById(R.id.fl_empty_epg);
        pb_epg = findViewById(R.id.pb_epg);

        loadEpg();
    }

    private void loadEpg() {
        if (NetworkUtils.isConnected(this)){
            LoadEpgFull loadEpgFull = new LoadEpgFull(this, new EpgFullListener() {
                @Override
                public void onStart() {
                    progressDialog.show();
                }

                @Override
                public void onEnd(String success, ArrayList<ItemEpgFull> epgArrayList) {
                    if (!isFinishing()){
                        progressDialog.dismiss();
                        if (!epgArrayList.isEmpty()){
                            if (!arrayList.isEmpty()){
                                arrayList.clear();
                            }
                            arrayList.addAll(epgArrayList);
                            FilterEpgList();
                        } else {
                            setEmpty();
                        }
                    }
                }
            }, helper.getAPIRequestID("get_simple_data_table","stream_id", stream_id, spHelper.getUserName(), spHelper.getPassword()));
            loadEpgFull.execute();
        } else {
            setEmpty();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void FilterEpgList() {
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                progressDialog.show();
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    // Define the input and output date formats
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    SimpleDateFormat outputFormatFull = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    // Use a Set to keep track of formatted dates
                    Set<String> seenDates = new HashSet<>();
                    for (int i = 0; i < arrayList.size(); i++) {
                        try {
                            Date date = inputFormat.parse(arrayList.get(i).getStart());
                            if (date != null) {
                                String formattedDate = outputFormat.format(date);
                                String formattedDateFull = outputFormatFull.format(date);
                                if (!seenDates.contains(formattedDate)) {
                                    seenDates.add(formattedDate);
                                    arraySeasons.add(new ItemSeasons(formattedDate, formattedDateFull));
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
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
                    if (!arraySeasons.isEmpty()){
                        setSeasonsAdapter();
                    } else {
                        setEmpty();
                    }
                }
            }
        }.execute();
    }

    private void setSeasonsAdapter() {
        RecyclerView rv_seasons = findViewById(R.id.rv_tab);
        rv_seasons.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rv_seasons.setLayoutManager(llm);
        rv_seasons.setNestedScrollingEnabled(false);
        AdapterTabEpg adapterTabEpg = new AdapterTabEpg(this, arraySeasons, (itemSeasons, position) -> filterDate(arraySeasons.get(position)));
        rv_seasons.setAdapter(adapterTabEpg);
        filterDate(arraySeasons.get(arraySeasons.size()-1));
        if (ApplicationUtil.isTvBox(this)){
            rv_seasons.requestFocus();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void filterDate(ItemSeasons itemSeasons) {
        new AsyncTask<String, String, String>() {

            @Override
            protected void onPreExecute() {
                pb_epg.setVisibility(View.VISIBLE);
                frameLayoutEpg.setVisibility(View.GONE);
                findViewById(R.id.rv_epg).setVisibility(View.GONE);
                if (!arrayListFilter.isEmpty()){
                    arrayListFilter.clear();
                }
                super.onPreExecute();
            }

            @Override
            protected String doInBackground(String... strings) {
                try {
                    // Define the input and output date formats
                    SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

                    // Use a Set to keep track of formatted dates
                    for (int i = 0; i < arrayList.size(); i++) {
                        try {
                            Date date = inputFormat.parse(arrayList.get(i).getStart());
                            if (date != null) {
                                String formattedDate = outputFormat.format(date);
                                if (itemSeasons.getSeasonNumber().equals(formattedDate)) {
                                    arrayListFilter.add(arrayList.get(i));
                                }
                            }
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                if (!isFinishing()){
                    pb_epg.setVisibility(View.GONE);
                    if (!arrayListFilter.isEmpty()){
                        setEpgAdapter();
                    } else {
                        setEmptyEpg();
                    }
                }
            }
        }.execute();
    }

    private void setEpgAdapter() {
        RecyclerView rv = findViewById(R.id.rv_epg);
        rv.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(llm);
        rv.setNestedScrollingEnabled(false);
        AdapterCatchUpEpg adapterTabEpg = new AdapterCatchUpEpg(spHelper.getIs12Format(), arrayListFilter, (itemEpgFull, position) -> {
            if (arrayListFilter.get(position).getHasArchive() == 1){
                openPlayer(arrayListFilter.get(position));
            }
        });
        rv.setAdapter(adapterTabEpg);
        rv.setVisibility(View.VISIBLE);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void openPlayer(ItemEpgFull item) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormatData = new SimpleDateFormat("yyyy-MM-dd:HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
            Date dateStart = inputFormat.parse(item.getStart());
            Date dateEnd = inputFormat.parse(item.getEnd());
            if (dateStart != null && dateEnd != null) {
                String formattedDateStart = outputFormat.format(dateStart);
                String formattedDateEnd = outputFormat.format(dateEnd);
                String start = outputFormatData.format(dateStart);
                try {
                    Date startTime = outputFormat.parse(formattedDateStart);
                    Date endTime = outputFormat.parse(formattedDateEnd);
                    if (startTime != null && endTime != null) {
                        long durationInMillis = endTime.getTime() - startTime.getTime();
                        long durationInMinutes = durationInMillis / (1000 * 60);
                        String channel_url = spHelper.getServerURL()+"streaming/timeshift.php?username="+spHelper.getUserName()+"&password="+spHelper.getPassword()+"&stream="+stream_id+"&start="+start+"&duration="+String.valueOf(durationInMinutes);
                        Intent intent = new Intent(CatchUpEpgActivity.this, PlayerSingleURLActivity.class);
                        intent.putExtra("channel_title", ApplicationUtil.decodeBase64(item.getTitle()));
                        intent.putExtra("channel_url", channel_url);
                        startActivity(intent);
                    }
                } catch (ParseException e) {
                    Toasty.makeText(CatchUpEpgActivity.this, "Date parsing error", Toasty.ERROR);
                }
            }
        } catch (Exception e) {
            Toasty.makeText(CatchUpEpgActivity.this, "Unexpected error", Toasty.ERROR);
        }
    }

    private void setEmptyEpg() {
        if (!arrayListFilter.isEmpty()) {
            frameLayoutEpg.setVisibility(View.GONE);
        } else {

            frameLayoutEpg.setVisibility(View.VISIBLE);

            frameLayoutEpg.removeAllViews();

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            myView.findViewById(R.id.tv_empty_msg_sub).setVisibility(View.GONE);

            frameLayoutEpg.addView(myView);
        }
    }

    private void setEmpty() {
        if (!arraySeasons.isEmpty()) {
            frameLayout.setVisibility(View.GONE);
        } else {

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
        return R.layout.activity_catch_up_epg;
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