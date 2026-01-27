package top.bogey.touch_tool.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.service.capture.CaptureService;

public class KeepAliveService extends Service {
    private final static int NOTIFICATION_ID = (int) (Math.random() * Integer.MAX_VALUE);

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotification();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannel runningChannel = notificationManager.getNotificationChannel(CaptureService.FOREGROUND_NOTIFICATION_CHANNEL);
            if (runningChannel == null) {
                runningChannel = new NotificationChannel(CaptureService.FOREGROUND_NOTIFICATION_CHANNEL, getString(R.string.app_setting_forge_service_channel_title), NotificationManager.IMPORTANCE_DEFAULT);
                runningChannel.setDescription(getString(R.string.app_setting_forge_service_channel_desc));
                notificationManager.createNotificationChannel(runningChannel);
            }

            Notification foregroundNotification = new NotificationCompat.Builder(this, CaptureService.FOREGROUND_NOTIFICATION_CHANNEL).build();
            startForeground(NOTIFICATION_ID, foregroundNotification);
        }
    }
}