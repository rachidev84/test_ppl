package nemosofts.streambox.callback;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import nemosofts.streambox.BuildConfig;
import nemosofts.streambox.item.ItemDns;
import nemosofts.streambox.item.ItemEpisodes;
import nemosofts.streambox.item.ItemLive;
import nemosofts.streambox.item.ItemNotification;
import nemosofts.streambox.item.ItemPoster;

public class Callback implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // API URL
    public static String API_URL = BuildConfig.BASE_URL+"api.php";

    // TAG API
    public static final String TAG_ROOT = BuildConfig.API_NAME;
    public static final String TAG_SUCCESS = "success";
    public static final String TAG_MSG = "MSG";

    public static String TAG_TV = "date_tv";
    public static String TAG_MOVIE = "date_movies";
    public static String TAG_SERIES = "date_series";

    public static String TAG_LOGIN = "none";
    public static String TAG_LOGIN_ONE_UI = "one_ui";
    public static String TAG_LOGIN_SINGLE_STREAM = "single_stream";
    public static String TAG_LOGIN_PLAYLIST = "playlist";
    public static String TAG_LOGIN_STREAM = "stream";
    public static String TAG_LOGIN_VIDEOS = "videos";

    public static int playPosLive = 0;
    public static List<ItemLive> arrayListLive = new ArrayList<>();

    public static int playPosEpisodes = 0;
    public static List<ItemEpisodes> arrayListEpisodes = new ArrayList<>();

    public static int posNotify = 0;
    public static List<ItemNotification> arrayListNotify = new ArrayList<>();

    public static Boolean isPlayed = false;
    public static int playPos = 0;
    public static List<ItemLive> arrayList_play = new ArrayList<>();

    public static String successLive = "0";
    public static String successSeries = "0";
    public static String successMovies = "0";

    public static Boolean isAppUpdate = false;
    public static int app_new_version = 1;
    public static String app_update_desc = "";
    public static String app_redirect_url = "";

    public static final String DIALOG_TYPE_UPDATE = "upgrade";
    public static final String DIALOG_TYPE_MAINTENANCE = "maintenance";
    public static final String DIALOG_TYPE_DEVELOPER = "developer";
    public static final String DIALOG_TYPE_VPN = "vpn";

    public static Boolean isAppOpen = false;

    public static List<ItemDns> arrayBlacklist = new ArrayList<>();

    public static int posterPos = 0;
    public static List<ItemPoster> arrayListPoster = new ArrayList<>();

    public static Boolean isDataUpdate = false;
    public static boolean is_recreate = false;
    public static boolean is_recreate_ui = false;

    // Advertising
    public static final String AD_TYPE_ADMOB = "admob";

    public static String adNetwork = "admob";
    public static Boolean isAdsStatus = false;

    public static int adCount = 0;
    public static int interstitialAdShow = 5;
    public static long reward_minutes = 20;

    public static String
            admobPublisherID = "",
            admobBannerAdID = "",
            admobInterstitialAdID = "",
            admobRewardAdID = "";

    public static Boolean
            banner_movie = false,
            banner_series = false,
            banner_epg = false,
            isInterAd = false,
            reward_ad_on_movie = false,
            reward_ad_on_episodes = false,
            reward_ad_on_live = false,
            reward_ad_on_single = false,
            reward_ad_on_local = false;

    // Custom Advertising
    public static String adsTitle = "";
    public static String adsImage = "";
    public static String adsRedirectType = "";
    public static String adsRedirectURL = "";
}
