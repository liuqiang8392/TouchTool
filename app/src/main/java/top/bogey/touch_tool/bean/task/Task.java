package top.bogey.touch_tool.bean.task;

import android.graphics.Point;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.start.InnerStartAction;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.action.task.ExecuteTaskAction;
import top.bogey.touch_tool.bean.action.variable.GetOrSetVariableAction;
import top.bogey.touch_tool.bean.action.variable.GetVariableAction;
import top.bogey.touch_tool.bean.action.variable.SetVariableAction;
import top.bogey.touch_tool.bean.base.Identity;
import top.bogey.touch_tool.bean.other.Usage;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.utils.GsonUtil;
import top.bogey.touch_tool.utils.callback.BooleanResultCallback;

public class Task extends Identity implements IActionManager, ITaskManager, IVariableManager, ITagManager {
    public final static int FLAG_DEBUG = 1;

    private final long createTime;

    private final ActionManager actionManager;

    private final TaskManager taskManager;
    private final VariableManager variableManager;

    private final TagManager tagManager;

    private int flag = 0;

    private transient Task parent;

    private transient Map<String, PinObject> nextParams;
    private transient Pin nextPin;

    public Task() {
        super();
        createTime = System.currentTimeMillis();

        actionManager = new ActionManager();

        taskManager = new TaskManager(this);
        variableManager = new VariableManager(this);

        tagManager = new TagManager();
    }

    public Task(JsonObject jsonObject) {
        super(jsonObject);
        createTime = GsonUtil.getAsLong(jsonObject, "createTime", System.currentTimeMillis());

        actionManager = GsonUtil.getAsObject(jsonObject, "actionManager", ActionManager.class, new ActionManager());
        actionManager.filterNullAction();

        taskManager = GsonUtil.getAsObject(jsonObject, "taskManager", TaskManager.class, new TaskManager(this));
        taskManager.setParent(this);
        variableManager = GsonUtil.getAsObject(jsonObject, "variableManager", VariableManager.class, new VariableManager(this));
        variableManager.setParent(this);

        tagManager = GsonUtil.getAsObject(jsonObject, "tagManager", TagManager.class, new TagManager());

        flag = GsonUtil.getAsInt(jsonObject, "flag", 0);
    }

    @Override
    public void addAction(Action action) {
        actionManager.addAction(action);
    }

    @Override
    public void removeAction(String id) {
        actionManager.removeAction(id);
    }

    @Override
    public Action getAction(String id) {
        return actionManager.getAction(id);
    }

    @Override
    public List<Action> getActions() {
        return actionManager.getActions();
    }

    @Override
    public List<Action> getActions(String uid) {
        return actionManager.getActions(uid);
    }

    @Override
    public List<Action> getActions(Class<? extends Action> actionClass) {
        return actionManager.getActions(actionClass);
    }


    @Override
    public void addTask(Task task) {
        taskManager.addTask(task);
    }

    @Override
    public void removeTask(String id) {
        taskManager.removeTask(id);
    }

    @Override
    public Task getTask(String id) {
        return taskManager.getTask(id);
    }

    @Override
    public List<Task> getTasks() {
        return taskManager.getTasks();
    }

    @Override
    public List<Task> getTasks(String tag) {
        return taskManager.getTasks(tag);
    }

    @Override
    public Task upFindTask(String id) {
        return taskManager.upFindTask(id);
    }

    @Override
    public Task downFindTask(String id) {
        return taskManager.downFindTask(id);
    }

    @Override
    public Task getTopParent() {
        return taskManager.getTopParent();
    }

    @Override
    public boolean isMyParent(String id) {
        return taskManager.isMyParent(id);
    }

    public List<Usage> getTaskUses(String id) {
        List<Usage> usages = new ArrayList<>();
        List<Point> points = new ArrayList<>();
        for (Action action : getActions(ExecuteTaskAction.class)) {
            ExecuteTaskAction execute = (ExecuteTaskAction) action;
            String taskId = execute.getTaskId();
            if (id.equals(taskId)) {
                points.add(action.getPos());
            }
        }
        if (!points.isEmpty()) usages.add(new Usage(this, points));

        for (Task task : getTasks()) {
            usages.addAll(task.getTaskUses(id));
        }
        return usages;
    }

