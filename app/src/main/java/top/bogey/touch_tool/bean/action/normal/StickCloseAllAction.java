package top.bogey.touch_tool.bean.action.normal;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.custom.float_view.StickScreenFloatView;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

public class StickCloseAllAction extends ExecuteAction {

    public StickCloseAllAction() {
        super(ActionType.CLOSE_ALL_STICK);
    }

    public StickCloseAllAction(JsonObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        FloatWindow.dismiss(StickScreenFloatView.class);
        executeNext(runnable, outPin);
    }
}
