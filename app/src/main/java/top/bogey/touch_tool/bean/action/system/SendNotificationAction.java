package top.bogey.touch_tool.bean.action.system;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Icon;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.gson.JsonObject;

import java.util.UUID;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.InstantActivity;

public class SendNotificationAction extends ExecuteAction {
    private final static String CUSTOM_NOTIFICATION_CHANNEL = "CUSTOM_NOTIFICATION_CHANNEL";

    private final transient Pin titlePin = new Pin(new PinString(), R.string.send_notification_action_title);
    private final transient Pin contentPin = new Pin(new PinString(), R.string.send_notification_action_content);
    private final transient Pin iconPin = new Pin(new PinImage(PinSubType.WITH_ICON), R.string.send_notification_action_icon);
    private final transient Pin autoCancelPin = new Pin(new PinBoolean(true), R.string.send_notification_action_auto_cancel);
    private final transient Pin executePin = new Pin(new PinExecute(), R.string.send_notification_action_execute, true);
    private final transient Pin overridePin = new Pin(new PinBoolean(true), R.string.send_notification_action_override);

    public SendNotificationAction() {
        super(ActionType.SEND_NOTIFICATION);
        addPins(titlePin, contentPin, iconPin, autoCancelPin, executePin, overridePin);
    }

    public SendNotificationAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(titlePin, contentPin, iconPin, autoCancelPin, executePin, overridePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject title = getPinValue(runnable, titlePin);
        PinObject content = getPinValue(runnable, contentPin);
        PinImage icon = getPinValue(runnable, iconPin);
        PinBoolean autoCancel = getPinValue(runnable, autoCancelPin);
        PinBoolean override = getPinValue(runnable, overridePin);

        Context context = MainApplication.getInstance();
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CUSTOM_NOTIFICATION_CHANNEL, context.getString(R.string.send_notification_action_title), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.send_notification_action_desc));
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CUSTOM_NOTIFICATION_CHANNEL);
        builder.setContentTitle(title.toString());
        builder.setContentText(content.toString());
        builder.setStyle(new NotificationCompat.BigTextStyle().bigText(content.toString()));
        builder.setAutoCancel(autoCancel.getValue());
        if (icon.getImage() != null) {
            builder.setSmallIcon(IconCompat.createWithBitmap(icon.getImage()));
            builder.setLargeIcon(icon.getImage());
        } else {
            builder.setSmallIcon(R.mipmap.ic_launcher);
            builder.setLargeIcon(Icon.createWithResource(context, R.mipmap.ic_launcher));
        }

        String notificationId = runnable.getTask().getId() + getId();
        if (!override.getValue()) {
            notificationId = UUID.randomUUID().toString();
        }

        Pin linkedPin = executePin.getLinkedPin(runnable.getTask());
        if (linkedPin != null) {
            Intent intent = new Intent(context, InstantActivity.class);
            intent.setAction(InstantActivity.INTENT_KEY_DO_ACTION);
            intent.putExtra(InstantActivity.TASK_ID, runnable.getTask().getId());
            intent.putExtra(InstantActivity.ACTION_ID, getId());
            intent.putExtra(InstantActivity.PIN_ID, executePin.getId());
            PendingIntent pendingIntent = PendingIntent.getActivity(context, notificationId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE);
            builder.setContentIntent(pendingIntent);
        }
        notificationManager.notify(notificationId.hashCode(), builder.build());

        executeNext(runnable, outPin);
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            String permission = Manifest.permission.POST_NOTIFICATIONS;
            if (MainApplication.getInstance().checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_need_notification_warning);
            }
        }
    }
}
