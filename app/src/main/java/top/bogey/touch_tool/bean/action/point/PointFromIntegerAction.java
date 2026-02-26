package top.bogey.touch_tool.bean.action.point;

import android.graphics.Point;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;

public class PointFromIntegerAction extends CalculateAction {
    private final transient Pin xPin = new Pin(new PinInteger(), R.string.point_x);
    private final transient Pin yPin = new Pin(new PinInteger(), R.string.point_y);
    private final transient Pin pointPin = new Pin(new PinPoint(), R.string.pin_point, true);

    public PointFromIntegerAction() {
        super(ActionType.POINT_FROM_INT);
        addPins(xPin, yPin, pointPin);
    }

    public PointFromIntegerAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(xPin, yPin, pointPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> x = getPinValue(runnable, xPin);
        PinNumber<?> y = getPinValue(runnable, yPin);
        pointPin.getValue(PinPoint.class).setValue(new Point(x.intValue(), y.intValue()));
    }
}
