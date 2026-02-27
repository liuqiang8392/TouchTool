package top.bogey.touch_tool.bean.action.variable;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteOrCalculateAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;
import top.bogey.touch_tool.utils.GsonUtil;

public class GetOrSetVariableAction extends ExecuteOrCalculateAction implements SyncAction {
    private final String varId;

    private final transient Pin inVarPin;
    private final transient Pin outVarPin;
    private final transient Pin savePin = new NotLinkAbleExecuteShowablePin(new PinBoolean(false), R.string.set_value_action_save);

    public GetOrSetVariableAction(Variable variable) {
        super(ActionType.GET_OR_SET_VARIABLE);
        realtimeModePin.setTitleId(R.string.get_or_set_value_action_get_value);
        realtimeModePin.setHide(false);

        varId = variable.getId();
        inVarPin = new ExecuteShowablePin(variable.getValue());
        inVarPin.setUid(varId + "_in");
        outVarPin = new NotExecuteShowablePin(variable.getValue(), true);
        outVarPin.setUid(varId + "_out");
        addPins(inVarPin, outVarPin, savePin);
    }

    public GetOrSetVariableAction(JsonObject jsonObject) {
        super(jsonObject);
        realtimeModePin.setTitleId(R.string.get_or_set_value_action_get_value);
        realtimeModePin.setHide(false);

        varId = GsonUtil.getAsString(jsonObject, "varId", "");
        reAddPin(new ExecuteShowablePin(new PinObject()), true);
        inVarPin = getPinByUid(varId + "_in");
        reAddPin(new NotExecuteShowablePin(new PinObject(), true), true);
        outVarPin = getPinByUid(varId + "_out");
        reAddPin(savePin);
    }

    @Override
    public String getTitle() {
        if (title == null) return super.getTitle();
        return title;
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        Task task = runnable.getTask();
        Variable var = task.upFindVariable(varId);
        if (var == null) var = VariableSaver.getInstance().getVar(varId);
        if (var != null && inVarPin != null) {
            PinObject value = getPinValue(runnable, inVarPin);
            var.setSaveValue(value);

            // 保存变量，需要找到原始任务来保存
            if (savePin.getValue(PinBoolean.class).getValue()) {
                Task startTask = runnable.getStartTask();
                Task saveTask = TaskSaver.getInstance().getTask(startTask.getId());
                if (saveTask == null) saveTask = task;
                Variable variable = saveTask.downFindVariable(varId);
                if (variable == null) variable = VariableSaver.getInstance().getVar(varId);
                if (variable != null) {
                    variable.setSaveValue(value);
                    variable.save();
                }
            }
        }
        executeNext(runnable, outPin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        Task task = runnable.getTask();
        Variable var = task.upFindVariable(varId);
        if (var == null) var = VariableSaver.getInstance().getVar(varId);
        if (var == null || outVarPin == null) return;
        outVarPin.setValue(returnValue(var.getSaveValue()));
    }

    @Override
    protected void doAction(TaskRunnable runnable, Pin pin) {
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {
    }

    public String getVarId() {
        return varId;
    }

    @Override
    public void onValueUpdated(Task task, Pin origin, PinBase value) {
        super.onValueUpdated(task, origin, value);
        if (origin.equals(realtimeModePin)) {
            inVarPin.clearLinks(task);
            outVarPin.clearLinks(task);
        }
    }

    @Override
    public void sync(Task context) {
        Variable variable = context.upFindVariable(varId);
        if (variable == null) variable = VariableSaver.getInstance().getVar(varId);
        if (variable == null) return;

        syncVarPin(context, inVarPin, variable);
        syncVarPin(context, outVarPin, variable);

        String globalFlag = variable.getParent() == null ? SelectActionDialog.GLOBAL_FLAG : "";
        PinInfo pinInfo = PinInfo.getPinInfo(variable.getValue());
        setTitle(MainApplication.getInstance().getString(R.string.get_or_set_value_action, pinInfo.getTitle()) + " - " + globalFlag + variable.getTitle());
    }

    public static void syncVarPin(Task context, Pin pin, Variable variable) {
        if (pin == null) return;

        PinBase inValue = pin.getValue();
        pin.setTitle(variable.getTitle());
        if (variable.getValue() instanceof PinList pinList) {
            if (!(inValue instanceof PinList inValueList)
                    || pinList.getValueType().getType() != inValueList.getValueType().getType()) {
                pin.setValue(context, pinList.copy());
            }
        } else if (variable.getValue() instanceof PinMap pinMap) {
            if (!(inValue instanceof PinMap inValueMap)
                    || pinMap.getValueType().getType() != inValueMap.getValueType().getType()
                    || pinMap.getKeyType().getType() != inValueMap.getKeyType().getType()) {
                pin.setValue(context, pinMap.copy());
            }
        } else if (!pin.isSameClass(variable.getValue())) {
            pin.setValue(context, variable.getValue().copy());
        }
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        Variable variable = task.upFindVariable(varId);
        if (variable == null) variable = VariableSaver.getInstance().getVar(varId);
        if (variable == null) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_not_exist_variable_error);
        }
    }

    private static class NotLinkAbleExecuteShowablePin extends ExecuteShowablePin {
        public NotLinkAbleExecuteShowablePin(PinBase value, int titleId) {
            super(value, titleId);
        }

        @Override
        public boolean linkAble() {
            return false;
        }
    }

    private static class NotExecuteShowablePin extends ExecuteShowablePin {

        public NotExecuteShowablePin(PinBase value, boolean out) {
            super(value, out);
        }

        @Override
        public boolean showAble(Task context) {
            return !super.showAble(context);
        }
    }
}
