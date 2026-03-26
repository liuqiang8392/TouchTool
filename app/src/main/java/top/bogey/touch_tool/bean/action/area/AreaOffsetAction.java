package top.bogey.touch_tool.bean.action.area;

import android.graphics.Point;
import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;

public class AreaOffsetAction extends CalculateAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin offsetPin = new Pin(new PinPoint(), R.string.point_offset_action_offset);
    private final transient Pin resultPin = new Pin(new PinArea(), R.string.pin_boolean_result, true);

    public AreaOffsetAction() {
        super(ActionType.AREA_OFFSET);
        addPins(areaPin, offsetPin, resultPin);
    }

    public AreaOffsetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, offsetPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        Rect areaRect = area.getValue();
        PinPoint offset = getPinValue(runnable, offsetPin);
        Point point = offset.getValue();
        areaRect.offset(point.x, point.y);

        resultPin.getValue(PinArea.class).setValue(areaRect);
    }
}
