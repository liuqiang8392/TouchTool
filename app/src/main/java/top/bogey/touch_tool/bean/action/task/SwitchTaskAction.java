package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinTaskString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskInfoSummary;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.play.PlayFloatView;
import top.bogey.touch_tool.ui.play.SinglePlayView;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class SwitchTaskAction extends ExecuteAction {
    private final transient Pin taskPin = new NotLinkAblePin(new PinTaskString(PinSubType.ALL_TASK_ID), R.string.switch_task_action_task_id);
    private final transient Pin switchPin = new Pin(new PinBoolean(true), R.string.switch_task_action_switch);

    public SwitchTaskAction() {
        super(ActionType.SWITCH_TASK);
        addPins(taskPin, switchPin);
    }

    public SwitchTaskAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(taskPin, switchPin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        // 必须是存档里的任务，开始动作的真实数据在那里
        Task task = getTask();
        PinBoolean value = getPinValue(runnable, switchPin);
        if (task != null && task.isEnable() != value.getValue()) {
            task.setEnable(value.getValue());
            task.save();

            // 手动执行悬浮窗只显示已启用的开始动作，开关任务后必须刷新，否则关闭的任务还留在悬浮窗上
            refreshManualPlayView();
        }
        executeNext(runnable, outPin);
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

    public Task getTask() {
        PinTaskString taskString = taskPin.getValue();
        return TaskSaver.getInstance().getTask(taskString.getValue());
    }

    // 悬浮窗没显示时不用刷新，免得把已关闭的悬浮窗重新弹出来
    private static void refreshManualPlayView() {
        boolean showing = FloatWindow.getView(PlayFloatView.class.getName()) != null
                || !FloatWindow.getViews(SinglePlayView.class).isEmpty();
        if (!showing) return;

        TaskInfoSummary.getInstance().tryShowManualPlayView(true);
    }
}
