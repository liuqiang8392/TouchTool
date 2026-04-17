package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
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
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.MatchResult;

public class FindImagesAction extends FindExecuteAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin templatePin = new Pin(new PinImage(), R.string.find_images_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.find_images_action_similarity);
    private final transient Pin scalePin = new Pin(new PinSingleSelect(R.array.match_image_scale, 1), R.string.image_action_scale, false, false, true);
    private final transient Pin cannyPin = new Pin(new PinBoolean(false), R.string.image_action_canny);
    private final transient Pin areasPin = new Pin(new PinList(new PinArea()), true);
    private final transient Pin similaritiesPin = new Pin(new PinList(new PinInteger()), true);

    public FindImagesAction() {
        super(ActionType.FIND_IMAGES);
        intervalPin.getValue(PinInteger.class).setValue(200);
        addPins(sourcePin, templatePin, similarityPin, scalePin, cannyPin, areasPin, similaritiesPin);
    }

    public FindImagesAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, templatePin, similarityPin, scalePin, cannyPin, areasPin, similaritiesPin);
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
        PinBoolean canny = getPinValue(runnable, cannyPin);

        List<MatchResult> rectList = DisplayUtil.matchAllTemplate(bitmap, template.getImage(), null, similarity.intValue(), scale.getIndex(), canny.getValue());
        if (rectList != null && !rectList.isEmpty()) {
            rectList.forEach(result -> {
                areasPin.getValue(PinList.class).add(new PinArea(result.area));
                similaritiesPin.getValue(PinList.class).add(new PinInteger((int) (result.value * 100)));
            });
        }
        return !areasPin.getValue(PinList.class).isEmpty();
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
