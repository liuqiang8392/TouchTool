package top.bogey.touch_tool.bean.action.point;

import android.graphics.Point;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class PointToTouchAction extends CalculateAction implements DynamicPinsAction {
    private final static Pin posMorePin = new Pin(new PinPoint(), R.string.pin_point);
    private final static Pin timeMorePin = new Pin(new PinInteger(100), R.string.point_to_touch_action_time);
    private final transient Pin pointPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin timePin = new Pin(new PinInteger(100), R.string.point_to_touch_action_time);
    private final transient Pin addPin = new Pin(new PinAdd(Arrays.asList(posMorePin, timeMorePin)), R.string.pin_add_pin);
    private final transient Pin touchPin = new Pin(new PinTouchPath(), R.string.pin_touch, true);

    public PointToTouchAction() {
        super(ActionType.POINT_TO_TOUCH);
        addPins(pointPin, timePin, addPin, touchPin);
    }

    public PointToTouchAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pointPin, timePin);
        Pin pin = tmpPins.get(0);
        while (!pin.isSameClass(PinAdd.class)) {
            reAddPin(posMorePin.newCopy());
            reAddPin(timeMorePin.newCopy());
            pin = tmpPins.get(0);
        }
        reAddPins(addPin, touchPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        List<Pin> dynamicPins = getDynamicPins();
        List<PinTouchPath.PathPart> pathParts = new ArrayList<>();
        for (int i = 0; i < dynamicPins.size(); i += 2) {
            Pin pointPin = dynamicPins.get(i);
            Pin timePin = dynamicPins.get(i + 1);
            PinPoint point = getPinValue(runnable, pointPin);
            PinNumber<?> time = getPinValue(runnable, timePin);
            Point pos = point.getValue();
            PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart(time.intValue(), pos.x, pos.y);
            pathParts.add(pathPart);
        }
        PinTouchPath pinTouchPath = new PinTouchPath(pathParts);
        touchPin.setValue(pinTouchPath);
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (pin == pointPin) start = true;
            if (pin == addPin) start = false;
            if (start) pins.add(pin);
        }
        return pins;
    }

    @Override
    public void removePin(Pin pin) {
        List<Pin> pins = getPins();
        int index = pins.indexOf(pin);
        int i = index % 2;
        if (i == 0) {
            super.removePin(pins.get(index + 1));
            super.removePin(pin);
        }
        if (i == 1) {
            super.removePin(pin);
            super.removePin(pins.get(index - 1));
        }
    }

    @Override
    public void removePin(Task context, Pin pin) {
        List<Pin> pins = getPins();
        int index = pins.indexOf(pin);
        int i = index % 2;
        if (i == 0) {
            pin.clearLinks(context);
            pins.get(index + 1).clearLinks(context);
        }
        if (i == 1) {
            pin.clearLinks(context);
            pins.get(index - 1).clearLinks(context);
        }
        removePin(pin);
    }
}
