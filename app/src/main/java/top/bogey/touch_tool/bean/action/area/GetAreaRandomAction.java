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

public class GetAreaRandomAction extends CalculateAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin posPin = new Pin(new PinPoint(), R.string.pin_point, true);

    public GetAreaRandomAction() {
        super(ActionType.GET_AREA_RANDOM);
        addPins(areaPin, posPin);
    }

    public GetAreaRandomAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, posPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        Rect areaRect = area.getValue();
        int x = (int) (areaRect.left + Math.random() * areaRect.width());
        int y = (int) (areaRect.top + Math.random() * areaRect.height());
        posPin.getValue(PinPoint.class).setValue(new Point(x, y));
    }
}
