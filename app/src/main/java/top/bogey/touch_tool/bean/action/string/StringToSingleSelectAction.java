package top.bogey.touch_tool.bean.action.string;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Collections;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class StringToSingleSelectAction extends CalculateAction {
    private final transient Pin textPin = new Pin(new PinString(), R.string.string_to_single_select_action_select);
    private final transient Pin singleSelectPin = new Pin(new PinSingleSelect(), R.string.pin_string_select, true);

    public StringToSingleSelectAction() {
        super(ActionType.STRING_TO_SINGLE_SELECT);
        addPins(textPin, singleSelectPin);
    }

    public StringToSingleSelectAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(textPin, singleSelectPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinObject text = getPinValue(runnable, textPin);
        PinSingleSelect singleSelect = singleSelectPin.getValue();
        Pin linkedPin = singleSelectPin.getLinkedPin(runnable.getTask());
        if (linkedPin == null) return;
        PinBase value = linkedPin.getValue();
        if (value instanceof PinSingleSelect pinSingleSelect) {
            singleSelect.setOptions(pinSingleSelect.getOptions());
            singleSelect.setValue(text.toString());
        } else if (value instanceof PinString) {
            singleSelect.setOptions(new ArrayList<>(Collections.singletonList(text.toString())));
            singleSelect.setValue(text.toString());
        }
    }
}
