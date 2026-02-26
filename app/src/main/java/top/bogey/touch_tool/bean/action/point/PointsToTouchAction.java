package top.bogey.touch_tool.bean.action.point;

import android.graphics.Point;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.service.TaskRunnable;

public class PointsToTouchAction extends CalculateAction {
    private final transient Pin pointsPin = new Pin(new PinList(new PinPoint()), R.string.pin_point);
    private final transient Pin timePin = new Pin(new PinInteger(500), R.string.points_to_touch_action_time);
    private final transient Pin touchPin = new Pin(new PinTouchPath(), R.string.pin_touch, true);

    public PointsToTouchAction() {
        super(ActionType.POINTS_TO_TOUCH);
        addPins(pointsPin, timePin, touchPin);
    }

    public PointsToTouchAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(pointsPin, timePin, touchPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinList points = getPinValue(runnable, pointsPin);
        PinNumber<?> time = getPinValue(runnable, timePin);

        List<PinTouchPath.PathPart> pathParts = new ArrayList<>();

        int everyTime = time.intValue();
        int firstTime = everyTime;
        if (points.size() > 1) {
            firstTime = 0;
            everyTime = everyTime / (points.size() - 1);
        }

        for (int i = 0; i < points.size(); i++) {
            PinPoint point = (PinPoint) points.get(i);
            Point pos = point.getValue();
            PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart(i == 0 ? firstTime : everyTime, pos.x, pos.y);
            pathParts.add(pathPart);
        }
        PinTouchPath pinTouchPath = new PinTouchPath(pathParts);
        touchPin.setValue(pinTouchPath);
    }
}
