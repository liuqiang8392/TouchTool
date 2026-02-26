package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.system.SwitchCaptureAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.special_pin.NotShowPin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.TouchPathFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;

public class TouchImageAction extends ExecuteAction {
    private final transient Pin templatePin = new Pin(new PinImage(), R.string.touch_image_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.touch_image_action_similarity);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.touch_image_action_area, false, false, true);
    private final transient Pin scalePin = new Pin(new PinSingleSelect(R.array.match_image_scale, 1), R.string.touch_image_action_scale, false, false, true);
    private final transient Pin useAccPin = new NotShowPin(new PinBoolean(true), R.string.touch_image_action_use_accessibility, false, false, true);
    private final transient Pin randomPin = new Pin(new PinBoolean(), R.string.touch_image_action_offset);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.if_action_else, true);

    public TouchImageAction() {
        super(ActionType.TOUCH_IMAGE);
        addPins(templatePin, similarityPin, areaPin, scalePin, useAccPin, randomPin, elsePin);
    }

    public TouchImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(templatePin, similarityPin, areaPin, scalePin, useAccPin, randomPin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        Bitmap bitmap = service.tryGetScreenShot();

        PinImage template = getPinValue(runnable, templatePin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);
        PinArea area = getPinValue(runnable, areaPin);
        PinSingleSelect scale = getPinValue(runnable, scalePin);
        PinBoolean random = getPinValue(runnable, randomPin);

        Rect rect = DisplayUtil.matchTemplate(bitmap, template.getImage(), area.getValue(), similarity.intValue(), scale.getIndex() + 1);
        if (rect == null || rect.isEmpty()) {
            executeNext(runnable, elsePin);
            return;
        }
        if (random.getValue()) {
            int x = rect.left + (int) (Math.random() * rect.width());
            int y = rect.top + (int) (Math.random() * rect.height());
            service.runGesture(x, y, 50, null);
            TouchPathFloatView.showGesture(x, y);
        } else {
            service.runGesture(rect.centerX(), rect.centerY(), 50, null);
            TouchPathFloatView.showGesture(rect.centerX(), rect.centerY());
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            List<Action> actions = task.getActions(SwitchCaptureAction.class);
            if (actions.isEmpty()) {
                result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_need_capture_warning);
            }
        }
    }
}
