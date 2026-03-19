package top.bogey.touch_tool.bean.action.task;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.DynamicPinsAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinTaskString;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;

public class ExecuteTaskAction extends Action implements DynamicPinsAction, SyncAction {
    private final transient Pin taskPin = new NotLinkAblePin(new PinTaskString(), R.string.execute_task_action_task_id, false, false, true);

    private transient boolean synced = false;

    private transient boolean checking = false;

    public ExecuteTaskAction() {
        super(ActionType.EXECUTE_TASK);
        addPin(taskPin);
    }

    public ExecuteTaskAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPin(taskPin);
        tmpPins.forEach(pin -> {
            if (pin.getValue() instanceof PinExecute) {
                ExecuteShowAblePin showAblePin = new ExecuteShowAblePin(new PinExecute());
                showAblePin.sync(pin);
                addPin(showAblePin);
            } else {
                addPin(pin);
            }
        });
        tmpPins.clear();
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        if (checking) return;
        checking = true;
        super.check(result, task);
        Task executeTask = getTask(task);
        if (executeTask == null) {
            checking = false;
            return;
        }
        executeTask.check(result);
        checking = false;
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Task task = getTask(runnable.getTask());
        if (task == null) return;

        if (!synced) {
            synced = true;
            sync(runnable.getTask(), task);
        }

        Map<String, PinObject> params = new HashMap<>();
        for (Pin p : getDynamicPins()) {
            if (!p.isOut() && !p.isVertical()) {
                PinObject value = getPinValue(runnable, p);
                params.put(p.getUid(), value);
            }
        }

