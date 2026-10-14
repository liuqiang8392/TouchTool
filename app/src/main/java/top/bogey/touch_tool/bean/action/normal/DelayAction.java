package top.bogey.touch_tool.bean.action.normal;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueArea;
import top.bogey.touch_tool.service.TaskRunnable;

public class DelayAction extends ExecuteAction {
    private final transient Pin delay = new Pin(new PinValueArea(300, 300), R.string.delay_action_time);

    public DelayAction() {
        super(ActionType.DELAY);
        addPin(delay);
    }

    public DelayAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(delay);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinValueArea delayValue = getPinValue(runnable, delay);
        runnable.sleep(delayValue.getRandomValue());
        executeNext(runnable, outPin);
    }

    public Pin getDelay() {
        return delay;
    }
}
