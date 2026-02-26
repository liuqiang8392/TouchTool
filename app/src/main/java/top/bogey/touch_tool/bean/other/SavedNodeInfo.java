package top.bogey.touch_tool.bean.other;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.List;

public class SavedNodeInfo {
    public final List<SavedNodeInfo> children = new ArrayList<>();

    public String clazz;
    public String id;
    public int index;

    public String text;
    public String desc;
    public boolean usable;
    public boolean visible;
    public Rect area;

    public SavedNodeInfo(NodeInfo nodeInfo) {
        clazz = nodeInfo.clazz;
        id = nodeInfo.id;
        index = nodeInfo.index;

        text = nodeInfo.text;
        desc = nodeInfo.desc;
        usable = nodeInfo.usable;
        visible = nodeInfo.visible;
        area = nodeInfo.area;

        for (NodeInfo child : nodeInfo.getChildren()) {
            children.add(new SavedNodeInfo(child));
        }
    }
}
