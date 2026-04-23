package top.bogey.touch_tool.ui.blueprint.selecter.select_action;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionInfo;
import top.bogey.touch_tool.bean.action.ActionMap;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.task.CustomEndAction;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.action.variable.GetOrSetVariableAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.utils.callback.ResultCallback;

public class SelectActionByPinDialog extends SelectActionDialog {
    private final Pin touchedPin;

    public SelectActionByPinDialog(@NonNull Context context, Task task, Pin touchedPin, ResultCallback<Action> callback) {
        super(context, task, result -> {
            if (result instanceof GetOrSetVariableAction getOrSet) {
                getOrSet.setRealtimeMode(!touchedPin.isOut());
            }
            callback.onResult(result);
        });
        this.touchedPin = touchedPin;
        dataMap = getGroupData(groupType);
        refreshSubGroup(dataMap);
    }

    @Override
    protected Map<String, List<Object>> getGroupData(GroupType groupType) {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        if (touchedPin == null) return map;
        switch (groupType) {
            case PRESET -> {
                for (ActionMap.ActionGroupType actionGroupType : ActionMap.ActionGroupType.values()) {
                    List<Object> types = new ArrayList<>();
                    for (ActionType actionType : new ArrayList<>(ActionMap.getTypes(actionGroupType))) {
                        ActionInfo actionInfo = ActionInfo.getActionInfo(actionType);
                        if (actionInfo == null) continue;
                        Action action = actionInfo.getAction();
                        if (action == null) continue;
                        Pin pin = action.findConnectToAblePin(touchedPin);
                        if (pin == null) continue;
                        types.add(actionType);
                    }
                    if (types.isEmpty()) continue;
                    map.put(actionGroupType.getName(), types);
                }
            }
            case TASK -> {
                // 私有任务
                List<Object> privateTasks = new ArrayList<>();
                for (Task task : task.getTasks()) {
                    if (isConnectAbleTask(task)) privateTasks.add(task);
                }
                map.put(PRIVATE, privateTasks);

                // 公共任务
                List<Object> publicTasks = new ArrayList<>();
                for (Task task : TaskSaver.getInstance().getOrderTasks()) {
                    if (isConnectAbleTask(task)) publicTasks.add(task);
                }
                map.put(GLOBAL, publicTasks);

                // 父任务
                Task parent = task.getParent();
                while (parent != null) {
                    List<Object> list = new ArrayList<>();
                    if (isConnectAbleTask(parent)) list.add(parent);
                    for (Task task : parent.getTasks()) {
                        if (isConnectAbleTask(task)) list.add(task);
                    }
                    if (!list.isEmpty()) map.put(PARENT_PREFIX + parent.getTitle(), list);
                    parent = parent.getParent();
                }
            }
            case VARIABLE -> {
                // 私有变量
                List<Object> privateVars = new ArrayList<>();
                for (Variable var : task.getVariables()) {
                    if (touchedPin.getValue().linkFromAble(var.getValue())) privateVars.add(var);
                    else if (touchedPin.isSameClass(PinExecute.class)) privateVars.add(var);
                }
                map.put(PRIVATE, privateVars);

                // 全局变量
                List<Object> publicVars = new ArrayList<>();
                for (Variable var : VariableSaver.getInstance().getVars()) {
                    if (touchedPin.getValue().linkFromAble(var.getValue())) publicVars.add(var);
                    else if (touchedPin.isSameClass(PinExecute.class)) publicVars.add(var);
                }
                map.put(GLOBAL, publicVars);

                // 父级变量
                Task parent = task.getParent();
                while (parent != null) {
                    List<Object> list = new ArrayList<>();
                    for (Variable var : parent.getVariables()) {
                        if (touchedPin.getValue().linkFromAble(var.getValue())) list.add(var);
                        else if (touchedPin.isSameClass(PinExecute.class)) list.add(var);
                    }
                    if (!list.isEmpty()) map.put(PARENT_PREFIX + parent.getTitle(), list);
                    parent = parent.getParent();
                }
            }
        }
        return map;
    }

    private boolean isConnectAbleTask(Task task) {
        for (Action action : task.getActions(CustomStartAction.class)) {
            if (action.findConnectToAblePin(touchedPin) != null) return true;
        }
        for (Action action : task.getActions(CustomEndAction.class)) {
            if (action.findConnectToAblePin(touchedPin) != null) return true;
        }
        return false;
    }
}
