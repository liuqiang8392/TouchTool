package top.bogey.touch_tool.bean.action.variable;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.action.parent.SyncAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;
import top.bogey.touch_tool.utils.GsonUtil;

public class GetVariableAction extends CalculateAction implements SyncAction {
    private final String varId;
    private final transient Pin varPin;

    public GetVariableAction(Variable variable) {
        super(ActionType.GET_VARIABLE);
        varId = variable.getId();
        varPin = new Pin(variable.getValue(), true);
        varPin.setUid(varId);
        addPin(varPin);
    }

    public GetVariableAction(JsonObject jsonObject) {
        super(jsonObject);
        varId = GsonUtil.getAsString(jsonObject, "varId", "");
        reAddPins(null, true);
        varPin = getPinByUid(varId);
    }

    @Override
    public String getTitle() {
        if (title == null) return super.getTitle();
        return title;
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        Task task = runnable.getTask();
        Variable var = task.upFindVariable(varId);
        if (var == null) var = VariableSaver.getInstance().getVar(varId);
        if (var == null || varPin == null) return;
        varPin.setValue(returnValue(var.getSaveValue()));
    }

    @Override
    public void resetReturnValue(TaskRunnable runnable, Pin pin) {

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
        setTitle(MainApplication.getInstance().getString(R.string.get_value_action, pinInfo.getTitle()) + " - " + globalFlag + variable.getTitle());
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
