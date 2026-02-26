package top.bogey.touch_tool.bean.action.area;

import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.service.TaskRunnable;

public class CheckAreaRelationAction extends CalculateAction {
    private final transient Pin firstAreaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin secondAreaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin resultPin = new Pin(new PinSingleSelect(R.array.area_relation), R.string.check_area_relation_action_relation, true);

    public CheckAreaRelationAction() {
        super(ActionType.CHECK_AREA_RELATION);
        addPins(firstAreaPin, secondAreaPin, resultPin);
    }

    public CheckAreaRelationAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(firstAreaPin, secondAreaPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea firstArea = getPinValue(runnable, firstAreaPin);
        Rect firstRect = firstArea.getValue();
        PinArea secondArea = getPinValue(runnable, secondAreaPin);
        Rect secondRect = secondArea.getValue();
        if (firstRect.contains(secondRect)) {
            resultPin.getValue(PinSingleSelect.class).setIndex(0);
        } else if (Rect.intersects(firstRect, secondRect)) {
            resultPin.getValue(PinSingleSelect.class).setIndex(1);
        } else {
            resultPin.getValue(PinSingleSelect.class).setIndex(2);
        }
    }
}
