package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.special_pin.AlwaysShowPin;
import top.bogey.touch_tool.service.TaskRunnable;

public class SequenceExecuteAction extends Action implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinExecute(), R.string.pin_execute, true);

    private final transient Pin inPin = new Pin(new PinExecute(), R.string.pin_execute);
    private final transient Pin addPin = new AlwaysShowPin(new PinAdd(morePin), R.string.pin_add_execute, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.sequence_action_complete, true);

    public SequenceExecuteAction() {
        super(ActionType.SEQUENCE_LOGIC);
        // 默认的首个分支针脚，和动态添加的分支一样可以移除
        Pin firstPin = new Pin(new PinExecute(), R.string.pin_execute, true, true);
        addPins(inPin, firstPin, addPin, completePin);
    }

    public SequenceExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(inPin);
        reAddPins(morePin);
        reAddPins(addPin, completePin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {

    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Action startAction = runnable.getAction();
        for (Pin dynamicPin : getDynamicPins()) {
            if (runnable.isCurrentInterrupt()) return;
            if (!startAction.equals(runnable.getAction())) return;
            executeNext(runnable, dynamicPin);
        }
        executeNext(runnable, completePin);
    }

    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == completePin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
            if (pin == inPin) start = true;
        }
        return pins;
    }
}
