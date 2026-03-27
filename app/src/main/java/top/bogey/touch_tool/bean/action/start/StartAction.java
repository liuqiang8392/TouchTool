package top.bogey.touch_tool.bean.action.start;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.service.TaskRunnable;

public abstract class StartAction extends Action {

    private final transient Pin enablePin = new NotLinkAblePin(new PinBoolean(true), R.string.start_action_enable);
    private final transient Pin restartPin = new NotLinkAblePin(new PinSingleSelect(R.array.restart_type), R.string.start_action_restart);
    private final transient Pin breakPin = new Pin(new PinBoolean(false), R.string.start_action_break);
    protected final transient Pin showPin = new NotLinkAblePin(new PinBoolean(false), R.string.start_action_show);
    protected final transient Pin executePin = new Pin(new PinExecute(), R.string.pin_execute, true);

    private transient boolean checking;

    protected StartAction(ActionType type) {
        super(type);
        addPins(enablePin, restartPin, breakPin, showPin, executePin);
    }

    protected StartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(enablePin, restartPin, breakPin, showPin, executePin);
    }

    public boolean ready() {
        return true;
    }

    public boolean stop(TaskRunnable runnable) {
        if (checking) return false;
        checking = true;
        PinBoolean stop = getPinValue(runnable, breakPin);
        checking = false;
        return stop.getValue();
    }

    public boolean isEnable() {
        return enablePin.getValue(PinBoolean.class).getValue();
    }

    public void setEnable(boolean enable) {
        enablePin.getValue(PinBoolean.class).setValue(enable);
    }

    public boolean isShow() {
        return showPin.getValue(PinBoolean.class).getValue();
    }

    public RestartType getRestartType() {
        int index = restartPin.getValue(PinSingleSelect.class).getIndex();
        if (index > 0 && index < RestartType.values().length) return RestartType.values()[index];
        return RestartType.RESTART;
    }

    public Pin getExecutePin() {
        return executePin;
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        runnable.addDebugLog(this, 1);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {

    }


    public enum RestartType {
        RESTART, CANCEL, NEW
    }
}
