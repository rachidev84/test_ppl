package nemosofts.streambox.util;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import nemosofts.streambox.activity.LauncherActivity;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, @NonNull Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Start the MainActivity after boot
            Intent startIntent = new Intent(context, LauncherActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startIntent);

            // Optional: Show a toast message for testing purposes
            Toast.makeText(context, "App Auto-Started after Boot", Toast.LENGTH_SHORT).show();
        }
    }
}