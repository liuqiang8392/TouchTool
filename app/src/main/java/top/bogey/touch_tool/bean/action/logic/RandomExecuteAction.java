package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.special_pin.AlwaysShowPin;
import top.bogey.touch_tool.service.TaskRunnable;

public class RandomExecuteAction extends ExecuteAction implements DynamicPinsAction {
    private final static Pin morePin = new Pin(new PinExecute(), R.string.pin_execute, true);

    private final transient Pin timesPin = new Pin(new PinInteger(1), R.string.random_action_times);
    private final transient Pin allowRepeatPin = new Pin(new PinBoolean(false), R.string.random_action_allow_repeat);

    private final transient Pin addPin = new AlwaysShowPin(new PinAdd(morePin), R.string.pin_add_execute, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.random_action_complete, true);

    private final Random random = new Random();

    public RandomExecuteAction() {
        super(ActionType.RANDOM_LOGIC);
        Pin secondPin = new Pin(new PinExecute(), R.string.pin_execute, true, true);
        addPins(timesPin, allowRepeatPin, secondPin, addPin, completePin);
    }

    public RandomExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(timesPin, allowRepeatPin);
        reAddPins(morePin);
        reAddPins(addPin, completePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Action startAction = runnable.getAction();
        PinNumber<?> times = getPinValue(runnable, timesPin);
        PinBoolean allowRepeat = getPinValue(runnable, allowRepeatPin);
        List<Pin> dynamicPins = getDynamicPins();
        for (int i = 0; i < times.intValue(); i++) {
            if (runnable.isCurrentInterrupt()) return;
            if (!startAction.equals(runnable.getAction())) return;
            if (dynamicPins.isEmpty()) break;
            int index = random.nextInt(dynamicPins.size());
            if (allowRepeat.getValue()) {
                executeNext(runnable, dynamicPins.get(index));
            } else {
                executeNext(runnable, dynamicPins.remove(index));
            }
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
        pins.add(outPin);
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
            if (pin == allowRepeatPin) start = true;
        }
        return pins;
    }
}
