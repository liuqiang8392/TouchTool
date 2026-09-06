package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListIndexOfAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin elementPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin indexPin = new Pin(new PinInteger(), R.string.list_action_index, true);

    public ListIndexOfAction() {
        super(ActionType.LIST_INDEX_OF);
        addPins(listPin, elementPin, indexPin);
    }

    public ListIndexOfAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(listPin);
        reAddPin(elementPin, true);
        reAddPin(indexPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinObject element = getPinValue(runnable, elementPin);
        int index = list.indexOf(element);
        indexPin.getValue(PinInteger.class).setValue(index + 1);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, elementPin);
    }

}
