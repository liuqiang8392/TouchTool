package top.bogey.touch_tool.bean.action.parent;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public abstract class ExecuteOrCalculateAction extends Action {
    protected final transient Pin inPin = new ExecuteShowablePin(new PinExecute(), R.string.pin_execute, false);
    protected final transient Pin outPin = new ExecuteShowablePin(new PinExecute(), R.string.pin_execute, true);
    protected final transient Pin realtimeModePin = new NotLinkAblePin(new PinBoolean(false), R.string.realtime_mode, false, false, true);

    protected ExecuteOrCalculateAction(ActionType type) {
        super(type);
        addPins(inPin, outPin, realtimeModePin);
    }

    protected ExecuteOrCalculateAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(inPin, outPin, realtimeModePin);
    }

    public Pin getInPin() {
        return inPin;
    }

    public Pin getOutPin() {
        return outPin;
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        if (isRealtimeMode()) return;
        doAction(runnable, pin);
        executeNext(runnable, outPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        if (isRealtimeMode()) doAction(runnable, pin);
    }

    protected abstract void doAction(TaskRunnable runnable, Pin pin);

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (isRealtimeMode() || (!pin.isOut() && pin.isSameClass(PinExecute.class))) {
            for (Pin p : getPins()) {
                if (p.isOut()) {
                    if (p.getValue() instanceof PinObject pinObject) {
                        pinObject.reset();
                    }
                }
            }
        }
    }

    @Override
    public void onValueUpdated(Task task, Pin origin, PinBase value) {
        super.onValueUpdated(task, origin, value);
        if (origin.equals(realtimeModePin)) {
            Pin inLinkedPin = inPin.getLinkedPin(task);
            Pin outLinkedPin = outPin.getLinkedPin(task);
            if (inLinkedPin != null && outLinkedPin != null) {
                inLinkedPin.mutualAddLink(task, outLinkedPin);
            }
            inPin.clearLinks(task);
            outPin.clearLinks(task);
        }
    }

    protected boolean isRealtimeMode() {
        return realtimeModePin.getValue(PinBoolean.class).getValue();
    }

    public void setRealtimeMode(boolean realtimeMode) {
        realtimeModePin.getValue(PinBoolean.class).setValue(realtimeMode);
    }

    protected static class ExecuteShowablePin extends ShowAblePin {
        public ExecuteShowablePin(PinBase value) {
            super(value);
        }

        public ExecuteShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        public ExecuteShowablePin(PinBase value, boolean out) {
            super(value, out);
        }

        public ExecuteShowablePin(PinBase value, int titleId, boolean out) {
            super(value, titleId, out);
        }

        @Override
        public boolean showAble(Task context) {
            ExecuteOrCalculateAction action = (ExecuteOrCalculateAction) context.getAction(getOwnerId());
            return !action.isRealtimeMode();
        }
    }
}
