package top.bogey.touch_tool.bean.action.variable;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.special_pin.NotLinkAblePin;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;
import top.bogey.touch_tool.utils.GsonUtil;

public class SetVariableAction extends ExecuteAction implements SyncAction {
    private final String varId;
    private final transient Pin varPin;
    private final transient Pin savePin = new NotLinkAblePin(new PinBoolean(false), R.string.set_value_action_save);

    public SetVariableAction(Variable variable) {
        super(ActionType.SET_VARIABLE);
        varId = variable.getId();
        varPin = new Pin(variable.getValue().copy());
        varPin.setUid(varId);
        addPins(varPin, savePin);
    }

    public SetVariableAction(JsonObject jsonObject) {
        super(jsonObject);
        varId = GsonUtil.getAsString(jsonObject, "varId", "");
        reAddPin(new Pin(new PinObject()), true);
        varPin = getPinByUid(varId);
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
        if (var != null && varPin != null) {
            PinObject value = getPinValue(runnable, varPin);
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

    public String getVarId() {
        return varId;
    }

    @Override
    public void sync(Task context) {
        Variable variable = context.upFindVariable(varId);
        if (variable == null) variable = VariableSaver.getInstance().getVar(varId);
        if (variable == null) return;
        if (varPin == null) return;

        GetOrSetVariableAction.syncVarPin(context, varPin, variable);

        String globalFlag = variable.getParent() == null ? SelectActionDialog.GLOBAL_FLAG : "";
        PinInfo pinInfo = PinInfo.getPinInfo(variable.getValue());
        setTitle(MainApplication.getInstance().getString(R.string.set_value_action, pinInfo.getTitle()) + " - " + globalFlag + variable.getTitle());
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
}
