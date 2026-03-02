package top.bogey.touch_tool.bean.action.node;

import com.google.gson.JsonObject;

import java.util.concurrent.atomic.AtomicBoolean;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.service.TaskRunnable;
import top.bogey.touch_tool.ui.blueprint.picker.NodePicker;

public class PickNodeAction extends ExecuteAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node, true);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.pick_node_action_else, true);

    public PickNodeAction() {
        super(ActionType.PICK_NODE);
        addPins(nodePin, elsePin);
    }

    public PickNodeAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        AtomicBoolean aBoolean = new AtomicBoolean(false);
        NodePicker.showPicker(result -> {
            if (result != null) {
                nodePin.getValue(PinNode.class).setNodeInfo(result);
                aBoolean.set(true);
            }
            runnable.resume();
        });
        runnable.await();
        if (aBoolean.get()) executeNext(runnable, outPin);
        else executeNext(runnable, elsePin);
    }
}
