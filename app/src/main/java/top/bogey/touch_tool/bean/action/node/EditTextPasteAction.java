package top.bogey.touch_tool.bean.action.node;

import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.EditText;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.special_pin.ShowAblePin;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.service.TaskRunnable;

public class EditTextPasteAction extends ExecuteAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.edit_text_paste_action_edit_text, false, false, true);
    private final transient Pin enterPin = new EnterShowablePin(new PinBoolean(), R.string.edit_text_paste_action_enter, false, false, true);
    private final transient Pin elsePin = new Pin(new PinExecute(), R.string.node_touch_action_else, true);

    public EditTextPasteAction() {
        super(ActionType.EDITTEXT_PASTE);
        addPins(nodePin, enterPin, elsePin);
    }

    public EditTextPasteAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, enterPin, elsePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        NodeInfo nodeInfo = null;
        if (nodePin.isLinked()) {
            PinNode node = getPinValue(runnable, nodePin);
            nodeInfo = node.getNodeInfo();
        } else {
            for (NodeInfo window : NodeInfo.getWindows()) {
                NodeInfo child = window.findChild(EditText.class.getName());
                if (child != null) {
                    nodeInfo = child;
                    break;
                }
            }
        }
        PinBoolean enter = getPinValue(runnable, enterPin);

        boolean result = true;
        if (nodeInfo != null && nodeInfo.usable && nodeInfo.node != null && nodeInfo.node.isFocusable()) {
            nodeInfo.node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            result = nodeInfo.node.performAction(AccessibilityNodeInfo.ACTION_PASTE);

            if (result && enter.getValue() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                nodeInfo.node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.getId());
            }
        }
        executeNext(runnable, result ? outPin : elsePin);
    }

    private static class EnterShowablePin extends ShowAblePin {
        public EnterShowablePin(PinBase value, int titleId, boolean out, boolean dynamic, boolean hide) {
            super(value, titleId, out, dynamic, hide);
        }

        @Override
        public boolean showAble(Task context) {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
        }
    }
}
