package top.bogey.touch_tool.bean.action.point;

import android.os.Build;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinFloat;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.TouchPathFloatView;
import top.bogey.touch_tool.utils.EAnchor;

public class TouchAction extends ExecuteAction {
    private final transient Pin touchPin = new Pin(new PinTouchPath(), R.string.pin_touch);
    private final transient Pin timePin = new Pin(new PinFloat(1), R.string.touch_action_time, false, false, true);
    private final transient Pin offsetPin = new Pin(new PinInteger(), R.string.touch_action_offset);

    public TouchAction() {
        super(ActionType.TOUCH);
        addPins(touchPin, timePin, offsetPin);
    }

    public TouchAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(touchPin, timePin, offsetPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinTouchPath path = getPinValue(runnable, touchPin);
        PinNumber<?> time = getPinValue(runnable, timePin);
        PinNumber<?> offset = getPinValue(runnable, offsetPin);

        AtomicBoolean pause = new AtomicBoolean(true);
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.runGesture(path.getStrokesList(time.floatValue(), offset.intValue()), result -> {
                pause.set(false);
                runnable.resume();
            });
        } else {
            service.runGesture(path.getStrokes(time.floatValue(), offset.intValue()), result -> {
                pause.set(false);
                runnable.resume();
            });
        }
        TouchPathFloatView.showGesture(path.getPathParts(EAnchor.TOP_LEFT), time.floatValue());
        if (pause.get()) runnable.await();
        executeNext(runnable, outPin);
    }
}
