package top.bogey.touch_tool.bean.action.start;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;

public class BatteryStartAction extends StartAction {
    private final transient Pin statePin = new Pin(new PinSingleSelect(R.array.charging_state), R.string.battery_start_action_state, true);
    private final transient Pin valuePin = new Pin(new PinInteger(), R.string.battery_start_action_value, true);

    public BatteryStartAction() {
        super(ActionType.BATTERY_START);
        addPins(statePin, valuePin);
    }

    public BatteryStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(statePin, valuePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        TaskInfoSummary.BatteryInfo batteryInfo = TaskInfoSummary.getInstance().getBatteryInfo();
        statePin.getValue(PinSingleSelect.class).setIndex(batteryInfo.status().ordinal());
        valuePin.getValue(PinInteger.class).setValue(batteryInfo.percent());
        executeNext(runnable, executePin);
    }
}
