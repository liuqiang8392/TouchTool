package top.bogey.touch_tool.bean.action.area;

import android.graphics.Point;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.service.TaskRunnable;

public class CheckAreaContainPosAction extends CalculateAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin posPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin resultPin = new Pin(new PinBoolean(), R.string.pin_boolean_result, true);

    public CheckAreaContainPosAction() {
        super(ActionType.CHECK_AREA_CONTAIN_POS);
        addPins(areaPin, posPin, resultPin);
    }

    public CheckAreaContainPosAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, posPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        PinPoint pos = getPinValue(runnable, posPin);
        Point point = pos.getValue();
        resultPin.getValue(PinBoolean.class).setValue(area.getValue().contains(point.x, point.y));
    }
}
