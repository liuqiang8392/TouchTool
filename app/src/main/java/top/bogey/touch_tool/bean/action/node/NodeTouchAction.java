package top.bogey.touch_tool.bean.action.node;

import android.view.accessibility.AccessibilityNodeInfo;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.service.TaskRunnable;

public class NodeTouchAction extends ExecuteAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node);
    private final transient Pin ltPin = new Pin(new PinBoolean(), R.string.node_touch_action_long);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.node_touch_action_else, true);
    private final transient Pin precisePin = new Pin(new PinBoolean(), R.string.node_touch_action_precise);

    public NodeTouchAction() {
        super(ActionType.NODE_TOUCH);
        addPins(nodePin, ltPin, elsePin, precisePin);
    }

    public NodeTouchAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, ltPin, elsePin, precisePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinNode node = getPinValue(runnable, nodePin);
        if (node != null) {
            PinBoolean longTouch = getPinValue(runnable, ltPin);
            PinBoolean precise = getPinValue(runnable, precisePin);
            NodeInfo nodeInfo = node.getNodeInfo();
            if (nodeInfo != null) {
                AccessibilityNodeInfo targetNode;
                if (precise.getValue()) {
                    targetNode = nodeInfo.node;
                } else {
                    NodeInfo usableParent = nodeInfo.findUsableParent();
                    targetNode = usableParent != null ? usableParent.node : null;
                }
                if (targetNode != null) {
                    if (longTouch.getValue()) {
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)) {
                            executeNext(runnable, outPin);
                            return;
                        }
                    } else {
                        if (targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                            executeNext(runnable, outPin);
                            return;
                        }
                    }
                }
            }
        }
        executeNext(runnable, elsePin);
    }
}
