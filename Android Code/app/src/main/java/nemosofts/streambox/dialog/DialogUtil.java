package nemosofts.streambox.dialog;

import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.WRAP_CONTENT;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.OptIn;
import androidx.core.app.ActivityCompat;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.SimpleExoPlayer;
import androidx.nemosofts.view.ImageHelperView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Objects;

import nemosofts.streambox.R;
import nemosofts.streambox.activity.WebActivity;
import nemosofts.streambox.adapter.AdapterRadioButton;
import nemosofts.streambox.callback.Callback;
import nemosofts.streambox.item.ItemRadioButton;
import nemosofts.streambox.util.ApplicationUtil;
import nemosofts.streambox.util.IfSupported;

public class DialogUtil {

    private static Dialog dialog;

    private DialogUtil() {
        throw new IllegalStateException("Utility class");
    }

    // Dialog --------------------------------------------------------------------------------------
    public static void ExitDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        boolean isTvBox = ApplicationUtil.isTvBox(activity);
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (isTvBox){
            dialog.setContentView(R.layout.dialog_app_tv);
            dialog.findViewById(R.id.tv_do_cancel).setOnClickListener(view -> dismissDialog());
            dialog.findViewById(R.id.tv_do_yes).setOnClickListener(view -> {
                dismissDialog();
                activity.finish();
            });
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            dialog.findViewById(R.id.tv_do_cancel).requestFocus();
        } else {
            dialog.setContentView(R.layout.dialog_app);

            ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
            icon.setImageResource(R.drawable.ic_exit_to_app);

            TextView title = dialog.findViewById(R.id.tv_dialog_title);
            title.setText(R.string.exit);

            TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
            msg.setText(R.string.sure_exit);

            dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dismissDialog());