    public Set<Task> getTaskReferences() {
        Set<Task> tasks = new HashSet<>();
        for (Action action : getActions(ExecuteTaskAction.class)) {
            ExecuteTaskAction execute = (ExecuteTaskAction) action;
            String taskId = execute.getTaskId();
            Task task = TaskSaver.getInstance().getTask(taskId);
            if (task != null) tasks.add(task);
        }

        for (Task task : getTasks()) {
            tasks.addAll(task.getTaskReferences());
        }
        return tasks;
    }

    public Set<Variable> getVariableReferences() {
        Set<Variable> variables = new HashSet<>();
        for (Action action : getActions(GetVariableAction.class)) {
            GetVariableAction get = (GetVariableAction) action;
            String varId = get.getVarId();
            Variable variable = VariableSaver.getInstance().getVar(varId);
            if (variable != null) variables.add(variable);
        }
        for (Action action : getActions(SetVariableAction.class)) {
            SetVariableAction set = (SetVariableAction) action;
            String varId = set.getVarId();
            Variable variable = VariableSaver.getInstance().getVar(varId);
            if (variable != null) variables.add(variable);
        }
        for (Action action : getActions(GetOrSetVariableAction.class)) {
            GetOrSetVariableAction getOrSet = (GetOrSetVariableAction) action;
            String varId = getOrSet.getVarId();
            Variable variable = VariableSaver.getInstance().getVar(varId);
            if (variable != null) variables.add(variable);
        }
        for (Task task : getTasks()) {
            variables.addAll(task.getVariableReferences());
        }
        return variables;
    }

    @Override
    public boolean addVariable(Variable variable) {
        return variableManager.addVariable(variable);
    }

    @Override
    public void removeVariable(String id) {
        variableManager.removeVariable(id);
    }

    @Override
    public Variable getVariable(String id) {
        return variableManager.getVariable(id);
    }

    @Override
    public List<Variable> getVariables() {
        return variableManager.getVariables();
    }

    @Override
    public Variable upFindVariable(String id) {
        return variableManager.upFindVariable(id);
    }

    @Override
    public Variable downFindVariable(String id) {
        return variableManager.downFindVariable(id);
    }

    @Override
    public Variable findVariableByName(String name) {
        return variableManager.findVariableByName(name);
    }

    // 清理变量存档
    public void cleanVariableSave() {
        // 清理变量存档
        for (Variable variable : getVariables()) {
            variable.setSaveValue((PinObject) variable.getValue().copy());
        }

        for (Task task : getTasks()) {
            task.cleanVariableSave();
        }
    }

    // 清理无效标签
    public void cleanInvalidTag() {
        List<String> allTags = TagSaver.getInstance().getTags();
        for (String tag : new ArrayList<>(getTags())) {
            if (allTags.contains(tag)) continue;
            removeTag(tag);
        }

        for (Variable variable : getVariables()) {
            for (String tag : new ArrayList<>(variable.getTags())) {
                if (allTags.contains(tag)) continue;
                variable.removeTag(tag);
            }
        }

        for (Task task : getTasks()) {
            task.cleanInvalidTag();
        }
    }

    public Set<String> getUsedTags() {
        Set<String> tags = new HashSet<>(getTags());

        for (Variable variable : getVariables()) {
            tags.addAll(variable.getTags());
        }

        for (Task task : getTasks()) {
            tags.addAll(task.getUsedTags());
        }

        return tags;
    }

