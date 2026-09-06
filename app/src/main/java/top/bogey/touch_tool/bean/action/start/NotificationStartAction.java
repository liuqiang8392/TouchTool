package top.bogey.touch_tool.bean.action.start;

import android.app.Notification;

import com.google.gson.JsonObject;

import java.util.Map;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_application.PinApplication;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinApplications;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class NotificationStartAction extends StartAction {
    private final transient Pin appsPin = new NotLinkAblePin(new PinApplications(PinSubType.MULTI_APP, MainApplication.getInstance().getString(R.string.common_package)), R.string.pin_app);
    private final transient Pin typePin = new NotLinkAblePin(new PinSingleSelect(R.array.notification_type), R.string.notification_start_action_type);
    private final transient Pin notifyAppPin = new Pin(new PinApplication(PinSubType.SINGLE_APP), R.string.notification_start_action_notify_app, true);
    private final transient Pin notifyTextPin = new Pin(new PinString(), R.string.notification_start_action_notify_text, true);
    private final transient Pin notifyValuePin = new Pin(new PinMap(new PinString(), new PinString()), R.string.notification_start_action_notify_value, true);

    public NotificationStartAction() {
        super(ActionType.NOTIFICATION_START);
        addPins(appsPin, typePin, notifyAppPin, notifyTextPin, notifyValuePin);
    }

    public NotificationStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(appsPin, typePin, notifyAppPin, notifyTextPin, notifyValuePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        TaskInfoSummary.Notification notification = TaskInfoSummary.getInstance().getNotification(getId());
        PinApplication application = new PinApplication(notification.packageName());
        notifyAppPin.setValue(application);
        Map<String, String> content = notification.content();
        PinBase pinBase = PinBase.parseValue(content);
        notifyValuePin.setValue(pinBase);
        notifyTextPin.getValue(PinString.class).setValue(content.get(Notification.EXTRA_TEXT));
        executeNext(runnable, executePin);
    }

    @Override
    public boolean ready() {
        return false;
    }

    public boolean ready(TaskInfoSummary.Notification notification) {
        if (notification == null || notification.content() == null) return false;
        int index = typePin.getValue(PinSingleSelect.class).getIndex();
        if (index != notification.type().ordinal()) return false;
        PinApplication application = new PinApplication(notification.packageName());
        return appsPin.getValue(PinApplications.class).contains(application);
    }
}
