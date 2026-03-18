package top.bogey.touch_tool.service.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static boolean BOOT_COMPLETED = false;

    public static boolean isBootCompleted() {
        return BOOT_COMPLETED || SystemClock.elapsedRealtime() > 1000 * 60;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            BOOT_COMPLETED = true;
            MainAccessibilityService service = MainApplication.getInstance().getService();
            if (service != null && service.isEnabled()) service.tryStartMainActivity();
        }
    }
}
