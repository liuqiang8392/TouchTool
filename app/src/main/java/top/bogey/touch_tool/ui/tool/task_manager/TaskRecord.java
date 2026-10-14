package top.bogey.touch_tool.ui.tool.task_manager;

import android.content.Context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.task.ExecuteTaskAction;
import top.bogey.touch_tool.bean.action.variable.GetOrSetVariableAction;
import top.bogey.touch_tool.bean.action.variable.GetVariableAction;
import top.bogey.touch_tool.bean.action.variable.SetVariableAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinTaskString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.utils.AppUtil;

public record TaskRecord(Set<Task> tasks, Set<Variable> variables) {

    public Set<Task> getTaskReferences(Task task) {
        Set<Task> references = new HashSet<>();
        for (Action action : task.getActions(ExecuteTaskAction.class)) {
            ExecuteTaskAction execute = (ExecuteTaskAction) action;
            String taskId = execute.getTaskId();
            Task taskById = getTaskById(taskId);
            if (taskById != null) references.add(taskById);
        }

        for (Task childTask : task.getTasks()) {
            references.addAll(getTaskReferences(childTask));
        }
        return references;
    }

    public Set<Variable> getVariableReferences(Task task) {
        Set<Variable> variables = new HashSet<>();
        for (Action action : task.getActions(GetVariableAction.class)) {
            GetVariableAction get = (GetVariableAction) action;
            String varId = get.getVarId();
            Variable variable = getVariableById(varId);
            if (variable != null) variables.add(variable);
        }
        for (Action action : task.getActions(SetVariableAction.class)) {
            SetVariableAction set = (SetVariableAction) action;
            String varId = set.getVarId();
            Variable variable = getVariableById(varId);
            if (variable != null) variables.add(variable);
        }
        for (Action action : task.getActions(GetOrSetVariableAction.class)) {
            GetOrSetVariableAction getOrSet = (GetOrSetVariableAction) action;
            String varId = getOrSet.getVarId();
            Variable variable = getVariableById(varId);
            if (variable != null) variables.add(variable);
        }

        for (Task childTask : task.getTasks()) {
            // 这里必须递归 TaskRecord 自己的版本：Task.getVariableReferences() 走的是 VariableSaver 全局表，
            // 而导入时文件里的变量还没入库，子任务引用的变量会全部查不到而丢失
            variables.addAll(getVariableReferences(childTask));
        }
        return variables;
    }

    // 生成一份全新 ID 的任务副本，并把副本内的任务引用针脚改写为副本自己的 ID
    // 变量不换 ID：变量 ID 即全局变量的身份，换 ID 会凭空多出一个重复的全局变量
    public TaskRecord duplicate() {
        Map<String, String> idMap = new HashMap<>();
        Set<Task> copies = new HashSet<>();
        for (Task task : tasks) {
            Task copy = task.newCopy();
            idMap.put(task.getId(), copy.getId());
            copies.add(copy);
        }

        if (!idMap.isEmpty()) {
            for (Task copy : copies) {
                remapTaskReferences(copy, idMap);
            }
        }
        return new TaskRecord(copies, variables);
    }

    // 递归改写任务内所有任务 ID 针脚，执行任务、停止任务、判断任务是否运行等都靠它指向副本
    private static void remapTaskReferences(Task task, Map<String, String> idMap) {
        for (Action action : task.getActions()) {
            for (Pin pin : action.getPins()) {
                if (!(pin.getValue() instanceof PinTaskString taskString)) continue;
                String newId = idMap.get(taskString.getValue());
                if (newId == null) continue;
                taskString.setValue(newId);
            }
        }

        for (Task childTask : task.getTasks()) {
            remapTaskReferences(childTask, idMap);
        }
    }

    public Task getTaskById(String id) {
        for (Task task : tasks) {
            if (task.getId().equals(id)) return task;
        }
        return null;
    }

    public Variable getVariableById(String id) {
        for (Variable variable : variables) {
            if (variable.getId().equals(id)) return variable;
        }
        return null;
    }

    public String getDefaultName(Context context) {
        if (tasks.size() == 1) {
            return tasks.iterator().next().getTitle();
        } else {
            return "TT_" + AppUtil.formatDateTime(context, System.currentTimeMillis(), false, true);
        }
    }
}
