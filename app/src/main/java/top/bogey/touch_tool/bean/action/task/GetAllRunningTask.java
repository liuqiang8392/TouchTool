package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteOrCalculateAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetAllRunningTask extends ExecuteOrCalculateAction {
    private final transient Pin tasksPin = new Pin(new PinList(new PinString()), R.string.get_all_running_task_action_result, true);

    public GetAllRunningTask() {
        super(ActionType.GET_ALL_RUNNING_TASK);
        addPin(tasksPin);
    }

    public GetAllRunningTask(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(tasksPin);
    }

    @Override
    protected void doAction(TaskRunnable runnable, Pin pin) {
        PinList list = tasksPin.getValue(PinList.class);

        MainAccessibilityService service = MainApplication.getInstance().getService();
        List<TaskRunnable> runningTask = service.getRunningTask();
        for (TaskRunnable taskRunnable : runningTask) {
            list.add(new PinString(taskRunnable.getTask().getTitle()));
        }
    }
}
