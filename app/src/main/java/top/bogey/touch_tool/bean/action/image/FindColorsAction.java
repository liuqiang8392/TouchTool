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
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinImage;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.DisplayUtil;

public class FindColorsAction extends FindExecuteAction {
    private final transient Pin sourcePin = new Pin(new PinImage(), R.string.pin_image_full, false, false, true);
    private final transient Pin templatePin = new Pin(new PinColor(), R.string.find_colors_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.find_colors_action_similarity);
    private final transient Pin areasPin = new Pin(new PinList(new PinArea()), true);
    private final transient Pin firstAreaPin = new Pin(new PinArea(), R.string.pin_area_first, true);

    public FindColorsAction() {
        super(ActionType.FIND_COLORS);
        intervalPin.getValue(PinInteger.class).setValue(200);
        addPins(sourcePin, templatePin, similarityPin, areasPin, firstAreaPin);
    }

    public FindColorsAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(sourcePin, templatePin, similarityPin, areasPin, firstAreaPin);
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
        PinColor template = getPinValue(runnable, templatePin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);

        PinList list = areasPin.getValue(PinList.class);
        List<Rect> rectList = DisplayUtil.matchColor(bitmap, template.getValue().getColor(), null, similarity.intValue());
        if (rectList != null && !rectList.isEmpty()) {
            int minArea = template.getValue().getMinArea();
            int maxArea = template.getValue().getMaxArea();
            for (Rect rect : rectList) {
                int size = rect.width() * rect.height();
                if (size >= minArea && size <= maxArea) {
                    list.add(new PinArea(rect));
                }
            }
            if (!list.isEmpty()) {
                PinArea area = (PinArea) list.get(0);
                firstAreaPin.getValue(PinArea.class).setValue(area.getValue());
            }
        }

        return !list.isEmpty();
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
