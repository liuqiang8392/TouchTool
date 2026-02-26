package top.bogey.touch_tool.bean.action.point;

import android.graphics.Point;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;

public class PointOffsetAction extends CalculateAction {
    private final transient Pin pointPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin offsetPin = new Pin(new PinPoint(), R.string.point_offset_action_offset);
    private final transient Pin resultPin = new Pin(new PinPoint(), R.string.pin_boolean_result, true);

    public PointOffsetAction() {
        super(ActionType.POINT_OFFSET);
        addPins(pointPin, offsetPin, resultPin);
    }

    public PointOffsetAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pointPin, offsetPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinPoint point = getPinValue(runnable, pointPin);
        PinPoint offset = getPinValue(runnable, offsetPin);
        Point pointValue = point.getValue();
        Point offsetValue = offset.getValue();
        resultPin.getValue(PinPoint.class).setValue(new Point(pointValue.x + offsetValue.x, pointValue.y + offsetValue.y));
    }
}
