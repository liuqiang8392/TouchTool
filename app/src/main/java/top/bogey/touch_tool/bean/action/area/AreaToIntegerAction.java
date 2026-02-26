package top.bogey.touch_tool.bean.action.area;

import android.graphics.Rect;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;

public class AreaToIntegerAction extends CalculateAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin leftPin = new Pin(new PinInteger(), R.string.area_left, true);
    private final transient Pin topPin = new Pin(new PinInteger(), R.string.area_top, true);
    private final transient Pin rightPin = new Pin(new PinInteger(), R.string.area_right, true);
    private final transient Pin bottomPin = new Pin(new PinInteger(), R.string.area_bottom, true);

    public AreaToIntegerAction() {
        super(ActionType.AREA_TO_INT);
        addPins(areaPin, leftPin, topPin, rightPin, bottomPin);
    }

    public AreaToIntegerAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, leftPin, topPin, rightPin, bottomPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        Rect areaRect = area.getValue();
        leftPin.getValue(PinInteger.class).setValue(areaRect.left);
        topPin.getValue(PinInteger.class).setValue(areaRect.top);
        rightPin.getValue(PinInteger.class).setValue(areaRect.right);
        bottomPin.getValue(PinInteger.class).setValue(areaRect.bottom);
    }
}
