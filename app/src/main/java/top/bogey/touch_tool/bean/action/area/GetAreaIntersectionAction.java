package top.bogey.touch_tool.bean.action.area;

import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetAreaIntersectionAction extends CalculateAction {
    private final transient Pin firstAreaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin secondAreaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin resultPin = new Pin(new PinArea(), R.string.pin_area, true);

    public GetAreaIntersectionAction() {
        super(ActionType.GET_AREA_INTERSECTION);
        addPins(firstAreaPin, secondAreaPin, resultPin);
    }

    public GetAreaIntersectionAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(firstAreaPin, secondAreaPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea firstArea = getPinValue(runnable, firstAreaPin);
        Rect firstRect = firstArea.getValue();
        PinArea secondArea = getPinValue(runnable, secondAreaPin);
        Rect secondRect = secondArea.getValue();
        Rect intersect = new Rect();
        if (intersect.setIntersect(firstRect, secondRect)) {
            resultPin.getValue(PinArea.class).setValue(intersect);
        }
    }
}
