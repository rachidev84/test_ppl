package nemosofts.streambox.view;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.NonNull;

import nemosofts.streambox.R;
import nemosofts.streambox.util.IfSupported;

public class NSoftsProgressDialog extends Dialog {

    public NSoftsProgressDialog(@NonNull Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_custom_progress);
        setCancelable(false);
        setCanceledOnTouchOutside(false);

        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            hideStatusBarIfSupported(window);
        }
    }

    private void hideStatusBarIfSupported(Window window) {
        try {
            IfSupported.hideStatusBarDialog(window);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}