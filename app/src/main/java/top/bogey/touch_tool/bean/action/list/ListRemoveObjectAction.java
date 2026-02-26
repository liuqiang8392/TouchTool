package top.bogey.touch_tool.bean.action.list;

import androidx.annotation.NonNull;

import com.google.gson.JsonObject;

import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.service.TaskRunnable;

public class ListRemoveObjectAction extends ListExecuteAction {
    private final transient Pin listPin = new Pin(new PinList());
    private final transient Pin objectPin = new Pin(new PinObject(PinSubType.DYNAMIC), R.string.pin_object);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public ListRemoveObjectAction() {
        super(ActionType.LIST_REMOVE_OBJECT);
        addPins(listPin, objectPin, resultPin);
    }

    public ListRemoveObjectAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(listPin);
        reAddPin(objectPin, true);
        reAddPin(resultPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinList list = getPinValue(runnable, listPin);
        PinObject object = getPinValue(runnable, objectPin);
        boolean removed = list.remove(object);
        resultPin.getValue(PinBoolean.class).setValue(removed);
        executeNext(runnable, outPin);
    }

    @NonNull
    @Override
    public List<Pin> getDynamicTypePins() {
        return Arrays.asList(listPin, objectPin);
    }
}
