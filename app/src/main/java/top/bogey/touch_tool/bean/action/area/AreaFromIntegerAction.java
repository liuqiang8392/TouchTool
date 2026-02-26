package top.bogey.touch_tool.bean.action.area;

import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;

public class AreaFromIntegerAction extends CalculateAction {
    private final transient Pin leftPin = new Pin(new PinInteger(), R.string.area_left);
    private final transient Pin topPin = new Pin(new PinInteger(), R.string.area_top);
    private final transient Pin rightPin = new Pin(new PinInteger(), R.string.area_right);
    private final transient Pin bottomPin = new Pin(new PinInteger(), R.string.area_bottom);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);

    public AreaFromIntegerAction() {
        super(ActionType.AREA_FROM_INT);
        addPins(leftPin, topPin, rightPin, bottomPin, areaPin);
    }

    public AreaFromIntegerAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(leftPin, topPin, rightPin, bottomPin, areaPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNumber<?> left = getPinValue(runnable, leftPin);
        PinNumber<?> top = getPinValue(runnable, topPin);
        PinNumber<?> right = getPinValue(runnable, rightPin);
        PinNumber<?> bottom = getPinValue(runnable, bottomPin);
        PinArea area = areaPin.getValue(PinArea.class);
        area.setValue(new Rect(left.intValue(), top.intValue(), right.intValue(), bottom.intValue()));
    }
}
