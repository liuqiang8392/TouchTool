package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListSubListAction extends ListCalculateAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin startIndexPin = new Pin(new PinInteger(1), R.string.list_sublist_action_start);
    private final transient Pin endIndexPin = new Pin(new PinInteger(1), R.string.list_sublist_action_end);
    private final transient Pin resultPin = new Pin(new PinList(), R.string.pin_object, true);

    public ListSubListAction() {
        super(ActionType.LIST_SUBLIST);
        addPins(listPin, startIndexPin, endIndexPin, resultPin);
    }

    public ListSubListAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(listPin, startIndexPin, endIndexPin);
        reAddPin(resultPin, true);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinNumber<?> startIndex = getPinValue(runnable, startIndexPin);
        PinNumber<?> endIndex = getPinValue(runnable, endIndexPin);

        int start = startIndex.intValue();
        int end = endIndex.intValue();

        if (start < 0) {
            start += list.size() + 1;
        }
        if (end < 0) {
            end += list.size() + 1;
        }

        int fromIndex = Math.max(0, Math.min(start - 1, list.size()));
        int toIndex = Math.max(fromIndex, Math.min(end, list.size()));

        List<PinObject> subList = list.subList(fromIndex, toIndex);
        PinList result = new PinList();
        for (PinObject obj : subList) {
            result.add((PinObject) obj.copy());
        }
        resultPin.setValue(returnValue(result));
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, resultPin);
    }
}
