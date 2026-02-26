package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Build;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.system.SwitchCaptureAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetColorAction extends CalculateAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image, false, false, true);
    private final transient Pin posPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin colorPin = new Pin(new PinColor(), R.string.pin_color, true);

    public GetColorAction() {
        super(ActionType.GET_COLOR);
        addPins(sourcePin, posPin, colorPin);
    }

    public GetColorAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, posPin, colorPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        Bitmap bitmap;
        if (sourcePin.isLinked()) {
            PinImage source = getPinValue(runnable, sourcePin);
            bitmap = source.getImage();
        } else {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            bitmap = service.tryGetScreenShot();
        }
        PinPoint pos = getPinValue(runnable, posPin);
        Point point = pos.getValue();
        if (bitmap != null) {
            int pixel = bitmap.getPixel(point.x, point.y);
            colorPin.getValue(PinColor.class).setValue(new PinColor.ColorInfo(pixel, 0, Integer.MAX_VALUE));
        }
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!sourcePin.isLinked()) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                List<Action> actions = task.getActions(SwitchCaptureAction.class);
                if (actions.isEmpty()) {
                    result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_need_capture_warning);
                }
            }
        }
    }
}
