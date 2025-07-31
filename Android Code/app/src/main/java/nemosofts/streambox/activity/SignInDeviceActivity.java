package nemosofts.streambox.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.util.UnstableApi;
import androidx.nemosofts.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import nemosofts.streambox.R;
import nemosofts.streambox.adapter.AdapterDeviceID;
import nemosofts.streambox.asyncTask.LoadLogin;
import nemosofts.streambox.asyncTask.LoadUsers;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.callback.Method;
import nemosofts.streambox.dialog.DialogUtil;
import nemosofts.streambox.dialog.Toasty;
import nemosofts.streambox.interfaces.LoginListener;
import nemosofts.streambox.interfaces.UsersListener;
import nemosofts.streambox.item.ItemUsers;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.NetworkUtils;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;
import nemosofts.streambox.view.NSoftsProgressDialog;

@UnstableApi
public class SignInDeviceActivity extends AppCompatActivity {

    private Helper helper;
    private SPHelper spHelper;
    private RecyclerView rv;
    private ArrayList<ItemUsers> arrayList;
    private ProgressBar pb;
    private String device_id="N/A";
    private NSoftsProgressDialog progressDialog;
    private FrameLayout frameLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        IfSupported.IsRTL(this);
        IfSupported.IsScreenshot(this);
        IfSupported.hideStatusBar(this);
        IfSupported.keepScreenOn(this);

        findViewById(R.id.theme_bg).setBackgroundResource(ApplicationUtil.openThemeBg(this));

        progressDialog = new NSoftsProgressDialog(SignInDeviceActivity.this);

        device_id = ApplicationUtil.getDeviceID(this);

        TextView tv_device_id = findViewById(R.id.tv_device_id);
        tv_device_id.setText("ID - " + device_id);

        helper = new Helper(this);
        spHelper = new SPHelper(this);

        arrayList = new ArrayList<>();

        frameLayout = findViewById(R.id.fl_empty);
        pb = findViewById(R.id.pb);
        rv = findViewById(R.id.rv);
        rv.setHasFixedSize(true);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setHasFixedSize(true);
        rv.setNestedScrollingEnabled(false);

        loadDeviceData();

        findViewById(R.id.rl_list_player).setOnClickListener(view -> {
            Intent intent = new Intent(SignInDeviceActivity.this, SelectPlayerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.putExtra("from", "");
            startActivity(intent);
            finish();
        });
    }

    private void loadDeviceData() {
        if (NetworkUtils.isConnected(this)){
            LoadUsers loadUsers = new LoadUsers(new UsersListener() {
                @Override
                public void onStart() {
                    if (arrayList.isEmpty()) {
                        rv.setVisibility(View.GONE);
                        pb.setVisibility(View.VISIBLE);
                        frameLayout.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onEnd(String success, String verifyStatus, String message, ArrayList<ItemUsers> arrayListUsers) {
                    if (!isFinishing()){
                        if (success.equals("1")) {
                            if (arrayListUsers.isEmpty()) {
                                Toasty.makeText(SignInDeviceActivity.this, getString(R.string.err_no_data_found), Toasty.ERROR);
                                setEmpty();
                            } else {
                                arrayList.addAll(arrayListUsers);
                                setAdapter();
                            }
                        } else {
                            Toasty.makeText(SignInDeviceActivity.this, getString(R.string.err_server_not_connected), Toasty.ERROR);
                            setEmpty();
                        }
                    }
                }
            }, helper.getAPIRequestNSofts(Method.METHOD_GET_DEVICE_ID, "", "", "", device_id));
            loadUsers.execute();
        } else {
            Toasty.makeText(SignInDeviceActivity.this, getString(R.string.err_internet_not_connected), Toasty.ERROR);
            setEmpty();
        }
    }

    private void setAdapter() {
        AdapterDeviceID adapterDeviceID = new AdapterDeviceID(this, arrayList, (itemUsers, position) -> loadLogin(arrayList.get(position)));
        rv.setAdapter(adapterDeviceID);
        if (ApplicationUtil.isTvBox(this)){
            rv.requestFocus();
        }
        setEmpty();
    }

    private void setEmpty() {
        if (!arrayList.isEmpty()){
            rv.setVisibility(View.VISIBLE);
            pb.setVisibility(View.GONE);
            frameLayout.setVisibility(View.GONE);
        } else {
            pb.setVisibility(View.GONE);
            frameLayout.setVisibility(View.VISIBLE);
            frameLayout.removeAllViews();

            LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            @SuppressLint("InflateParams") View myView = inflater.inflate(R.layout.row_empty, null);

            myView.findViewById(R.id.tv_empty_msg_sub).setVisibility(View.GONE);

            frameLayout.addView(myView);
        }
    }

    private void loadLogin(ItemUsers itemUsers) {
        if (NetworkUtils.isConnected(this)){
            LoadLogin login = new LoadLogin(new LoginListener() {
                @Override
                public void onStart() {
                    progressDialog.show();
                }

                @Override
                public void onEnd(String success, String username, String password, String message, int auth, String status, String exp_date, String is_trial, String active_cons, String created_at, String max_connections, String allowed_output_formats, boolean xui, String version, int revision, String url, String port, String https_port, String server_protocol, String rtmp_port, int timestamp_now, String time_now, String timezone) {
                    progressDialog.dismiss();
                    if (!isFinishing()){
                        if (success.equals("1")) {
                            try {
                                if (Boolean.TRUE.equals(itemUsers.getUserType().equals("xui"))){
                                    spHelper.setLoginDetails(username,password,message,auth,status, exp_date, is_trial, active_cons,created_at,max_connections,
                                            xui,version,revision,url,port,https_port,server_protocol,rtmp_port,timestamp_now,time_now,timezone
                                    );
                                    spHelper.setLoginType(Callback.TAG_LOGIN_ONE_UI);
                                } else {
                                    spHelper.setLoginDetails(username,password,message,auth,status, exp_date, is_trial, active_cons,created_at,max_connections,
                                            xui,version,revision,url,port,https_port,server_protocol,rtmp_port,timestamp_now,time_now,timezone
                                    );
                                    spHelper.setLoginType(Callback.TAG_LOGIN_STREAM);
                                }

                                if (!allowed_output_formats.isEmpty()){
                                    if (allowed_output_formats.contains("m3u8")){
                                        spHelper.setLiveFormat(2);
                                    } else {
                                        spHelper.setLiveFormat(1);
                                    }
                                } else {
                                    spHelper.setLiveFormat(0);
                                }

                                spHelper.setAnyName(itemUsers.getUserName());
                                spHelper.setIsFirst(false);
                                spHelper.setIsLogged(true);
                                spHelper.setIsAutoLogin(true);

                                Toast.makeText(SignInDeviceActivity.this, "Login successfully.", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            ApplicationUtil.openThemeActivity(SignInDeviceActivity.this);
                        }  else {
                            Toast.makeText(SignInDeviceActivity.this, getString(R.string.err_login_not_incorrect), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            },itemUsers.getDnsBase(), helper.getAPIRequestLogin(itemUsers.getUserName(), itemUsers.getUserPassword()));
            login.execute();
        }  else {
            Toast.makeText(this, getString(R.string.err_internet_not_connected), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public int setContentViewID() {
        return R.layout.activity_sign_in_device;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        DialogUtil.ExitDialog(SignInDeviceActivity.this);
    }
}