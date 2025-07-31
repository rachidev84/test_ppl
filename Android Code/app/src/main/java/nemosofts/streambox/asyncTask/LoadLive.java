package nemosofts.streambox.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import nemosofts.streambox.interfaces.LoadSuccessListener;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.HttpsTrustManager;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.JSHelper;
import nemosofts.streambox.util.helper.SPHelper;

public class LoadLive extends AsyncTask<String, String, String> {

    private final JSHelper jsHelper;
    private final Helper helper;
    private final LoadSuccessListener listener;
    private String msg = "";

    private final String API;
    private final String USER_NAME;
    private final String USER_PASS;

    public LoadLive(Context ctx, LoadSuccessListener listener) {
        this.listener = listener;
        helper = new Helper(ctx);
        jsHelper = new JSHelper(ctx);

        SPHelper spHelper = new SPHelper(ctx);
        API = spHelper.getAPI();
        USER_NAME = spHelper.getUserName();
        USER_PASS = spHelper.getPassword();
    }

    @Override
    protected void onPreExecute() {
        jsHelper.removeAllLive(); // Clear existing live data before loading new data
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            // Fetch live stream categories --------------------------------------------------------
            try {
                String jsonCategory = ApplicationUtil.responsePost(API, helper.getAPIRequest("get_live_categories", USER_NAME, USER_PASS));
                if (jsonCategory.isEmpty()){
                    msg = "No live categories found";
                    return "3";
                }
                JSONArray arrayCategory = new JSONArray(jsonCategory);
                if (arrayCategory.length() != 0){
                    jsHelper.addToCatLiveList(jsonCategory);
                }  else {
                    msg = "No live categories found";
                    return "3";
                }
            } catch (Exception e) {
                String jsonResponseCategory = "";
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                try {
                    HttpsTrustManager.allowAllSSL();

                    URL url = new URL(API+"?username="+USER_NAME+"&password="+USER_PASS+"&action=get_vod_categories");

                    // Open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream
                    InputStream inputStream = urlConnection.getInputStream();
                    if (inputStream == null) {
                        msg = "Please try again";
                        return "3";
                    }

                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    if (stringBuilder.length() == 0) {
                        msg = "Please try again";
                        return "3";
                    }
                    jsonResponseCategory = stringBuilder.toString();
                } catch (IOException e2) {
                    e2.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                }

                try {
                    JSONArray arrayCategory = new JSONArray(jsonResponseCategory);
                    if (arrayCategory.length() != 0){
                        jsHelper.addToCatLiveList(jsonResponseCategory);
                    }  else {
                        msg = "No live categories found";
                        return "3";
                    }
                } catch (Exception ex) {
                    msg = "Please try again";
                    return "3";
                }
            }

            // Fetch live stream
            try {
                String jsonLive = ApplicationUtil.responsePost(API, helper.getAPIRequest("get_live_streams", USER_NAME, USER_PASS));
                if (jsonLive.isEmpty()){
                    msg = "No live found";
                    return "3";
                }
                JSONArray jsonarray = new JSONArray(jsonLive);
                if (jsonarray.length() != 0){
                    jsHelper.setLiveSize(jsonarray.length());
                    jsHelper.addToLiveData(jsonLive );
                } else {
                    msg = "No live found";
                    return "3";
                }
            } catch (Exception e) {
                String jsonResponseLive = "";
                HttpURLConnection urlConnection = null;
                BufferedReader reader = null;

                try {
                    HttpsTrustManager.allowAllSSL();

                    URL url = new URL(API+"?username="+USER_NAME+"&password="+USER_PASS+"&action=get_live_streams");

                    // Open the connection
                    urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.connect();

                    // Read the input stream
                    InputStream inputStream = urlConnection.getInputStream();
                    if (inputStream == null) {
                        msg = "Please try again";
                        return "3";
                    }

                    reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder stringBuilder = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        stringBuilder.append(line);
                    }

                    if (stringBuilder.length() == 0) {
                        msg = "Please try again";
                        return "3";
                    }
                    jsonResponseLive = stringBuilder.toString();
                } catch (IOException e2) {
                    e2.printStackTrace();
                } finally {
                    if (urlConnection != null) {
                        urlConnection.disconnect();
                    }
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e22) {
                            e22.printStackTrace();
                        }
                    }
                }

                try {
                    JSONArray jsonarray = new JSONArray(jsonResponseLive);
                    if (jsonarray.length() != 0){
                        jsHelper.setLiveSize(jsonarray.length());
                        jsHelper.addToLiveData(jsonResponseLive);
                    } else {
                        msg = "No live found";
                        return "3";
                    }
                } catch (Exception ex) {
                    msg = "Please try again";
                    return "3";
                }
            }
            return "1";
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, msg);
        super.onPostExecute(s);
    }

    @Override
    protected void onCancelled(String s) {
        listener.onEnd(s, msg);
        super.onCancelled(s);
    }
}