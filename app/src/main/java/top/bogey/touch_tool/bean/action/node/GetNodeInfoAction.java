package top.bogey.touch_tool.bean.action.node;

import com.google.gson.JsonObject;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.CalculateAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinNodePathString;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetNodeInfoAction extends CalculateAction {
    private final transient Pin nodePin = new Pin(new PinNode(), R.string.pin_node);
    private final transient Pin clazzPin = new Pin(new PinString(), R.string.get_node_info_action_clazz, true);
    private final transient Pin idPin = new Pin(new PinString(), R.string.get_node_info_action_id, true, false, true);
    private final transient Pin textPin = new Pin(new PinString(), R.string.pin_string, true);
    private final transient Pin descPin = new Pin(new PinString(), R.string.get_node_info_action_node_desc, true, false, true);
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area, true);
    private final transient Pin pathPin = new Pin(new PinNodePathString(), R.string.pin_string_node_path, true, false, true);
    private final transient Pin usablePin = new Pin(new PinBoolean(), R.string.get_node_info_action_usable, true, false, true);
    private final transient Pin visiblePin = new Pin(new PinBoolean(), R.string.get_node_info_action_visible, true, false, true);

    public GetNodeInfoAction() {
        super(ActionType.GET_NODE_INFO);
        addPins(nodePin, clazzPin, idPin, textPin, descPin, areaPin, usablePin, visiblePin);
    }

    public GetNodeInfoAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(nodePin, clazzPin, idPin, textPin, descPin, areaPin, usablePin, visiblePin);
    }

    @Override
    public void calculate(TaskRunnable runnable, Pin pin) {
        PinNode node = getPinValue(runnable, nodePin);
        if (node == null) return;
        NodeInfo nodeInfo = node.getNodeInfo();
        if (nodeInfo == null) return;
        clazzPin.getValue(PinString.class).setValue(nodeInfo.clazz);
        idPin.getValue(PinString.class).setValue(nodeInfo.id);
        textPin.getValue(PinString.class).setValue(nodeInfo.text);
        descPin.getValue(PinString.class).setValue(nodeInfo.desc);
        areaPin.getValue(PinArea.class).setValue(nodeInfo.area);
        pathPin.getValue(PinNodePathString.class).setValue(nodeInfo);
        usablePin.getValue(PinBoolean.class).setValue(nodeInfo.usable);
        visiblePin.getValue(PinBoolean.class).setValue(nodeInfo.visible);
    }
}
