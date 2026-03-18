package top.bogey.touch_tool.bean.action.system;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.service.super_user.CmdResult;
import top.bogey.touch_tool.service.super_user.ISuperUser;
import top.bogey.touch_tool.service.super_user.SuperUser;

public class ExecuteShellAction extends ExecuteAction {
    private final transient Pin cmdPin = new Pin(new PinString(), R.string.execute_shell_action_cmd);
    private final transient Pin outputPin = new Pin(new PinString(), R.string.execute_shell_action_output, true);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.execute_shell_action_else, true);

    public ExecuteShellAction() {
        super(ActionType.SHELL);
        addPins(cmdPin, outputPin, elsePin);
    }

    public ExecuteShellAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(cmdPin, outputPin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinObject cmd = getPinValue(runnable, cmdPin);

        ISuperUser superUser = SuperUser.getInstance();
        if (superUser.isValid() && superUser.init()) {
            CmdResult cmdResult = superUser.runCommand(cmd.toString());
            if (cmdResult != null) {
                outputPin.getValue(PinString.class).setValue(cmdResult.getOutput());
            }
            if (cmdResult != null && cmdResult.getResult()) {
                executeNext(runnable, outPin);
                return;
            }
        }
        executeNext(runnable, elsePin);
    }

    @Override
    public void check(ActionCheckResult result, Task task) {
        super.check(result, task);
        if (!SuperUser.getInstance().isValid()) {
            result.addResult(ActionCheckResult.ResultType.ERROR, R.string.check_need_super_user_error);
        }
    }
}