    public List<Usage> getVariableUses(String id) {
        List<Usage> usages = new ArrayList<>();
        List<Point> points = new ArrayList<>();
        for (Action action : getActions(GetVariableAction.class)) {
            GetVariableAction get = (GetVariableAction) action;
            if (id.equals(get.getVarId())) {
                points.add(action.getPos());
            }
        }

        for (Action action : getActions(SetVariableAction.class)) {
            SetVariableAction set = (SetVariableAction) action;
            if (id.equals(set.getVarId())) {
                points.add(action.getPos());
            }
        }

        for (Action action : getActions(GetOrSetVariableAction.class)) {
            GetOrSetVariableAction getOrSet = (GetOrSetVariableAction) action;
            if (id.equals(getOrSet.getVarId())) {
                points.add(action.getPos());
            }
        }
        if (!points.isEmpty()) usages.add(new Usage(this, points));

        for (Task task : getTasks()) {
            usages.addAll(task.getVariableUses(id));
        }

        return usages;
    }

    @Override
    public void addTag(String tag) {
        tagManager.addTag(tag);
    }

    @Override
    public void removeTag(String tag) {
        tagManager.removeTag(tag);
    }

    public void removeInnerTag(String tag) {
        removeTag(tag);
        getTasks().forEach(task -> task.removeInnerTag(tag));
        getVariables().forEach(variable -> variable.removeTag(tag));
    }

    @Override
    public List<String> getTags() {
        return tagManager.getTags();
    }

    @Override
    public void setTags(List<String> tags) {
        tagManager.setTags(tags);
    }

    @Override
    public String getTagString() {
        return tagManager.getTagString();
    }

    public boolean isEnable() {
        for (Action action : getActions(StartAction.class)) {
            if (((StartAction) action).isEnable()) return true;
        }
        return false;
    }

    public void setEnable(boolean enable) {
        for (Action action : getActions(StartAction.class)) {
            ((StartAction) action).setEnable(enable);
        }
    }

    public void check(ActionCheckResult result) {
        getActions().stream().filter(Objects::nonNull).forEach(action -> action.check(result, this));
        getTasks().forEach(task -> task.check(result));
    }

    public void save() {
        if (parent != null) parent.save();
        else TaskSaver.getInstance().saveTask(this);
    }

    @Override
    public Task copy() {
        Task copy = GsonUtil.copy(this, Task.class);
        copy.parent = parent;
        copy.taskManager.setParent(copy);
        copy.variableManager.setParent(copy);
        return copy;
    }

    @Override
    public Task newCopy() {
        Task copy = copy();
        copy.setId(UUID.randomUUID().toString());
        copy.parent = null;
        copy.actionManager.newCopy();
        copy.taskManager.setNewParent(copy);
        copy.variableManager.setNewParent(copy);
        return copy;
    }

    public void execute(TaskRunnable runnable, StartAction startAction, BooleanResultCallback callback) {
        Task task = this;
        Action action = startAction;
        if (!(startAction instanceof InnerStartAction)) {
            task = copy();
            action = task.getAction(startAction.getId());
        }
        runnable.pushStack(task, action);
        callback.onResult(true);
        action.execute(runnable, null);
    }

    public void execute(TaskRunnable runnable, ExecuteTaskAction startAction, Pin pin, Map<String, PinObject> params) {
        Task copy = copy();
        boolean flag = false;
        for (Action action : copy.getActions(CustomStartAction.class)) {
            ((CustomStartAction) action).setParams(params);
            runnable.pushStack(copy, action);
            action.execute(runnable, pin);
            flag = true;
            break;
        }

        if (flag) runnable.popStack();
        startAction.setParams(copy.nextParams);
        if (copy.nextPin == null) copy.nextPin = startAction.getFirstOutExecutePin();
        startAction.executeNext(runnable, copy.nextPin);
    }

    public void setNext(Map<String, PinObject> nextParams, Pin nextPin) {
        this.nextParams = nextParams;
        this.nextPin = nextPin;
    }

    public long getCreateTime() {
        return createTime;
    }

    public Task getParent() {
        return parent;
    }

    void setParent(Task parent) {
        this.parent = parent;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void addFlag(int flag) {
        this.flag |= flag;
    }

    public void toggleFlag(int flag) {
        this.flag ^= flag;
    }

    public boolean hasFlag(int flag) {
        return (this.flag & flag) != 0;
    }

    public static class TaskDeserialize implements JsonDeserializer<Task> {
        @Override
        public Task deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return new Task(json.getAsJsonObject());
        }
    }
}
