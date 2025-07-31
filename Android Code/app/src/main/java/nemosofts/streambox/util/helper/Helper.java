package nemosofts.streambox.util.helper;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Objects;
import java.util.Random;

import nemosofts.streambox.R;
import nemosofts.streambox.activity.DownloadService;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.callback.Method;
import nemosofts.streambox.interfaces.InterAdListener;
import nemosofts.streambox.interfaces.RewardAdListener;
import nemosofts.streambox.item.ItemVideoDownload;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.advertising.AdManagerInterAdmob;
import nemosofts.streambox.util.advertising.GDPRChecker;
import nemosofts.streambox.util.advertising.RewardAdAdmob;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class Helper {

    private final Context ctx;
    private InterAdListener interAdListener;
    boolean isRewarded = false;

    public Helper(Context ctx) {
        this.ctx = ctx;
    }

    public Helper(Context ctx, InterAdListener interAdListener) {
        this.ctx = ctx;
        this.interAdListener = interAdListener;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
    }

    public void initializeAds() {
        if (!ApplicationUtil.isTvBox(ctx)){
            if (Callback.adNetwork.equals(Callback.AD_TYPE_ADMOB)) {
                MobileAds.initialize(ctx, initializationStatus -> {
                });
            }
        }
    }

    public Object showBannerAd(LinearLayout linearLayout, Boolean isShowAd) {
        if (isBannerAd() && isShowAd){
            if (Objects.equals(Callback.adNetwork, Callback.AD_TYPE_ADMOB)){
                Bundle extras = new Bundle();
                AdView adViewAdmob = new AdView(ctx);
                AdRequest adRequest = new AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter.class, extras)
                        .build();
                adViewAdmob.setAdUnitId(Callback.admobBannerAdID);
                adViewAdmob.setAdSize(AdSize.BANNER);
                linearLayout.addView(adViewAdmob);
                adViewAdmob.loadAd(adRequest);
                return adViewAdmob;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void showInterAd(final int pos, final String type) {
        if (isInterAd()){
            if (Objects.equals(Callback.adNetwork, Callback.AD_TYPE_ADMOB)){
                final AdManagerInterAdmob adManagerInterAdmob = new AdManagerInterAdmob(ctx);
                if (adManagerInterAdmob.getAd() != null) {
                    adManagerInterAdmob.getAd().setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            AdManagerInterAdmob.setAd(null);
                            adManagerInterAdmob.createAd();
                            interAdListener.onClick(pos, type);
                            super.onAdDismissedFullScreenContent();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(@NonNull @NotNull com.google.android.gms.ads.AdError adError) {
                            AdManagerInterAdmob.setAd(null);
                            adManagerInterAdmob.createAd();
                            interAdListener.onClick(pos, type);
                            super.onAdFailedToShowFullScreenContent(adError);
                        }
                    });
                    adManagerInterAdmob.getAd().show((Activity) ctx);
                } else {
                    AdManagerInterAdmob.setAd(null);
                    adManagerInterAdmob.createAd();
                    interAdListener.onClick(pos, type);
                }
            } else {
                interAdListener.onClick(pos, type);
            }
        } else {
            interAdListener.onClick(pos, type);
        }
    }

    public void loadRewardAds(RewardAdListener rewardAdListener, boolean playWhenReady) {
        if (Objects.equals(Callback.adNetwork, Callback.AD_TYPE_ADMOB)){
            final RewardAdAdmob rewardAdAdmob = new RewardAdAdmob(ctx);
            if (rewardAdAdmob.getAd() != null) {
                rewardAdAdmob.getAd().setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        RewardAdAdmob.setAd(null);
                        rewardAdAdmob.createAd();
                        if (isRewarded) {
                            rewardAdListener.isPlaying(playWhenReady);
                        }
                        super.onAdDismissedFullScreenContent();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull @NotNull com.google.android.gms.ads.AdError adError) {
                        RewardAdAdmob.setAd(null);
                        rewardAdAdmob.createAd();
                        rewardAdListener.isPlaying(playWhenReady);
                        super.onAdFailedToShowFullScreenContent(adError);
                    }
                });
                rewardAdAdmob.getAd().show((Activity) ctx, rewardItem -> isRewarded = true);
            } else {
                RewardAdAdmob.setAd(null);
                rewardAdAdmob.createAd();
                rewardAdListener.isPlaying(playWhenReady);
            }
        } else {
            rewardAdListener.isPlaying(playWhenReady);
        }
    }

    public void showRewardAds(Boolean isShowAd, Boolean playWhenReady, RewardAdListener rewardAdListener) {
        if (!ApplicationUtil.isTvBox(ctx) && isNetworkAvailable() && isShowAd && Boolean.TRUE.equals(Callback.isAdsStatus) && new GDPRChecker(ctx).canLoadAd()) {
            loadRewardAds(rewardAdListener, playWhenReady);
        } else {
            rewardAdListener.isPlaying(playWhenReady);
        }
    }

    private boolean isBannerAd() {
        return !ApplicationUtil.isTvBox(ctx) && isNetworkAvailable() && Boolean.TRUE.equals(Callback.isAdsStatus) && new GDPRChecker(ctx).canLoadAd();
    }

    private boolean isInterAd() {
        if (!ApplicationUtil.isTvBox(ctx) && isNetworkAvailable() && Boolean.TRUE.equals(Callback.isInterAd) && Boolean.TRUE.equals(Callback.isAdsStatus) && new GDPRChecker(ctx).canLoadAd()) {
            Callback.adCount = Callback.adCount + 1;
            return Callback.adCount % Callback.interstitialAdShow == 0;
        } else {
            return false;
        }
    }

    public RequestBody getAPIRequestLogin(String username, String password) {
        return new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("username", username)
            .addFormDataPart("password", password)
            .build();
    }

    public RequestBody getAPIRequest(String action, String username, String password) {
        return new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("username", username)
            .addFormDataPart("password", password)
            .addFormDataPart("action", action)
            .build();
    }

    public RequestBody getAPIRequestID(String action,String type, String series_id, String username, String password) {
        return new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("username", username)
            .addFormDataPart("password", password)
            .addFormDataPart("action", action)
            .addFormDataPart(type, series_id)
            .build();
    }

    public RequestBody getAPIRequestNSofts(String helper_name, String reportTitle, String reportMessages, String userName, String userPass) {
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd' 'HH:mm:ss").create();
        JsonObject jsObj = (JsonObject) new Gson().toJsonTree(gson);
        jsObj.addProperty("helper_name", helper_name);
        jsObj.addProperty("application_id", ctx.getPackageName());
        if (Method.METHOD_REPORT.equals(helper_name)) {
            jsObj.addProperty("user_name", userName);
            jsObj.addProperty("user_pass", userPass);
            jsObj.addProperty("report_title", reportTitle);
            jsObj.addProperty("report_msg", reportMessages);
        } else if (Method.METHOD_GET_DEVICE_ID.equals(helper_name)) {
            jsObj.addProperty("device_id", userPass);
        }
        return new MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("data", ApplicationUtil.toBase64(jsObj.toString()))
            .build();
    }

    @SuppressLint("StaticFieldLeak")
    public void download(final ItemVideoDownload itemDownload, String table) {
        File root = new File(ctx.getExternalFilesDir("").getAbsolutePath() + File.separator + "/temp");
        if (!root.exists()) {
            root.mkdirs();
        }

        Random random = new Random();
        String a = String.valueOf(System.currentTimeMillis());
        String name = random.nextInt((999999 - 100000) + 100000) + a.substring(a.length() - 6, a.length() - 1);

        File file = new File(root, name + ApplicationUtil.containerExtension(itemDownload.getContainerExtension()));
        if (Boolean.FALSE.equals(new DBHelper(ctx).checkDownload(table, itemDownload.getStreamID(), ApplicationUtil.containerExtension(itemDownload.getContainerExtension())))) {
            if (Boolean.FALSE.equals(DownloadService.getInstance().isDownloading())) {
                Intent serviceIntent = new Intent(ctx, DownloadService.class);
                serviceIntent.setAction(DownloadService.ACTION_START);
                serviceIntent.putExtra("downloadUrl", itemDownload.getVideoURL());
                serviceIntent.putExtra("file_path", root.toString());
                serviceIntent.putExtra("file_name", file.getName());
                serviceIntent.putExtra("file_container", ApplicationUtil.containerExtension(itemDownload.getContainerExtension()));
                serviceIntent.putExtra("item", itemDownload);
                ctx.startService(serviceIntent);
            } else {
                Intent serviceIntent = new Intent(ctx, DownloadService.class);
                serviceIntent.setAction(DownloadService.ACTION_ADD);
                serviceIntent.putExtra("downloadUrl", itemDownload.getVideoURL());
                serviceIntent.putExtra("file_path", root.toString());
                serviceIntent.putExtra("file_name", file.getName());
                serviceIntent.putExtra("file_container", ApplicationUtil.containerExtension(itemDownload.getContainerExtension()));
                serviceIntent.putExtra("item", itemDownload);
                ctx.startService(serviceIntent);
            }
        } else {
            Toast.makeText(ctx, ctx.getResources().getString(R.string.already_download), Toast.LENGTH_SHORT).show();
        }
    }
}
