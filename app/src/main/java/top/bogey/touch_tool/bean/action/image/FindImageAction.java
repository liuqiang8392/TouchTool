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
import top.bogey.touch_tool.bean.action.parent.FindExecuteAction;
import top.bogey.touch_tool.bean.action.system.SwitchCaptureAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;

public class FindImageAction extends FindExecuteAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin templatePin = new Pin(new PinImage(), R.string.find_image_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.find_image_action_similarity);
    private final transient Pin scalePin = new Pin(new PinSingleSelect(R.array.match_image_scale, 1), R.string.find_image_action_scale, false, false, true);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);

    public FindImageAction() {
        super(ActionType.FIND_IMAGE);
        intervalPin.getValue(PinInteger.class).setValue(200);
        addPins(sourcePin, templatePin, similarityPin, scalePin, areaPin);
    }

    public FindImageAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, templatePin, similarityPin, scalePin, areaPin);
    }

    @Override
    public boolean find(TaskRunnable runnable) {
        Bitmap bitmap;
        if (sourcePin.isLinked()) {
            PinImage source = getPinValue(runnable, sourcePin);
            bitmap = source.getImage();
        } else {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            bitmap = service.tryGetScreenShot();
        }
        PinImage template = getPinValue(runnable, templatePin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);
        PinSingleSelect scale = getPinValue(runnable, scalePin);

        Rect rect = DisplayUtil.matchTemplate(bitmap, template.getImage(), null, similarity.intValue(), scale.getIndex() + 1);
        if (rect == null || rect.isEmpty()) {
            return false;
        }
        areaPin.getValue(PinArea.class).setValue(rect);
        return true;
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
