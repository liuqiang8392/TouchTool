package top.bogey.touch_tool.bean.action.normal;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.card.InputConfigActionFloatCard;
import top.bogey.touch_tool.utils.EAnchor;

public class InputConfigAction extends ExecuteAction implements DynamicPinsAction {
    private final transient Pin posTypePin = new NotLinkAblePin(new PinSingleSelect(R.array.float_pos_type, 0), R.string.pin_point, false, false, true);
    private final transient Pin anchorPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.window_anchor, false, false, true);
    private final transient Pin gravityPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.screen_anchor, false, false, true);
    private final transient Pin showPosPin = new PosShowablePin(new PinPoint(), R.string.screen_anchor_pos, false, false, true);
    private final transient Pin savePin = new NotLinkAblePin(new PinBoolean(true), R.string.input_config_action_save, false, false, true);
    private final transient Pin timeoutPin = new NotLinkAblePin(new PinInteger(0), R.string.input_config_action_timeout, false, false, true);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.input_config_action_else, true);


    public InputConfigAction() {
        super(ActionType.INPUT_CONFIG);
        addPins(posTypePin, anchorPin, gravityPin, showPosPin, savePin, timeoutPin, elsePin);
    }

    public InputConfigAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(posTypePin, anchorPin, gravityPin, showPosPin, savePin, timeoutPin, elsePin);
        tmpPins.forEach(this::addPin);
        tmpPins.clear();
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Map<String, Pin> inPins = new HashMap<>();
        Map<String, Pin> outPins = new HashMap<>();
        for (Pin dynamicPin : getDynamicPins()) {
            if (dynamicPin.isOut()) outPins.put(dynamicPin.getUid(), dynamicPin);
            else inPins.put(dynamicPin.getUid(), dynamicPin);
        }
        inPins.forEach((uid, inPin) -> {
            PinObject pinValue = getPinValue(runnable, inPin);
            inPin.getValue().sync(pinValue);
        });

        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);
        AtomicBoolean aResult = new AtomicBoolean(false);
        if (getTypeValue() == 0) {
            InputConfigActionFloatCard.showInputConfig(runnable.getTask(), this, timeout.intValue(), result -> {
                aResult.set(result);
                runnable.resume();
            });
        } else {
            PinSingleSelect anchor = getPinValue(runnable, anchorPin);
            PinSingleSelect gravity = getPinValue(runnable, gravityPin);
            PinPoint showPos = getPinValue(runnable, showPosPin);
            InputConfigActionFloatCard.showInputConfig(runnable.getTask(), this, timeout.intValue(), result -> {
                aResult.set(result);
                runnable.resume();
            }, EAnchor.values()[anchor.getIndex()], EAnchor.values()[gravity.getIndex()], showPos.getValue());
        }
        runnable.await();
        if (aResult.get()) {
            outPins.forEach((uid, outPin) -> {
                Pin inPin = inPins.get(uid);
                if (inPin != null) outPin.getValue().sync(inPin.getValue());
            });

            // 将当前值保存一下
            PinBoolean save = getPinValue(runnable, savePin);
            if (save.getValue()) {
                Task task = runnable.getTask();
                Task topTask = task.getTopParent();
                Task saveTask = TaskSaver.getInstance().getTask(topTask.getId());
                Task currentTask = saveTask.downFindTask(task.getId());
                Action action = currentTask.getAction(getId());
                if (action instanceof InputConfigAction inputConfigAction) {
                    for (Pin dynamicPin : inputConfigAction.getDynamicPins()) {
                        Pin pinById = getPinById(dynamicPin.getId());
                        dynamicPin.getValue().sync(pinById.getValue());
                    }
                    saveTask.save();
                }
            }
            executeNext(runnable, outPin);
        } else {
            executeNext(runnable, elsePin);
        }
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (start) pins.add(pin);
            if (pin == elsePin) start = true;
        }
        return pins;
    }

    @Override
    public void removePin(Task context, Pin pin) {
        super.removePin(context, pin);
        Pin pinByUid = getPinByUid(pin.getUid());
        if (pinByUid != null) super.removePin(context, pinByUid);
    }

    @Override
    public void onValueReplaced(Task task, Pin origin, PinBase value) {
        super.onValueReplaced(task, origin, value);
        for (Pin dynamicPin : getDynamicPins()) {
            if (dynamicPin == origin) continue;
            if (dynamicPin.getUid().equals(origin.getUid())) {
                if (dynamicPin.getValue().equals(value)) continue;
                dynamicPin.setValue(task, value.copy());
            }
        }
    }

    @Override
    public void onTitleChanged(Pin origin, String title) {
        super.onTitleChanged(origin, title);
        for (Pin dynamicPin : getDynamicPins()) {
            if (dynamicPin == origin) continue;
            if (dynamicPin.getUid().equals(origin.getUid())) {
                if (dynamicPin.getTitle().equals(title)) continue;
                dynamicPin.setTitle(title);
            }
        }
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
            InputConfigAction action = (InputConfigAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 1;
        }
    }
}
