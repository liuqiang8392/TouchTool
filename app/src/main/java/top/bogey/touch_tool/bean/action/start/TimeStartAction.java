package top.bogey.touch_tool.bean.action.start;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinDate;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinPeriodic;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_number.PinTime;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.AppUtil;

public class TimeStartAction extends StartAction {
    private final transient Pin datePin = new NotLinkAblePin(new PinDate(), R.string.time_start_action_date);
    private final transient Pin timePin = new NotLinkAblePin(new PinTime(), R.string.time_start_action_time);
    private final transient Pin periodic = new NotLinkAblePin(new PinPeriodic(), R.string.time_start_action_periodic);

    public TimeStartAction() {
        super(ActionType.TIME_START);
        addPins(datePin, timePin, periodic);
    }

    public TimeStartAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(datePin, timePin, periodic);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        super.execute(runnable, pin);
        executeNext(runnable, executePin);
    }

    public long getStartTime() {
        long date = datePin.getValue(PinDate.class).getValue();
        long time = timePin.getValue(PinTime.class).getValue();
        return AppUtil.mergeDateTime(date, time);
    }

    public long getPeriodic() {
        return periodic.getValue(PinPeriodic.class).getValue();
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!SettingSaver.PERMISSION_EXACT_ALARM.get()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_exact_alarm_permission_error);
        }
    }
}
