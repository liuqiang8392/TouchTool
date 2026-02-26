package top.bogey.touch_tool.bean.action.node;

import com.google.gson.JsonObject;

import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.ActionType;
import top.bogey.touch_tool.bean.action.parent.ExecuteAction;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.PinNode;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinArea;
import top.bogey.touch_tool.service.TaskRunnable;

public class GetNodesInAreaAction extends ExecuteAction {
    private final transient Pin areaPin = new Pin(new PinArea(), R.string.pin_area);
    private final transient Pin nodesPin = new Pin(new PinList(new PinNode()), true);
    private final transient Pin firstNodePin = new Pin(new PinNode(), R.string.pin_node_top, true);

    public GetNodesInAreaAction() {
        super(ActionType.GET_NODES_IN_AREA);
        addPins(areaPin, nodesPin, firstNodePin);
    }

    public GetNodesInAreaAction(JsonObject jsonObject) {
        super(jsonObject);
        reAddPins(areaPin, nodesPin, firstNodePin);
    }

    @Override
    public void execute(TaskRunnable runnable, Pin pin) {
        PinArea area = getPinValue(runnable, areaPin);
        PinList nodes = nodesPin.getValue();

        NodeInfo nodeInfo = NodeInfo.getActiveWindow();
        if (nodeInfo != null) {
            List<NodeInfo> childrenInArea = nodeInfo.findChildren(area.getValue(), false);
            for (NodeInfo info : childrenInArea) {
                nodes.add(new PinNode(info));
            }

            if (!nodes.isEmpty()) {
                firstNodePin.setValue(nodes.get(nodes.size() - 1));
            }
        }

        executeNext(runnable, outPin);
    }
}
