package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.service.TaskRunnable;

public class CustomStartAction extends Action implements DynamicPinsAction {

    public CustomStartAction() {
        super(ActionType.CUSTOM_START);
        setExpandType(ExpandType.FULL);
        addFlag(SINGLE_IN_TASK);

        addPin(new Pin(new PinExecute(), R.string.pin_execute, true, true));
    }

    public CustomStartAction(JsonObject jsonObject) {
        super(jsonObject);
        addFlag(SINGLE_IN_TASK);
        tmpPins.forEach(this::addPin);
        tmpPins.clear();
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        runnable.addExecuteProgress(this);
        runnable.addDebugLog(this, 1);
        Pin pinByUid = getPinByUid(pin.getUid());
        // 如果pin不是执行针脚，便利所有针脚找到执行针脚
        if (pinByUid == null && !pin.isVertical()) {
            for (Pin p : getPins()) {
                if (p.isVertical()) {
                    pinByUid = p;
                    break;
                }
            }
        }
        executeNext(runnable, pinByUid);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {

    }

    public void setParams(Map<String, PinObject> params) {
        if (params == null) return;
        params.forEach((key, value) -> {
            Pin pin = getPinByUid(key);
            if (pin == null) return;
            pin.setValue(value);
        });
    }

    @Override
    public List<Pin> getDynamicPins() {
        return new ArrayList<>(getPins());
    }

    @Override
    public Pin findConnectToAblePin(Pin pin) {
        return getPins().stream().filter(p -> p.linkAble() && p.linkAble(pin.getValue())).findFirst().orElse(null);
    }
}
