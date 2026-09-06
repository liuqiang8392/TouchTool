package top.bogey.touch_tool.bean.action.parent;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public abstract class FindExecuteAction extends ExecuteAction {
    protected final transient Pin elsePin = new Pin(new PinExecute(), R.string.if_action_else, true);
    protected final transient Pin timeoutPin = new Pin(new PinInteger(3000), R.string.wait_if_action_timeout, false, false, true);
    protected final transient Pin intervalPin = new Pin(new PinInteger(100), R.string.find_execute_action_interval, false, false, true);

    public FindExecuteAction(ActionType type) {
        super(type);
        addPins(elsePin, timeoutPin, intervalPin);
    }

    public FindExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(elsePin, timeoutPin, intervalPin);
    }

    @Override
    public final void execute(TaskRunnable runnable, Pin pin) {
        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);
        PinNumber<?> interval = getPinValue(runnable, intervalPin);

        long startTime = System.currentTimeMillis();
        boolean found = find(runnable);
        while (!found) {
            if (runnable.isCurrentInterrupt()) return;
            if (timeout.intValue() < System.currentTimeMillis() - startTime) break;
            runnable.sleep(interval.intValue());
            found = find(runnable);
        }

        if (found) {
            executeNext(runnable, outPin);
        } else {
            executeNext(runnable, elsePin);
        }
    }

    public abstract boolean find(TaskRunnable runnable);
}
