package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.ChoiceExecuteFloatView.Choice;
import top.bogey.touch_tool.ui.custom.float_view.ListChoiceFloatView;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class ListChoiceAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin timeoutPin = new NotLinkAblePin(new PinInteger(0), R.string.list_choice_action_timeout);

    private final transient Pin posTypePin = new Pin(new PinSingleSelect(R.array.float_pos_type, 0), R.string.pin_point, false, false, true);
    private final transient Pin anchorPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.window_anchor, false, false, true);
    private final transient Pin gravityPin = new PosShowablePin(new PinSingleSelect(R.array.anchor, 4), R.string.screen_anchor, false, false, true);
    private final transient Pin posPin = new PosShowablePin(new PinPoint(), R.string.screen_anchor_pos, false, false, true);

    private final transient Pin resultPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object, true);
    private final transient Pin defaultPin = new Pin(new PinExecute(), R.string.list_choice_action_default, true);

    public ListChoiceAction() {
        super(ActionType.LIST_CHOICE);
        addPins(listPin, timeoutPin, posTypePin, anchorPin, gravityPin, posPin, resultPin, defaultPin);
    }

    public ListChoiceAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, timeoutPin, posTypePin, anchorPin, gravityPin, posPin);
        reAddPin(resultPin, true);
        reAddPin(defaultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            PinObject pinObject = list.get(i);
            choices.add(new Choice(String.valueOf(i), pinObject.toString(), null));
        }

        PinNumber<?> timeout = getPinValue(runnable, timeoutPin);
        AtomicReference<String> resultIndex = new AtomicReference<>();

        if (getTypeValue() == 0) {
            ListChoiceFloatView.showChoice(getValidDescription(), choices, result -> {
                resultIndex.set(result);
                runnable.resume();
            });
        } else {
            PinSingleSelect anchor = getPinValue(runnable, anchorPin);
            PinSingleSelect gravity = getPinValue(runnable, gravityPin);
            PinPoint point = getPinValue(runnable, posPin);
            ListChoiceFloatView.showChoice(getValidDescription(), choices, result -> {
                resultIndex.set(result);
                runnable.resume();
            }, EAnchor.values()[anchor.getIndex()], EAnchor.values()[gravity.getIndex()], point.getValue());
        }

        runnable.await(timeout.intValue());
        FloatWindow.dismiss(ListChoiceFloatView.class.getName());

        String index = resultIndex.get();
        if (index != null) {
            int i = Integer.parseInt(index);
            resultPin.setValue(returnValue(list.get(i)));
            executeNext(runnable, outPin);
            return;
        }
        executeNext(runnable, defaultPin);
    }

    private int getTypeValue() {
        PinSingleSelect type = posTypePin.getValue();
        return type.getIndex();
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }

    private static class PosShowablePin extends ShowAblePin {
        public PosShowablePin(PinBase value, int titleId, boolean out, boolean dynamic, boolean hide) {
            super(value, titleId, out, dynamic, hide);
        }

        @Override
        public boolean showAble(Task context) {
            ListChoiceAction action = (ListChoiceAction) context.getAction(getOwnerId());
            return action.getTypeValue() == 1;
        }
    }
}
