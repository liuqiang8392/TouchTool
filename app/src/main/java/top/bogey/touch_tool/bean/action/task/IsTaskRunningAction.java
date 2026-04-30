package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinTaskString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class IsTaskRunningAction extends CalculateAction {
    private final transient Pin taskPin = new NotLinkAblePin(new PinTaskString(PinSubType.ALL_TASK_ID), R.string.is_task_running_action_task_id);
    private final transient Pin resultPin = new Pin(new PinBoolean(false), R.string.pin_boolean_result, true);


    public IsTaskRunningAction() {
        super(ActionType.IS_TASK_RUNNING);
        addPins(taskPin, resultPin);
    }

    public IsTaskRunningAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(taskPin, resultPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        boolean running = service.isTaskRunning(getTask());
        resultPin.getValue(PinBoolean.class).setValue(running);
    }

    public Task getTask() {
        PinTaskString taskString = taskPin.getValue();
        return TaskSaver.getInstance().getTask(taskString.getValue());
    }
}
