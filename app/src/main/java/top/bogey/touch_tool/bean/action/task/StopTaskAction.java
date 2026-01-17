package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinTaskString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class StopTaskAction extends ExecuteAction {
    private final transient Pin allTaskPin = new NotLinkAblePin(new PinBoolean(false), R.string.stop_task_action_task_all);
    private final transient Pin taskPin = new TaskShowablePin(new PinTaskString(PinSubType.ALL_TASK_ID), R.string.stop_task_action_task_id);

    public StopTaskAction() {
        super(ActionType.STOP_TASK);
        addPins(allTaskPin, taskPin);
    }

    public StopTaskAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(allTaskPin, taskPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (isAllTask()) {
            service.stopAllTask(runnable.getStartTask());
        } else {
            Task task = getTask();
            if (task != null) {
                service.stopTask(task);
            } else {
                String taskId = taskPin.getValue(PinTaskString.class).getValue();
                if (taskId == null || taskId.isEmpty()) {
                    service.stopTask(runnable.getStartTask());
                    return;
                }
            }
        }
        executeNext(runnable, outPin);
    }

    public Task getTask() {
        PinTaskString taskString = taskPin.getValue();
        return TaskSaver.getInstance().getTask(taskString.getValue());
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        String taskId = taskPin.getValue(PinTaskString.class).getValue();
        if (taskId == null || taskId.isEmpty()) {
            return;
        }
        Task selectTask = getTask();
        if (selectTask == null) result.addResult(ActionCheckResult.ResultType.WARNING, R.string.check_not_global_task_warning);
    }

    private boolean isAllTask() {
        PinBoolean pin = allTaskPin.getValue();
        return pin.getValue();
    }

    private static class TaskShowablePin extends ShowAblePin {
        public TaskShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean showAble(Task context) {
            StopTaskAction action = (StopTaskAction) context.getAction(getOwnerId());
            return !action.isAllTask();
        }

        @Override
        public boolean linkAble(Task context) {
            return false;
        }
    }
}
