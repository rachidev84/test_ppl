package nemosofts.streambox.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import org.json.JSONArray;

import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.interfaces.DataListener;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.helper.SPHelper;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.JSHelper;

public class LoadData extends AsyncTask<String, String, String> {

    private final JSHelper jsHelper;
    private final Helper helper;
    private final SPHelper spHelper;
    private final DataListener listener;

    public LoadData(Context ctx, DataListener listener) {
        this.listener = listener;
        helper = new Helper(ctx);
        spHelper = new SPHelper(ctx);
        jsHelper = new JSHelper(ctx);
    }

    @Override
    protected void onPreExecute() {
        listener.onStart();
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            if (jsHelper.getUpdateDate().isEmpty()){
                jsHelper.setUpdateDate();
                return "1";
            } else {
                // 5 Hours
                if (Boolean.TRUE.equals(ApplicationUtil.calculateUpdateHours(jsHelper.getUpdateDate(), spHelper.getAutoUpdate()))){
                    jsHelper.setUpdateDate();

                    try {
                        String currentSeries = spHelper.getCurrent(Callback.TAG_SERIES);
                        if (spHelper.getIsUpdateSeries() && !currentSeries.isEmpty()) {
                            String jsonSeries = ApplicationUtil.responsePost(spHelper.getAPI(), helper.getAPIRequest("get_series", spHelper.getUserName(), spHelper.getPassword()));
                            if (!jsonSeries.isEmpty()) {
                                JSONArray arraySeries = new JSONArray(jsonSeries);
                                if (arraySeries.length() != 0 && arraySeries.length() != jsHelper.getSeriesSize()) {
                                    jsHelper.setSeriesSize(arraySeries.length());
                                    jsHelper.addToSeriesData(jsonSeries);
                                    Callback.successSeries = "1";
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        String currentMovies = spHelper.getCurrent(Callback.TAG_MOVIE);
                        if (spHelper.getIsUpdateMovies() && !currentMovies.isEmpty()) {
                            String jsonMovies = ApplicationUtil.responsePost(spHelper.getAPI(), helper.getAPIRequest("get_vod_streams", spHelper.getUserName(), spHelper.getPassword()));
                            if (!jsonMovies.isEmpty()) {
                                JSONArray arrayMovies = new JSONArray(jsonMovies);
                                if (arrayMovies.length() != 0 && arrayMovies.length() != jsHelper.getMoviesSize()) {
                                    jsHelper.setMovieSize(arrayMovies.length());
                                    jsHelper.addToMovieData(jsonMovies);
                                    Callback.successMovies = "1";
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    try {
                        String currentLive = spHelper.getCurrent(Callback.TAG_TV);
                        if (spHelper.getIsUpdateLive() && !currentLive.isEmpty()) {
                            String jsonLive = ApplicationUtil.responsePost(spHelper.getAPI(), helper.getAPIRequest("get_live_streams", spHelper.getUserName(), spHelper.getPassword()));
                            if (!jsonLive.isEmpty()) {
                                JSONArray arrayLive = new JSONArray(jsonLive);
                                if (arrayLive.length() != 0 && arrayLive.length() != jsHelper.getLiveSize()) {
                                    jsHelper.setLiveSize(arrayLive.length());
                                    jsHelper.addToLiveData(jsonLive);
                                    Callback.successLive = "1";
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    return "1";
                } else {
                    return "2";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        listener.onEnd(s);
        super.onPostExecute(s);
    }
}