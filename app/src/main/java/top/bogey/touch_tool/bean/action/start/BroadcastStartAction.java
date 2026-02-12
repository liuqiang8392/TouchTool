package top.bogey.touch_tool.bean.action.start;

import com.google.gson.JsonObject;

import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleLineString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class BroadcastStartAction extends StartAction {
    private final transient Pin actionPin = new NotLinkAblePin(new PinSingleLineString(), R.string.broadcast_start_action_name);
    private final transient Pin extrasPin = new Pin(new PinMap(new PinString(), new PinString()), R.string.broadcast_start_action_extra, true);

    public BroadcastStartAction() {
        super(ActionType.BROADCAST_START);
        addPins(actionPin, extrasPin);
    }

    public BroadcastStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(actionPin, extrasPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        TaskInfoSummary.BroadcastInfo broadcastInfo = TaskInfoSummary.getInstance().getBroadcastInfo();
        if (broadcastInfo == null) return;

        Map<String, String> extras = broadcastInfo.extras();
        extrasPin.setValue(PinBase.parseValue(extras));

        executeNext(runnable, executePin);
    }

    @Override
    public boolean ready() {
        TaskInfoSummary.BroadcastInfo info = TaskInfoSummary.getInstance().getBroadcastInfo();
        if (info == null) return false;
        String action = getAction();
        if (action == null || action.isEmpty()) return false;
        return action.equals(info.action());
    }

    public String getAction() {
        return actionPin.getValue(PinString.class).getValue();
    }
}
