package top.bogey.touch_tool.ui.tool.task_manager;

import android.content.Context;

import java.util.HashSet;
import java.util.Set;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.task.ExecuteTaskAction;
import top.bogey.touch_tool.bean.action.variable.GetOrSetVariableAction;
import top.bogey.touch_tool.bean.action.variable.GetVariableAction;
import top.bogey.touch_tool.bean.action.variable.SetVariableAction;
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
            variables.addAll(childTask.getVariableReferences());
        }
        return variables;
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
