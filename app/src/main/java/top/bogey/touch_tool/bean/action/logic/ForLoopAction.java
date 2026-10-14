package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class ForLoopAction extends ExecuteAction {
    private final transient Pin breakPin = new Pin(new PinExecute(), R.string.for_loop_action_break);
    private final transient Pin startPin = new Pin(new PinInteger(1), R.string.for_loop_action_start);
    private final transient Pin endPin = new Pin(new PinInteger(5), R.string.for_loop_action_end);
    private final transient Pin stepPin = new Pin(new PinInteger(1), R.string.for_loop_action_step, false, false, true);
    private final transient Pin currentPin = new Pin(new PinInteger(), R.string.for_loop_action_current, true);
    private final transient Pin completePin = new Pin(new PinExecute(), R.string.for_loop_action_complete, true);

    private transient boolean isBreak = false;

    public ForLoopAction() {
        super(ActionType.FOR_LOGIC);
        addPins(breakPin, startPin, endPin, stepPin, currentPin, completePin);
    }

    public ForLoopAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(breakPin, startPin, endPin, stepPin, currentPin, completePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        if (pin == inPin) {
            isBreak = false;
            Action startAction = runnable.getAction();

            PinNumber<?> start = getPinValue(runnable, startPin);
            PinNumber<?> end = getPinValue(runnable, endPin);
            PinNumber<?> step = getPinValue(runnable, stepPin);
            int startValue = start.intValue();
            int endValue = end.intValue();
            int stepValue = step.intValue();
            if (stepValue == 0) stepValue = 1;
            stepValue = Math.abs(stepValue);

            int currentValue = startValue;
            while (startValue <= endValue ? currentValue <= endValue : currentValue >= endValue) {
                if (runnable.isCurrentInterrupt()) return;
                if (!startAction.equals(runnable.getAction())) return;
                if (isBreak) break;
                currentPin.getValue(PinInteger.class).setValue(currentValue);
                executeNext(runnable, outPin);
                if (startValue <= endValue) currentValue += stepValue;
                else currentValue -= stepValue;
            }
            executeNext(runnable, completePin);
        } else {
            isBreak = true;
            super.beforeExecuteNext(runnable, null);
        }
    }

    @Override
    public void beforeExecuteNext(TaskRunnable runnable, Pin pin) {
        if (pin == completePin) {
            super.beforeExecuteNext(runnable, pin);
        }
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        // currentPin 记录的是循环进行到哪一步，只能在循环真正开始时清空
        // 否则从 break 针脚进来（以及循环结束走 completePin）时会被清成 0，循环结束后再取值就拿不到最后一次的值
        // 与 ListForeachAction、MapForeachAction 的写法保持一致
        if (pin == inPin) {
            super.resetReturnValue(runnable, pin);
        }
    }
}
