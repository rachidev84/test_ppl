package nemosofts.streambox.dialog;

import static android.view.WindowManager.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.WRAP_CONTENT;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.RadioGroup;

import java.util.Objects;

import nemosofts.streambox.R;
import nemosofts.streambox.util.IfSupported;
import nemosofts.streambox.util.helper.JSHelper;

public class FilterDialog {

    private final Dialog dialog;
    private final JSHelper jsHelper;
    private Boolean flag = false;
    private final FilterDialogListener listener;

    public FilterDialog(Context context, int pageType, FilterDialogListener filterListener) {
        this.listener = filterListener;
        jsHelper = new JSHelper(context);
        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.dialog_filter);
        dialog.findViewById(R.id.iv_close_btn).setOnClickListener(view -> dismissDialog());

        if (Boolean.TRUE.equals(pageType == 1)){
            flag = jsHelper.getIsLiveOrder();
        } else if (Boolean.TRUE.equals(pageType == 2)){
            flag = jsHelper.getIsMovieOrder();
        }  else if (Boolean.TRUE.equals(pageType == 3)){
            flag = jsHelper.getIsSeriesOrder();
        }

        RadioGroup rg = dialog.findViewById(R.id.rg);
        if (Boolean.TRUE.equals(flag)){
            rg.check(R.id.rd_1);
        } else {
            rg.check(R.id.rd_2);
        }

        dialog.findViewById(R.id.rd_1).setOnClickListener(view -> flag = true);
        dialog.findViewById(R.id.rd_2).setOnClickListener(view -> flag = false);

        dialog.findViewById(R.id.btn_cancel_filter).setOnClickListener(view -> {
            if (Boolean.TRUE.equals(pageType == 1)){
                jsHelper.setIsLiveOrder(false);
            } else if (Boolean.TRUE.equals(pageType == 2)){
                jsHelper.setIsMovieOrder(false);
            }  else if (Boolean.TRUE.equals(pageType == 3)){
                jsHelper.setIsSeriesOrder(false);
            }
            listener.onSubmit();
            dismissDialog();
        });
        dialog.findViewById(R.id.btn_submit_filter).setOnClickListener(view -> {
            if (Boolean.TRUE.equals(pageType == 1)){
                jsHelper.setIsLiveOrder(flag);
            } else if (Boolean.TRUE.equals(pageType == 2)){
                jsHelper.setIsMovieOrder(flag);
            }  else if (Boolean.TRUE.equals(pageType == 3)){
                jsHelper.setIsSeriesOrder(flag);
            }
            listener.onSubmit();
            dismissDialog();
        });

        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        dialog.show();
        Window window = dialog.getWindow();
        IfSupported.hideStatusBarDialog(window);
        window.setLayout(MATCH_PARENT, WRAP_CONTENT);
    }

    private void dismissDialog() {
        if (dialog != null && dialog.isShowing()){
            dialog.dismiss();
        }
    }

    public interface FilterDialogListener {
        void onSubmit();
    }
}
