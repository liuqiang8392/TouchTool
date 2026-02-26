package top.bogey.touch_tool.bean.action.point;

import android.annotation.SuppressLint;

import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinValueArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinInteger;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinNumber;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinPoint;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.service.super_user.SuperUser;
import top.bogey.touch_tool.ui.custom.TouchPathFloatView;

public class TouchPointAction extends ExecuteAction implements SyncAction {
    private final transient Pin touchPin = new Pin(new PinPoint(), R.string.pin_point);
    private final transient Pin timePin = new Pin(new PinValueArea(100, 100), R.string.touch_point_action_time, false, false, true);
    private final transient Pin offsetPin = new Pin(new PinInteger(), R.string.touch_point_action_offset);
    private final transient Pin typePin = new Pin(new PinSingleSelect(), R.string.touch_point_action_type, false, false, true);

    public TouchPointAction() {
        super(ActionType.TOUCH_POINT);
        addPins(touchPin, timePin, offsetPin, typePin);
    }

    public TouchPointAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(touchPin, timePin, offsetPin, typePin);
    }

    @SuppressLint("DefaultLocale")
    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        sync(runnable.getTask());
        PinPoint point = getPinValue(runnable, touchPin);
        PinValueArea time = getPinValue(runnable, timePin);
        int timeValue = time.getRandomValue();
        PinNumber<?> offset = getPinValue(runnable, offsetPin);
        PinSingleSelect type = getPinValue(runnable, typePin);

        int offsetValue = offset.intValue();
        int x = (int) (point.getValue().x - offsetValue + Math.random() * offsetValue * 2);
        int y = (int) (point.getValue().y - offsetValue + Math.random() * offsetValue * 2);

        if (type.getIndex() == 0) {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            service.runGesture(x, y, timeValue, null);
        } else {
            if (SuperUser.getInstance().isValid()) {
                SuperUser.getInstance().runCommand(String.format("input tap %d %d", x, y));
            }
        }
        TouchPathFloatView.showGesture(x, y);
        runnable.sleep(timeValue);

        executeNext(runnable, outPin);
    }

    @Override
    public void sync(Task context) {
        String[] types = MainApplication.getInstance().getResources().getStringArray(R.array.touch_point_type);
        if (SettingSaver.getInstance().getSuperUserType() == 0) {
            typePin.getValue(PinSingleSelect.class).setOptions(Collections.singletonList(types[0]));
        } else {
            typePin.getValue(PinSingleSelect.class).setOptions(List.of(types));
        }
    }
}
