package top.bogey.touch_tool.bean.action.start;

import android.content.Intent;

import com.google.gson.JsonObject;

import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class BroadcastStartAction extends StartAction {
    private final transient Pin actionPin = new Pin(new PinString(), R.string.broadcast_start_action_name);
    private final transient Pin receivedActionPin = new Pin(new PinString(), R.string.broadcast_start_action_received_name, true);
    private final transient Pin dataPin = new Pin(new PinString(), R.string.broadcast_start_action_data, true);
    private final transient Pin extrasPin = new Pin(new PinMap(new PinString(), new PinString()), R.string.broadcast_start_action_extra, true);

    public BroadcastStartAction() {
        super(ActionType.BROADCAST_START);
        addPins(actionPin, receivedActionPin, dataPin, extrasPin);
    }

    public BroadcastStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(actionPin, receivedActionPin, dataPin, extrasPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        TaskInfoSummary.BroadcastInfo broadcastInfo = TaskInfoSummary.getInstance().getBroadcastInfo();
        if (broadcastInfo == null) return;
        receivedActionPin.getValue(PinString.class).setValue(broadcastInfo.action());
        String data = broadcastInfo.data();
        dataPin.getValue(PinString.class).setValue(data == null ? "" : data);

        Map<String, String> extras = broadcastInfo.extras();
        extrasPin.setValue(PinBase.parseValue(extras));
        executeNext(runnable, executePin);
    }

    @Override
    public boolean ready() {
        TaskInfoSummary.BroadcastInfo info = TaskInfoSummary.getInstance().getBroadcastInfo();
        if (info == null) return false;
        String action = actionPin.getValue(PinString.class).getValue();
        if (action == null || action.isEmpty()) return false;
        return action.equals(info.action());
    }

    public String getAction() {
        return actionPin.getValue(PinString.class).getValue();
    }

    public static boolean isSystemAction(String action) {
        if (action == null) return false;
        return action.equals(Intent.ACTION_BATTERY_CHANGED)
                || action.equals(Intent.ACTION_SCREEN_ON)
                || action.equals(Intent.ACTION_SCREEN_OFF)
                || action.equals(Intent.ACTION_USER_PRESENT);
    }
}