            dialog.findViewById(R.id.tv_dialog_no).setOnClickListener(view -> dismissDialog());
            dialog.findViewById(R.id.tv_dialog_yes).setOnClickListener(view -> {
                dismissDialog();
                activity.finish();
            });

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
        }
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void MaintenanceDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.maintenance);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.we_are_performing_scheduled);

        // VISIBLE
        TextView title_sub = dialog.findViewById(R.id.tv_dialog_title_sub);
        title_sub.setVisibility(View.VISIBLE);
        title_sub.setText(R.string.temporarily_down_for_maintenance);

        ImageView icon_bg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        icon_bg.setVisibility(View.VISIBLE);
        icon_bg.setImageResource(R.drawable.ic_maintenance);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void UpgradeDialog(Activity activity, CancelListener listener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.upgrade);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.its_time_to_upgrade);

        // VISIBLE
        TextView title_sub = dialog.findViewById(R.id.tv_dialog_title_sub);
        title_sub.setVisibility(View.VISIBLE);
        title_sub.setText(R.string.upgrade);

        ImageView icon_bg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        icon_bg.setVisibility(View.VISIBLE);
        icon_bg.setImageResource(R.drawable.ic_upgrade_svg);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dismissDialog();
            listener.onCancel();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dismissDialog();
            listener.onCancel();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setText(R.string.do_it_now);
        yes.setOnClickListener(view -> {
            dismissDialog();
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Callback.app_redirect_url)));
        });

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void DModeDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.developer_mode);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.turn_off_developer_mode);

        // VISIBLE
        TextView title_sub = dialog.findViewById(R.id.tv_dialog_title_sub);
        title_sub.setVisibility(View.VISIBLE);
        title_sub.setText(R.string.developer_mode);

        ImageView icon_bg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        icon_bg.setVisibility(View.VISIBLE);
        icon_bg.setImageResource(R.drawable.ic_coding_development);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.try_again_later);
        no.setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void VpnDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_app);

        ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
        icon.setImageResource(R.drawable.ic_error);

        TextView title = dialog.findViewById(R.id.tv_dialog_title);
        title.setText(R.string.sniffing_detected);

        TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
        msg.setText(R.string.turn_off_all_sniffers_tools);

        // VISIBLE
        TextView title_sub = dialog.findViewById(R.id.tv_dialog_title_sub);
        title_sub.setVisibility(View.VISIBLE);
        title_sub.setText(R.string.sniffing_detected);

        ImageView icon_bg = dialog.findViewById(R.id.iv_dialog_icon_bg);
        icon_bg.setVisibility(View.VISIBLE);
        icon_bg.setImageResource(R.drawable.ic_vpn_network);

        dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView no = dialog.findViewById(R.id.tv_dialog_no);
        no.setText(R.string.cancel);
        no.setOnClickListener(view -> {
            dismissDialog();
            activity.finish();
        });

        TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
        yes.setVisibility(View.GONE);

        View view = dialog.findViewById(R.id.vw_dialog_bar);
        view.setVisibility(View.GONE);

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void LogoutDialog(Activity activity, LogoutListener logoutListener) {
        boolean isTvBox = ApplicationUtil.isTvBox(activity);
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (isTvBox){
            dialog.setContentView(R.layout.dialog_app_tv);

            ImageView iv_dialog_icon =  dialog.findViewById(R.id.iv_dialog_icon);
            iv_dialog_icon.setImageResource(R.drawable.ic_exit_to_app);

            TextView tv_dialog_desc =  dialog.findViewById(R.id.tv_dialog_desc);
            tv_dialog_desc.setText(R.string.sure_logout);

            TextView tv_do_yes = dialog.findViewById(R.id.tv_do_yes);
            tv_do_yes.setText(R.string.yes);
            tv_do_yes.setOnClickListener(view -> {
                dismissDialog();
                logoutListener.onLogout();
            });

            TextView tv_do_cancel = dialog.findViewById(R.id.tv_do_cancel);
            tv_do_cancel.setText(R.string.no);
            tv_do_cancel.setOnClickListener(view -> dismissDialog());

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            dialog.findViewById(R.id.tv_do_cancel).requestFocus();
        } else {

            dialog.setContentView(R.layout.dialog_app);

            ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
            icon.setImageResource(R.drawable.ic_exit_to_app);

            TextView title = dialog.findViewById(R.id.tv_dialog_title);
            title.setText(R.string.logout);

            TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
            msg.setText(R.string.sure_logout);

            dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dismissDialog());

            TextView no = dialog.findViewById(R.id.tv_dialog_no);
            no.setText(R.string.no);
            no.setOnClickListener(view -> dismissDialog());

            TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
            yes.setText(R.string.yes);
            yes.setOnClickListener(view -> {
                dismissDialog();
                logoutListener.onLogout();
            });

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
        }
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void DeleteDialog(Context context, DeleteListener listener) {
        boolean isTvBox = ApplicationUtil.isTvBox(context);
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (isTvBox){
            dialog.setContentView(R.layout.dialog_app_tv);

            ImageView iv_dialog_icon =  dialog.findViewById(R.id.iv_dialog_icon);
            iv_dialog_icon.setImageResource(R.drawable.ic_trash);

            TextView tv_dialog_desc =  dialog.findViewById(R.id.tv_dialog_desc);
            tv_dialog_desc.setText(R.string.sure_delete);

            TextView tv_do_yes = dialog.findViewById(R.id.tv_do_yes);
            tv_do_yes.setText(R.string.delete);
            tv_do_yes.setOnClickListener(view -> {
                dismissDialog();
                listener.onDelete();
            });

            TextView tv_do_cancel = dialog.findViewById(R.id.tv_do_cancel);
            tv_do_cancel.setText(R.string.cancel);
            tv_do_cancel.setOnClickListener(view -> dismissDialog());

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            dialog.findViewById(R.id.tv_do_cancel).requestFocus();

        } else {

            dialog.setContentView(R.layout.dialog_app);

            ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
            icon.setImageResource(R.drawable.ic_trash);

            TextView title = dialog.findViewById(R.id.tv_dialog_title);
            title.setText(R.string.delete);

            TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
            msg.setText(R.string.sure_delete);

            dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dismissDialog());

            TextView no = dialog.findViewById(R.id.tv_dialog_no);
            no.setText(R.string.cancel);
            no.setOnClickListener(view -> dismissDialog());

            TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
            yes.setText(R.string.delete);
            yes.setOnClickListener(view -> {
                dismissDialog();
                listener.onDelete();
            });

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
        }
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void DownloadDataDialog(Activity activity, String type, DownloadListener listener) {
        boolean isTvBox = ApplicationUtil.isTvBox(activity);
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (isTvBox){
            dialog.setContentView(R.layout.dialog_app_tv);

            ImageView iv_dialog_icon =  dialog.findViewById(R.id.iv_dialog_icon);
            iv_dialog_icon.setImageResource(R.drawable.ic_reset);

            TextView tv_dialog_desc =  dialog.findViewById(R.id.tv_dialog_desc);
            tv_dialog_desc.setText(R.string.sure_reload_data);

            TextView tv_do_yes = dialog.findViewById(R.id.tv_do_yes);
            tv_do_yes.setText(R.string.yes);
            tv_do_yes.setOnClickListener(view -> {
                dismissDialog();
                listener.onDownload(type);
            });

            TextView tv_do_cancel = dialog.findViewById(R.id.tv_do_cancel);
            tv_do_cancel.setText(R.string.no);
            tv_do_cancel.setOnClickListener(view -> dismissDialog());

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            dialog.findViewById(R.id.tv_do_cancel).requestFocus();
        } else {

            dialog.setContentView(R.layout.dialog_app);

            ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
            icon.setImageResource(R.drawable.ic_reset);

            TextView title = dialog.findViewById(R.id.tv_dialog_title);
            title.setText(R.string.reload_data);

            TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
            msg.setText(R.string.sure_reload_data);

            dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dismissDialog());

            TextView no = dialog.findViewById(R.id.tv_dialog_no);
            no.setText(R.string.no);
            no.setOnClickListener(view -> dismissDialog());

            TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
            yes.setText(R.string.yes);
            yes.setOnClickListener(view -> {
                dismissDialog();
                listener.onDownload(type);
            });

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
        }
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public static void DialogPlayerInfo(Activity ctx, SimpleExoPlayer exoPlayer, boolean isLive) {
        if (exoPlayer != null){
            if (dialog != null){
                dialog = null;
            }
            dialog = new Dialog(ctx);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_media_info);
            dialog.findViewById(R.id.iv_close_vw).setOnClickListener(v -> dismissDialog());
            dialog.findViewById(R.id.iv_back_player_info).setOnClickListener(v -> dismissDialog());

            String info_video = ApplicationUtil.getInfoVideo(exoPlayer, isLive);
            TextView media_video = dialog.findViewById(R.id.tv_info_video);
            media_video.setText(info_video);

            String info_audio = ApplicationUtil.getInfoAudio(exoPlayer);
            TextView media_audio = dialog.findViewById(R.id.tv_info_audio);
            media_audio.setText(info_audio);

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null){
                IfSupported.hideStatusBarDialog(window);
                window.setLayout(MATCH_PARENT, WRAP_CONTENT);
            }
        }
    }

    public static void ScreenDialog(Activity activity, ScreenDialogListener listener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_screen);
        dialog.setCancelable(false);
        dialog.findViewById(R.id.iv_screen_one).setOnClickListener(v -> {
            listener.onSubmit(1);
            dismissDialog();
        });
        dialog.findViewById(R.id.iv_screen_two).setOnClickListener(v -> {
            listener.onSubmit(2);
            dismissDialog();
        });
        dialog.findViewById(R.id.iv_screen_three).setOnClickListener(v -> {
            listener.onSubmit(3);
            dismissDialog();
        });
        dialog.findViewById(R.id.iv_screen_four).setOnClickListener(v -> {
            listener.onSubmit(4);
            dismissDialog();
        });
        dialog.findViewById(R.id.iv_screen_five).setOnClickListener(v -> {
            listener.onSubmit(5);
            dismissDialog();
        });
        dialog.setOnKeyListener((dialog, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_BACK)) {
                listener.onCancel();
                dialog.dismiss();
                return true;
            }
            return false;
        });
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void LiveDownloadDialog(Context context, LiveDownloadListener listener) {
        if (dialog != null){
            dialog = null;
        }
        boolean isTvBox = ApplicationUtil.isTvBox(context);
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        if (isTvBox){
            dialog.setContentView(R.layout.dialog_app_tv);

            ImageView iv_dialog_icon =  dialog.findViewById(R.id.iv_dialog_icon);
            iv_dialog_icon.setImageResource(R.drawable.ic_file_download);

            TextView tv_dialog_desc =  dialog.findViewById(R.id.tv_dialog_desc);
            tv_dialog_desc.setText(R.string.want_to_download);

            TextView tv_do_yes = dialog.findViewById(R.id.tv_do_yes);
            tv_do_yes.setText(R.string.yes);
            tv_do_yes.setOnClickListener(view -> {
                listener.onDownload();
                dismissDialog();
            });

            TextView tv_do_cancel = dialog.findViewById(R.id.tv_do_cancel);
            tv_do_cancel.setText(R.string.no);
            tv_do_cancel.setOnClickListener(view -> dismissDialog());

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
            dialog.show();
            dialog.findViewById(R.id.tv_do_cancel).requestFocus();

        } else {
            dialog.setContentView(R.layout.dialog_app);

            ImageView icon = dialog.findViewById(R.id.iv_dialog_icon);
            icon.setImageResource(R.drawable.ic_file_download);

            TextView title = dialog.findViewById(R.id.tv_dialog_title);
            title.setText(R.string.live_not_downloaded);

            TextView msg = dialog.findViewById(R.id.tv_dialog_msg);
            msg.setText(R.string.want_to_download);

            dialog.findViewById(R.id.iv_dialog_close).setOnClickListener(view -> dismissDialog());

            TextView no = dialog.findViewById(R.id.tv_dialog_no);
            no.setText(R.string.no);
            no.setOnClickListener(view -> dismissDialog());

            TextView yes = dialog.findViewById(R.id.tv_dialog_yes);
            yes.setText(R.string.yes);
            yes.setOnClickListener(view -> {
                dismissDialog();
                listener.onDownload();
            });

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
        }
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void RadioBtnDialog(Context context, ArrayList<ItemRadioButton> arrayList, int position, String pageTitle, RadioBtnListener radioBtnListener) {
        if (dialog != null){
            dialog = null;
        }
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_radio_btn);

        TextView tv_title = dialog.findViewById(R.id.tv_page_title_dil);
        tv_title.setText(pageTitle);

        RecyclerView rv = dialog.findViewById(R.id.rv_radio_btn);
        GridLayoutManager grid = new GridLayoutManager(context, 2);
        grid.setSpanCount(2);
        rv.setLayoutManager(grid);
        rv.setItemAnimator(new DefaultItemAnimator());
        rv.setHasFixedSize(true);

        AdapterRadioButton adapter = new AdapterRadioButton(context, arrayList, position);
        rv.setAdapter(adapter);

        dialog.findViewById(R.id.tv_submit).setOnClickListener(view -> {
            radioBtnListener.onSetLimit(adapter.getData());
            dismissDialog();
        });
        dialog.findViewById(R.id.iv_close).setOnClickListener(view -> dismissDialog());
        dialog.findViewById(R.id.tv_cancel).setOnClickListener(view -> dismissDialog());


        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null){
            IfSupported.hideStatusBarDialog(window);
            window.setLayout(MATCH_PARENT, WRAP_CONTENT);
        }
    }

    public static void PopupAdsDialog(Activity activity) {
        if (dialog != null){
            dialog = null;
        }
        if (!Callback.adsImage.isEmpty() && !Callback.adsRedirectURL.isEmpty()){
            dialog = new Dialog(activity);
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialog.setContentView(R.layout.dialog_popup_ads);

            ImageHelperView loadAds = dialog.findViewById(R.id.iv_ads);
            ProgressBar pb_ads  = dialog.findViewById(R.id.pb_ads);
            dialog.findViewById(R.id.vw_ads).setOnClickListener(v -> {
                if (Callback.adsRedirectType.equals("external")){
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(Callback.adsRedirectURL));
                    activity.startActivity(browserIntent);
                } else {
                    Intent intent = new Intent(activity, WebActivity.class);
                    intent.putExtra("web_url", Callback.adsRedirectURL);
                    intent.putExtra("page_title", Callback.adsTitle);
                    ActivityCompat.startActivity(activity, intent, null);
                }
            });
            try {
                Picasso.get()
                        .load(Callback.adsImage)
                        .placeholder(R.color.white)
                        .into(loadAds, new com.squareup.picasso.Callback() {
                            @Override
                            public void onSuccess() {
                                pb_ads.setVisibility(View.GONE);
                            }

                            @Override
                            public void onError(Exception e) {
                                pb_ads.setVisibility(View.GONE);
                            }
                        });
            } catch (Exception e) {
                e.printStackTrace();
            }

            dialog.findViewById(R.id.iv_back_ads).setOnClickListener(view -> {
                dismissDialog();
                Callback.adsImage ="";
                Callback.adsRedirectURL = "";
            });

            boolean isTvBox = ApplicationUtil.isTvBox(activity);
            if (isTvBox){
                dialog.findViewById(R.id.iv_back_ads).requestFocus();
            }

            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.dialogAnimation;
            dialog.show();
            Window window = dialog.getWindow();
            if (window != null){
                IfSupported.hideStatusBarDialog(window);
                window.setLayout(MATCH_PARENT, WRAP_CONTENT);
            }
        }
    }

    // Dismiss -------------------------------------------------------------------------------------
    public static void dismissDialog() {
        if (dialog != null && dialog.isShowing()){
            dialog.dismiss();
        }
    }

    // isShowing -----------------------------------------------------------------------------------
    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    // Listener ------------------------------------------------------------------------------------
    public interface CancelListener {
        void onCancel();
    }

    public interface LogoutListener {
        void onLogout();
    }

    public interface DeleteListener {
        void onDelete();
    }

    public interface DownloadListener {
        void onDownload(String type);
    }

    public interface LiveDownloadListener {
        void onDownload();
    }

    public interface ScreenDialogListener {
        void onSubmit(int screen);
        void onCancel();
    }

    public interface RadioBtnListener {
        void onSetLimit(int update);
    }
}