        task.execute(runnable, this, pin, params);
    }

    @Override
    public void executeNext(TaskRunnable runnable, Pin pin) {
        if (pin == null) return;
        if (isRealTimeMode(runnable.getTask())) return;
        Pin pinByUid = getPinByUid(pin.getUid());
        super.executeNext(runnable, pinByUid);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        if (isRealTimeMode(runnable.getTask())) execute(runnable, pin);
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
        if (isRealTimeMode(runnable.getTask()) || (!pin.isOut() && pin.isSameClass(PinExecute.class))) {
            for (Pin p : getDynamicPins()) {
                if (p.isOut()) {
                    p.getValue().reset();
                }
            }
        }
    }

    public void setParams(Map<String, PinObject> params) {
        if (params == null) return;
        params.forEach((key, value) -> {
            Pin pin = getPinByUid(key);
            if (pin == null) return;
            pin.setValue(value);
        });
    }

    public boolean isRealTimeMode(Task context) {
        Task task = getTask(context);
        if (task == null) return false;
        List<Action> actions = task.getActions(CustomEndAction.class);
        if (actions.isEmpty()) return false;
        for (Action action : actions) {
            CustomEndAction endAction = (CustomEndAction) action;
            return endAction.isRealTimeMode();
        }
        return false;
    }

    public Task getTask(Task context) {
        PinTaskString taskString = taskPin.getValue();
        return TaskSaver.getInstance().upFindTask(context, taskString.getValue());
    }

    public void setTask(Task task) {
        PinTaskString taskString = taskPin.getValue();
        taskString.setValue(task.getId());
    }

    public String getTaskId() {
        return taskPin.getValue(PinTaskString.class).getValue();
    }

    public Pin getFirstOutExecutePin() {
        for (Pin pin : getPins()) {
            if (pin.isOut() && pin.isSameClass(PinExecute.class)) {
                return pin;
            }
        }
        return null;
    }

    @Override
    public List<Pin> getDynamicPins() {
        List<Pin> pins = new ArrayList<>();
        boolean start = false;
        for (Pin pin : getPins()) {
            if (start) pins.add(pin);
            if (pin == taskPin) start = true;
        }
        return pins;
    }

    @Override
    public void sync(Task context) {
        Task task = getTask(context);
        if (task == null) return;
        sync(context, task);
    }

    public void sync(Task context, Task task) {
        String globalFlag = task.getParent() == null ? SelectActionDialog.GLOBAL_FLAG : "";
        setTitle(globalFlag + task.getTitle());

        List<Pin> syncPins = new ArrayList<>();
        for (Action action : task.getActions(CustomStartAction.class)) {
            CustomStartAction startAction = (CustomStartAction) action;
            syncPins.addAll(startAction.getDynamicPins());
            break;
        }
        for (Action action : task.getActions(CustomEndAction.class)) {
            CustomEndAction endAction = (CustomEndAction) action;
            syncPins.addAll(endAction.getDynamicPins());
            break;
        }

        List<Pin> dynamicPins = getDynamicPins();
        for (Pin dynamicPin : dynamicPins) {
            boolean flag = true;
            for (Pin syncPin : syncPins) {
                if (dynamicPin.getUid().equals(syncPin.getUid())) {
                    flag = false;
                    break;
                }
            }
            if (flag) {
                removePin(context, dynamicPin);
            }
        }

        List<Pin> orderPins = new ArrayList<>();
        for (Pin syncPin : syncPins) {
            Pin dynamicPin = getPinByUid(syncPin.getUid());
            if (dynamicPin == null) {
                dynamicPin = syncPin.newCopy();
                if (dynamicPin.getValue() instanceof PinExecute) {
                    ExecuteShowAblePin showAblePin = new ExecuteShowAblePin(new PinExecute());
                    showAblePin.sync(dynamicPin);
                    dynamicPin = showAblePin;
                }

                // 同步过来的针脚不再是动态的了
                dynamicPin.setDynamic(false);
                // 方向需要反转一下
                dynamicPin.setOut(!dynamicPin.isOut());
                addPin(dynamicPin);
            } else {
                // 同步针脚默认值，只有当类型变更时才同步默认值
                if (!syncPin.isSameClass(dynamicPin)) {
                    dynamicPin.clearLinks(context);
                    dynamicPin.setValue(syncPin.getValue().copy());
                } else {
                    // 类型一致但是内部类型不一致的针脚
                    if (syncPin.getValue() instanceof PinMap pinMap) {
                        PinMap dPinMap = dynamicPin.getValue();
                        if (pinMap.getKeyType().getType() != dPinMap.getKeyType().getType() || pinMap.getValueType().getType() != dPinMap.getValueType().getType()) {
                            dynamicPin.clearLinks(context);
                            dynamicPin.setValue(syncPin.getValue().copy());
                        }
                    } else if (syncPin.getValue() instanceof PinList pinList) {
                        PinList dPinList = dynamicPin.getValue();
                        if (pinList.getValueType().getType() != dPinList.getValueType().getType()) {
                            dynamicPin.clearLinks(context);
                            dynamicPin.setValue(syncPin.getValue().copy());
                        }
                    } else if (syncPin.getValue() instanceof PinSingleSelect pinSingleSelect) {
                        PinSingleSelect dPinSingleSelect = dynamicPin.getValue();
                        dPinSingleSelect.setOptions(pinSingleSelect.getOptions());
                    }
                }
                dynamicPin.setHide(syncPin.isHide());
                dynamicPin.setTitle(syncPin.getTitle());
            }

            orderPins.add(dynamicPin);
        }

        // 对同步过来的针脚排序
        List<Pin> origin = getPins();
        boolean start = true;
        for (int i = origin.size() - 1; i >= 0; i--) {
            Pin pin = origin.get(i);
            if (pin == taskPin) start = false;
            if (start) origin.remove(i);
        }
        origin.addAll(orderPins);
    }

    @Override
    public String getTitle() {
        if (title == null) return super.getTitle();
        return title;
    }

    private static class ExecuteShowAblePin extends ShowAblePin {
        public ExecuteShowAblePin(PinBase value) {
            super(value);
        }

        @Override
        public boolean showAble(Task context) {
            ExecuteTaskAction action = (ExecuteTaskAction) context.getAction(getOwnerId());
            if (action == null) return false;
            return !action.isRealTimeMode(context);
        }
    }
}
