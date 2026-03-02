package top.bogey.touch_tool.bean.action.logic;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinIconExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.AlwaysShowPin;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.ChoiceExecuteFloatView;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class ChoiceExecuteAction extends Action implements DynamicPinsAction {
    private final transient Pin inPin = new Pin(new PinExecute(), R.string.pin_execute);
    private final transient Pin outPin = new Pin(new PinIconExecute(), R.string.pin_execute, true);
    private final static Pin morePin = new Pin(new PinIconExecute(), R.string.pin_execute, true);


    private final transient Pin timeoutPin = new NotLinkAblePin(new PinInteger(0), R.string.choice_action_timeout);
    private final transient Pin posTypePin = new Pin(new PinSingleSelect(R.array.float_pos_type, 0), R.string.pin_point, false, false, true);

    private final transient Pin anchorPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.window_anchor, false, false, true);
    private final transient Pin gravityPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.screen_anchor, false, false, true);
    private final transient Pin posPin = new PosShowablePin(new PinPoint(), R.string.screen_anchor_pos, false, false, true);

    private final transient Pin secondPin = new Pin(new PinIconExecute(), R.string.pin_execute, true);
    private final transient Pin addPin = new AlwaysShowPin(new PinAdd(morePin), R.string.pin_add_execute, true);
    private final transient Pin defaultPin = new Pin(new PinExecute(), R.string.choice_action_default, true);

    public ChoiceExecuteAction() {
        super(ActionType.CHOICE_LOGIC);
        addPins(inPin, outPin, timeoutPin, posTypePin, anchorPin, gravityPin, posPin, secondPin, addPin, defaultPin);
    }

    public ChoiceExecuteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(inPin, outPin, timeoutPin, posTypePin, anchorPin, gravityPin, posPin, secondPin);
        reAddPins(morePin);
        reAddPins(addPin, defaultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        List<ChoiceExecuteFloatView.Choice> choices = new ArrayList<>();
        for (Pin dynamicPin : getDynamicPins()) {
            Action nextAction = getNextAction(runnable, dynamicPin);
            if (nextAction == null) continue;
            choices.add(new ChoiceExecuteFloatView.Choice(dynamicPin.getId(), dynamicPin.getValue(PinIconExecute.class).getValue(), dynamicPin.getValue(PinIconExecute.class).getImage()));
        }

        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);
        AtomicReference<String> nextPinId = new AtomicReference<>();

        if (getTypeValue() == 0) {
            ChoiceExecuteFloatView.showChoice(getValidDescription(), choices, result -> {
                nextPinId.set(result);
                runnable.resume();
            });
        } else {
            PinSingleSelect anchor = getPinValue(runnable, anchorPin);
            PinSingleSelect gravity = getPinValue(runnable, gravityPin);
            PinPoint point = getPinValue(runnable, posPin);
            ChoiceExecuteFloatView.showChoice(getValidDescription(), choices, result -> {
                nextPinId.set(result);
                runnable.resume();
            }, EAnchor.values()[anchor.getIndex()], EAnchor.values()[gravity.getIndex()], point.getValue());
        }

        runnable.await(timeout.intValue());
        FloatWindow.dismiss(ChoiceExecuteFloatView.class.getName());

        String pinId = nextPinId.get();
        if (pinId != null) {
            Pin nextPin = getPinById(pinId);
            if (nextPin != null) {
                executeNext(runnable, nextPin);
                return;
            }
        }
        executeNext(runnable, defaultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {

    }

    private Action getNextAction(TaskRunnable runnable, Pin pin) {
        Task task = runnable.getTask();
        Pin linkedPin = pin.getLinkedPin(runnable.getTask());
        if (linkedPin == null) return null;
        return task.getAction(linkedPin.getOwnerId());
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        pins.add(outPin);
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == secondPin) start = true;
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
        }
        return pins;
    }

    private int getTypeValue() {
        PinSingleSelect type = posTypePin.getValue();
        return type.getIndex();
    }


    private static class PosShowablePin extends ShowAblePin {
        public PosShowablePin(PinBase value, int titleId, boolean out, boolean dynamic, boolean hide) {
            super(value, titleId, out, dynamic, hide);
        }

        @Override
        public boolean showAble(Task context) {
            ChoiceExecuteAction action = (ChoiceExecuteAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 1;
        }
    }
}
