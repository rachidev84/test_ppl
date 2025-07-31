package nemosofts.streambox.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import androidx.nemosofts.Envato;

import org.json.JSONArray;
import org.json.JSONObject;

import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.callback.Method;
import nemosofts.streambox.interfaces.AboutListener;
import nemosofts.streambox.item.ItemDns;
import nemosofts.streambox.item.ItemNotification;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.helper.DBHelper;
import nemosofts.streambox.util.helper.Helper;
import nemosofts.streambox.util.helper.SPHelper;

public class LoadAbout extends AsyncTask<String, String, String> {

    private final DBHelper dbHelper;
    private final Envato envato;
    private final Helper helper;
    private final SPHelper spHelper;
    private final AboutListener aboutListener;
    private String verifyStatus = "0", message = "";

    public LoadAbout(Context context, AboutListener aboutListener) {
        this.aboutListener = aboutListener;
        helper = new Helper(context);
        spHelper = new SPHelper(context);
        envato = new Envato(context);
        dbHelper = new DBHelper(context);
    }

    @Override
    protected void onPreExecute() {
        aboutListener.onStart();
        if (!Callback.arrayListNotify.isEmpty()){
            Callback.arrayListNotify.clear();
        }
        super.onPreExecute();
    }

    @Override
    protected String doInBackground(String... strings) {
        try {
            String json = ApplicationUtil.responsePost(Callback.API_URL, helper.getAPIRequestNSofts(Method.METHOD_APP_DETAILS, "", "", "",""));
            JSONObject mainJson = new JSONObject(json);

            try {
                JSONObject jsonObject = mainJson.getJSONObject(Callback.TAG_ROOT);

                if (jsonObject.has("details")) {
                    JSONArray jsonArrayDetails = jsonObject.getJSONArray("details");

                    for (int i = 0; i < jsonArrayDetails.length(); i++) {
                        JSONObject c = jsonArrayDetails.getJSONObject(i);

                        // App Details
                        String email = c.getString("app_email");
                        String author = c.getString("app_author");
                        String contact = c.getString("app_contact");
                        String website = c.getString("app_website");
                        String description = c.getString("app_description");
                        String developed_by = c.getString("app_developed_by");
                        spHelper.setAboutDetails(email, author, contact, website, description, developed_by);

                        // Envato
                        String apikey = c.getString("envato_api_key");
                        if (!apikey.isEmpty()){
                            envato.setEnvatoKEY(apikey);
                        } else {
                            spHelper.setAboutDetails(false);
                        }

                        // isSupported
                        Boolean is_rtl = Boolean.parseBoolean(c.getString("is_rtl"));
                        Boolean is_maintenance = Boolean.parseBoolean(c.getString("is_maintenance"));
                        Boolean is_screenshot = Boolean.parseBoolean(c.getString("is_screenshot"));
                        Boolean is_apk = Boolean.parseBoolean(c.getString("is_apk"));
                        Boolean is_vpn = Boolean.parseBoolean(c.getString("is_vpn"));
                        Boolean is_xui_dns = Boolean.parseBoolean(c.getString("is_xui_dns"));
                        Boolean is_radio = Boolean.parseBoolean(c.getString("is_xui_radio"));
                        Boolean is_stream_dns = Boolean.parseBoolean(c.getString("is_stream_dns"));
                        Boolean is_stream_radio = Boolean.parseBoolean(c.getString("is_stream_radio"));
                        Boolean is_local_storage = Boolean.parseBoolean(c.getString("is_local_storage"));
                        spHelper.setIsSupported(is_rtl, is_maintenance, is_screenshot, is_apk, is_vpn, is_xui_dns, is_radio, is_stream_dns, is_stream_radio, is_local_storage);

                        // isSelect
                        Boolean is_xui = Boolean.parseBoolean(c.getString("is_select_xui"));
                        Boolean is_stream = Boolean.parseBoolean(c.getString("is_select_stream"));
                        Boolean is_playlist = Boolean.parseBoolean(c.getString("is_select_playlist"));
                        Boolean is_device_id = Boolean.parseBoolean(c.getString("is_select_device_id"));
                        Boolean is_single = Boolean.parseBoolean(c.getString("is_select_single"));
                        spHelper.setIsSelect(is_xui, is_stream, is_playlist, is_device_id, is_single);

                        // AppUpdate
                        Callback.isAppUpdate = Boolean.parseBoolean(c.getString("app_update_status"));
                        if(!c.getString("app_new_version").isEmpty()) {
                            Callback.app_new_version = Integer.parseInt(c.getString("app_new_version"));
                        }
                        Callback.app_update_desc = c.getString("app_update_desc");
                        Callback.app_redirect_url = c.getString("app_redirect_url");

                        if (c.has("is_theme") && (!c.getString("is_theme").isEmpty())) {
                            int theme = Integer.parseInt(c.getString("is_theme"));
                            spHelper.setIsTheme(theme);
                        }

                        if (c.has("is_epg") && (!c.getString("is_epg").isEmpty())) {
                            int theme = Integer.parseInt(c.getString("is_epg"));
                            spHelper.setIsThemeEPG(theme);
                        }

                        if (c.has("is_download")) {
                            Boolean download = Boolean.parseBoolean(c.getString("is_download"));
                            spHelper.setIsDownload(download);
                        }

                        if (c.has("tmdb_key")) {
                            String key = c.getString("tmdb_key");
                            spHelper.setTmdbKEY(key);
                        }
                    }
                }

                // Ads Network ---------------------------------------------------------------------
                if (jsonObject.has("ads_details")) {
                    try {
                        JSONArray jsonArrayDetails = jsonObject.getJSONArray("ads_details");
                        for (int i = 0; i < jsonArrayDetails.length(); i++) {
                            JSONObject c = jsonArrayDetails.getJSONObject(i);

                            Callback.isAdsStatus = Boolean.parseBoolean(c.getString("ad_status"));

                            // PRIMARY ADS -------------------------------------------------------------
                            Callback.adNetwork = c.getString("ad_network");

                            Callback.admobPublisherID = c.getString("publisher_id");
                            Callback.admobBannerAdID = c.getString("banner_ad_id");
                            Callback.admobInterstitialAdID = c.getString("interstital_ad_id");
                            Callback.admobRewardAdID = c.getString("reward_ad_id");

                            // ADS PLACEMENT -----------------------------------------------------------
                            if (c.has("banner_movie")){
                                Callback.banner_movie = Boolean.parseBoolean(c.getString("banner_movie"));
                                Callback.banner_series = Boolean.parseBoolean(c.getString("banner_series"));
                                Callback.banner_epg = Boolean.parseBoolean(c.getString("banner_epg"));
                                Callback.isInterAd = Boolean.parseBoolean(c.getString("interstital_ad"));
                                Callback.reward_ad_on_movie = Boolean.parseBoolean(c.getString("reward_ad_on_movie"));
                                Callback.reward_ad_on_episodes = Boolean.parseBoolean(c.getString("reward_ad_on_episodes"));
                                Callback.reward_ad_on_live = Boolean.parseBoolean(c.getString("reward_ad_on_live"));
                                Callback.reward_ad_on_single = Boolean.parseBoolean(c.getString("reward_ad_on_single"));
                                Callback.reward_ad_on_local = Boolean.parseBoolean(c.getString("reward_ad_on_local"));
                            }

                            // GLOBAL CONFIGURATION ----------------------------------------------------
                            if(!c.getString("interstital_ad_click").isEmpty()) {
                                Callback.interstitialAdShow = Integer.parseInt(c.getString("interstital_ad_click"));
                            }
                            if(!c.getString("reward_minutes").isEmpty()) {
                                Callback.reward_minutes = Integer.parseInt(c.getString("reward_minutes"));
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("xui_dns")) {
                    try {
                        dbHelper.removeAllDNS(DBHelper.TABLE_DNS_XUI);
                        JSONArray jsonArrayXui = jsonObject.getJSONArray("xui_dns");
                        if (jsonArrayXui.length() > 0) {
                            for (int i = 0; i < jsonArrayXui.length(); i++) {
                                JSONObject jsonobject = jsonArrayXui.getJSONObject(i);

                                String dns_title = jsonobject.getString("dns_title");
                                String dns_base = jsonobject.getString("dns_base");

                                ItemDns objItem = new ItemDns(dns_title, dns_base);
                                dbHelper.addToDNS(DBHelper.TABLE_DNS_XUI, objItem);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("stream_dns")) {
                    try {
                        dbHelper.removeAllDNS(DBHelper.TABLE_DNS_STREAM);
                        JSONArray jsonArrayXui = jsonObject.getJSONArray("stream_dns");
                        if (jsonArrayXui.length() > 0) {
                            for (int i = 0; i < jsonArrayXui.length(); i++) {
                                JSONObject jsonobject = jsonArrayXui.getJSONObject(i);

                                String dns_title = jsonobject.getString("dns_title");
                                String dns_base = jsonobject.getString("dns_base");

                                ItemDns objItem = new ItemDns(dns_title, dns_base);
                                dbHelper.addToDNS(DBHelper.TABLE_DNS_STREAM, objItem);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("xui_dns_block")) {
                    try {
                        JSONArray jsonArrayXui = jsonObject.getJSONArray("xui_dns_block");
                        if (jsonArrayXui.length() > 0) {
                            for (int i = 0; i < jsonArrayXui.length(); i++) {
                                JSONObject jsonobject = jsonArrayXui.getJSONObject(i);

                                String dns_base = jsonobject.getString("dns_base");

                                ItemDns objItem = new ItemDns("", dns_base);
                                Callback.arrayBlacklist.add(objItem);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("popup_ads")) {
                    try {
                        JSONArray jsonArrayDetails = jsonObject.getJSONArray("popup_ads");
                        for (int i = 0; i < jsonArrayDetails.length(); i++) {
                            JSONObject c = jsonArrayDetails.getJSONObject(i);

                            Callback.adsTitle = c.getString("ads_title");
                            Callback.adsImage = c.getString("ads_image");
                            Callback.adsRedirectType = c.getString("ads_redirect_type");
                            Callback.adsRedirectURL = c.getString("ads_redirect_url");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (jsonObject.has("notification_data")) {
                    try {
                        JSONArray jsonArrayNotify = jsonObject.getJSONArray("notification_data");
                        if (jsonArrayNotify.length() > 0) {
                            for (int i = 0; i < jsonArrayNotify.length(); i++) {
                                JSONObject jsonobject = jsonArrayNotify.getJSONObject(i);

                                String id = jsonobject.getString("id");
                                String title = jsonobject.getString("notification_title");
                                String msg = jsonobject.getString("notification_msg");
                                String des = jsonobject.getString("notification_description");
                                String notify_on = jsonobject.getString("notification_on");

                                ItemNotification objItem = new ItemNotification(id, title, msg, des, notify_on);
                                Callback.arrayListNotify.add(objItem);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return "1";
            } catch (Exception e) {
                JSONArray jsonArray = mainJson.getJSONArray(Callback.TAG_ROOT);
                JSONObject jsonObject = jsonArray.getJSONObject(0);
                verifyStatus = jsonObject.getString(Callback.TAG_SUCCESS);
                message = jsonObject.getString(Callback.TAG_MSG);
                e.printStackTrace();
                return "0";
            }
        } catch (Exception ee) {
            ee.printStackTrace();
            return "0";
        }
    }

    @Override
    protected void onPostExecute(String s) {
        aboutListener.onEnd(s, verifyStatus, message);
        super.onPostExecute(s);
    }
}