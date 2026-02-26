package top.bogey.touch_tool.bean.action.image;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Build;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.system.SwitchCaptureAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.NotShowPin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.TouchPathFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;

public class TouchColorAction extends ExecuteAction {
    private final transient Pin templatePin = new Pin(new PinColor(), R.string.touch_color_action_template);
    private final transient Pin similarityPin = new Pin(new PinInteger(80), R.string.touch_color_action_similarity);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.touch_color_action_area, false, false, true);
    private final transient Pin useAccPin = new NotShowPin(new PinBoolean(true), R.string.touch_color_action_use_accessibility, false, false, true);
    private final transient Pin offsetPin = new Pin(new PinBoolean(), R.string.touch_color_action_offset);
    private final transient Pin touchAllPin = new NotLinkAblePin(new PinBoolean(false), R.string.touch_color_action_touch_all);
    private final transient Pin touchIntervalPin = new TouchIntervalShowablePin(new PinInteger(300), R.string.touch_color_action_touch_interval);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.if_action_else, true);

    public TouchColorAction() {
        super(ActionType.TOUCH_COLOR);
        addPins(templatePin, similarityPin, areaPin, useAccPin, offsetPin, touchAllPin, touchIntervalPin, elsePin);
    }

    public TouchColorAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(templatePin, similarityPin, areaPin, useAccPin, offsetPin, touchAllPin, touchIntervalPin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        Bitmap bitmap = service.tryGetScreenShot();

        PinColor template = getPinValue(runnable, templatePin);
        PinNumber<?> similarity = getPinValue(runnable, similarityPin);
        PinArea area = getPinValue(runnable, areaPin);

        List<Rect> rectList = DisplayUtil.matchColor(bitmap, template.getValue().getColor(), area.getValue(), similarity.intValue());
        if (rectList != null && !rectList.isEmpty()) {
            List<Rect> validList = new ArrayList<>();
            int minArea = template.getValue().getMinArea();
            int maxArea = template.getValue().getMaxArea();
            for (Rect rect : rectList) {
                int size = rect.width() * rect.height();
                if (size >= minArea && size <= maxArea) {
                    validList.add(rect);
                }
            }
            if (validList.isEmpty()) {
                executeNext(runnable, elsePin);
                return;
            }

            if (isTouchAll()) {
                PinNumber<?> interval = getPinValue(runnable, touchIntervalPin);
                int index = 0;
                while (index < validList.size()) {
                    Rect rect = validList.get(index);
                    touch(runnable, rect);
                    runnable.await(interval.intValue());
                    index++;
                }
            } else {
                touch(runnable, validList.get(0));
            }
            executeNext(runnable, outPin);
            return;
        }
        executeNext(runnable, elsePin);
    }

    private void touch(TaskRunnable runnable, Rect rect) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        PinBoolean random = getPinValue(runnable, offsetPin);
        if (random.getValue()) {
            int x = rect.left + (int) (Math.random() * rect.width());
            int y = rect.top + (int) (Math.random() * rect.height());
            service.runGesture(x, y, 50, null);
            TouchPathFloatView.showGesture(x, y);
        } else {
            service.runGesture(rect.centerX(), rect.centerY(), 50, null);
            TouchPathFloatView.showGesture(rect.centerX(), rect.centerY());
        }
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

    private boolean isTouchAll() {
        return touchAllPin.getValue(PinBoolean.class).getValue();
    }


    private static class TouchIntervalShowablePin extends ShowAblePin {
        public TouchIntervalShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            TouchColorAction action = (TouchColorAction) context.getAction(getOwnerId());
            return action.isTouchAll();
        }
    }
}
