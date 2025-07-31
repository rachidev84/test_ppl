package nemosofts.streambox.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import nemosofts.streambox.interfaces.EpgFullListener;
import nemosofts.streambox.item.ItemEpgFull;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.helper.SPHelper;
import okhttp3.RequestBody;

public class LoadEpgFull extends AsyncTask<String, String, String> {

    private final SPHelper spHelper;
    private final RequestBody requestBody;
    private final EpgFullListener listener;
    private final ArrayList<ItemEpgFull> arrayList = new ArrayList<>();

    public LoadEpgFull(Context ctx, EpgFullListener listener, RequestBody requestBody) {
        this.listener = listener;
        this.requestBody = requestBody;
        spHelper = new SPHelper(ctx);
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {

            String json = ApplicationUtil.responsePost(spHelper.getAPI(), requestBody);
            JSONObject jsonObject = new JSONObject(json);

            if (jsonObject.has("epg_listings")) {

                JSONArray c =  jsonObject.getJSONArray("epg_listings");

                for (int i = 0; i < c.length(); i++) {
                    JSONObject jsonobject = c.getJSONObject(i);

                    String id = jsonobject.getString("id");
                    String start = jsonobject.getString("start");
                    String end = jsonobject.getString("end");
                    String title = jsonobject.getString("title");
                    String description = jsonobject.getString("description");
                    String start_timestamp = jsonobject.getString("start_timestamp");
                    String stop_timestamp = jsonobject.getString("stop_timestamp");
                    int now_playing = jsonobject.getInt("now_playing");
                    int has_archive = jsonobject.getInt("has_archive");
                    if (jsonobject.getInt("has_archive") == 1){
                        ItemEpgFull objItem = new ItemEpgFull(id,start,end,title,description,start_timestamp,stop_timestamp,now_playing,has_archive);
                        arrayList.add(objItem);
                    }
                }
            }

            return "1";
        } catch (Exception ee) {
            ee.printStackTrace();
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s, arrayList);
        super.onPostExecute(s);
    }

}