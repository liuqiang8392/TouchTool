package top.bogey.touch_tool.bean.action.point;

import android.graphics.Point;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;

public class PointToIntegerAction extends CalculateAction {
    private final transient Pin pointPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin xPin = new Pin(new PinInteger(), R.string.point_x, true);
    private final transient Pin yPin = new Pin(new PinInteger(), R.string.point_y, true);

    public PointToIntegerAction() {
        super(ActionType.POINT_TO_INT);
        addPins(pointPin, xPin, yPin);
    }

    public PointToIntegerAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pointPin, xPin, yPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinPoint point = getPinValue(runnable, pointPin);
        Point pointValue = point.getValue();
        xPin.getValue(PinInteger.class).setValue(pointValue.x);
        yPin.getValue(PinInteger.class).setValue(pointValue.y);
    }
}
