package top.bogey.touch_tool.bean.other;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Stack;
import java.util.regex.Pattern;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.tree.ITreeNodeData;

public class NodeInfo extends SimpleNodeInfo implements ITreeNodeData {
    public final transient AccessibilityNodeInfo node;
    private final transient List<NodeInfo> children = new ArrayList<>();

    public String text;
    public String desc;
    public boolean usable;
    public boolean visible;
    public Rect area;


    public NodeInfo(AccessibilityNodeInfo node) {
        this.node = node;

        CharSequence className = node.getClassName();
        if (className != null) clazz = className.toString();

        id = node.getViewIdResourceName();

        CharSequence nodeText = node.getText();
        if (nodeText != null) text = nodeText.toString();

        CharSequence nodeDesc = node.getContentDescription();
        if (nodeDesc != null) desc = nodeDesc.toString();

        usable = node.isEnabled() && (node.isClickable() || node.isLongClickable() || node.isEditable());
        visible = node.isVisibleToUser();
        area = new Rect();
        node.getBoundsInScreen(area);

        for (int i = 0; i < node.getChildCount(); i++) {
            children.add(null);
        }
    }

    public int getChildCount() {
        return node.getChildCount();
    }

    public List<NodeInfo> getChildren() {
        List<NodeInfo> children = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            NodeInfo child = getChild(i);
            if (child != null) children.add(child);
        }
        return children;
    }

    public List<NodeInfo> getCacheChildren() {
        return children;
    }

    public NodeInfo getChild(int index) {
        if (children.isEmpty()) return null;
        NodeInfo nodeInfo = children.get(index);
        if (nodeInfo == null) {
            AccessibilityNodeInfo child = node.getChild(index);
            if (child != null) {
                nodeInfo = new NodeInfo(child);
                nodeInfo.index = index + 1;
                children.set(index, nodeInfo);
            }
        }
        return nodeInfo;
    }

    public NodeInfo findUsableChild(int x, int y) {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            NodeInfo child = getChild(i);
            if (child == null) continue;
            NodeInfo result = child.findUsableChild(x, y);
            if (result != null) return result;
        }
        if (area.contains(x, y) && usable && visible) return this;
        return null;
    }

    public NodeInfo getParent() {
        AccessibilityNodeInfo parent = node.getParent();
        if (parent == null) return null;
        NodeInfo nodeInfo = new NodeInfo(parent);
        AccessibilityNodeInfo grandParent = parent.getParent();
        if (grandParent != null) {
            for (int i = 0; i < grandParent.getChildCount(); i++) {
                AccessibilityNodeInfo child = grandParent.getChild(i);
                if (parent.equals(child)) {
                    nodeInfo.index = i + 1;
                    break;
                }
            }
        }
        return nodeInfo;
    }

    public NodeInfo findUsableParent() {
        NodeInfo parent = this;
        while (parent != null) {
            if (parent.usable) return parent;
            parent = parent.getParent();
        }
        return null;
    }

    public static List<NodeInfo> getWindows() {
        List<NodeInfo> rootNodes = new ArrayList<>();
        for (AccessibilityNodeInfo window : AppUtil.getWindows(MainApplication.getInstance().getService())) {
            rootNodes.add(new NodeInfo(window));
        }
        return rootNodes;
    }

    public static NodeInfo getActiveWindow() {
        MainAccessibilityService service = MainApplication.getInstance().getService();
        if (service == null || !service.isEnabled()) return null;
        AccessibilityNodeInfo window = service.getRootInActiveWindow();
        return window == null ? null : new NodeInfo(window);
    }

    public NodeInfo findChild(SimpleNodeInfo nodeInfo, boolean fullPath) {
        // 先根据class，id，index一起查找
        if (nodeInfo.index > 0 && nodeInfo.index <= getChildCount()) {
            NodeInfo child = getChild(nodeInfo.index - 1);
            if (child != null) {
                if (nodeInfo.matchNodeClass(child) && nodeInfo.matchNodeId(child)) return child;
            }
        }

        //带标记，却没有找到，不再继续
        if (fullPath) return null;

        // 如果没找到，再根据class，id查找
        for (int i = 0; i < getChildCount(); i++) {
            NodeInfo child = getChild(i);
            if (child != null) {
                if (nodeInfo.matchNodeClass(child) && nodeInfo.matchNodeId(child)) return child;
            }
        }

        // 如果还是没找到，再根据class，index查找
        if (nodeInfo.index > 0 && nodeInfo.index <= getChildCount()) {
            NodeInfo child = getChild(nodeInfo.index - 1);
            if (child != null) {
                if (nodeInfo.matchNodeClass(child)) return child;
            }
        }

        //如果还是没找到，再根据class查找
        for (int i = 0; i < getChildCount(); i++) {
            NodeInfo child = getChild(i);
            if (child != null) {
                if (nodeInfo.matchNodeClass(child)) return child;
            }
        }

        return null;
    }

    public NodeInfo findChild(String className) {
        Class<?> viewClazz;
        try {
            viewClazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }

        Stack<NodeInfo> stack = new Stack<>();
        if (area.contains(this.area) || Rect.intersects(area, this.area)) stack.push(this);
        while (!stack.isEmpty()) {
            NodeInfo node = stack.pop();
            if (node == null) continue;

            try {
                Class<?> clazz = Class.forName(node.clazz);
                if (viewClazz.isAssignableFrom(clazz)) return node;
            } catch (ClassNotFoundException ignored) {
            }

            for (NodeInfo child : node.getChildren()) {
                if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                    stack.push(child);
                }
            }
        }
        return null;
    }

    public List<NodeInfo> findChildren(Rect area, boolean justUsable) {
        List<NodeInfo> nodes = new ArrayList<>();
        Queue<NodeInfo> queue = new LinkedList<>();
        if (area.contains(this.area) || Rect.intersects(area, this.area)) queue.add(this);
        while (!queue.isEmpty()) {
            NodeInfo node = queue.poll();
            if (node == null) continue;
            if (!node.visible) continue;

            if (node.usable || !justUsable) {
                nodes.add(node);
            }

            for (NodeInfo child : node.getChildren()) {
                if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                    queue.add(child);
                }
            }
        }
        return nodes;
    }

    public void mapChildrenDepth(Map<NodeInfo, Integer> map, Rect area, int depth) {
        for (NodeInfo child : getChildren()) {
            if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                map.put(child, depth);
                child.mapChildrenDepth(map, area, depth + 1);
            }
        }
    }

    public List<NodeInfo> findChildrenByText(String text, Rect area) {
        List<NodeInfo> nodes = new ArrayList<>();

        String regexMetaChars = ".*+?^$|\\[]{}()";
        boolean flag = true;
        for (char c : regexMetaChars.toCharArray()) {
            if (text.indexOf(c) != -1) {
                flag = false;
                break;
            }
        }

        if (flag) {
            if (area.contains(this.area) || Rect.intersects(area, this.area)) {
                if (this.text != null && this.text.contains(text)) nodes.add(this);

                List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByText(text);
                for (AccessibilityNodeInfo node : list) {
                    if (node == null) continue;
                    NodeInfo nodeInfo = new NodeInfo(node);
                    if (area.contains(nodeInfo.area) || Rect.intersects(area, nodeInfo.area)) {
                        nodes.add(nodeInfo);
                    }
                }
            }
        }

        if (!flag || nodes.isEmpty()) {
            Stack<NodeInfo> stack = new Stack<>();
            Pattern pattern = AppUtil.getPattern(text);
            if (area.contains(this.area) || Rect.intersects(area, this.area)) stack.push(this);
            while (!stack.isEmpty()) {
                NodeInfo node = stack.pop();
                if (node == null) continue;

                if (node.text != null) {
                    if (pattern == null) {
                        if (node.text.toLowerCase().contains(text.toLowerCase())) nodes.add(node);
                    } else {
                        if (pattern.matcher(node.text).find()) nodes.add(node);
                    }
                }

                for (NodeInfo child : node.getChildren()) {
                    if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                        stack.push(child);
                    }
                }
            }
        }

        return nodes;
    }

    public List<NodeInfo> findChildrenById(String id, Rect area) {
        List<NodeInfo> nodes = new ArrayList<>();
        if (area.contains(this.area) || Rect.intersects(area, this.area)) {
            if (id.equals(this.id)) nodes.add(this);

            List<AccessibilityNodeInfo> list = node.findAccessibilityNodeInfosByViewId(id);
            if (list == null || list.isEmpty()) {
                Stack<NodeInfo> stack = new Stack<>();
                stack.push(this);
                Pattern pattern = AppUtil.getPattern(id);
                while (!stack.isEmpty()) {
                    NodeInfo node = stack.pop();
                    if (node == null) continue;

                    if (node.id != null) {
                        if (pattern == null) {
                            if (node.id.toLowerCase().contains(id.toLowerCase())) nodes.add(node);
                        } else {
                            if (pattern.matcher(node.id).find()) nodes.add(node);
                        }
                    }

                    for (NodeInfo child : node.getChildren()) {
                        if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                            stack.push(child);
                        }
                    }
                }
            } else {
                for (AccessibilityNodeInfo node : list) {
                    if (node == null) continue;
                    NodeInfo nodeInfo = new NodeInfo(node);
                    if (area.contains(nodeInfo.area) || Rect.intersects(area, nodeInfo.area)) {
                        nodes.add(nodeInfo);
                    }
                }
            }
        }

        return nodes;
    }

    public List<NodeInfo> findChildrenByClass(String className, Rect area) {
        List<NodeInfo> nodes = new ArrayList<>();
        Class<?> viewClazz = null;
        try {
            viewClazz = Class.forName(className);
        } catch (ClassNotFoundException ignored) {
        }

        Stack<NodeInfo> stack = new Stack<>();
        if (area.contains(this.area) || Rect.intersects(area, this.area)) stack.push(this);
        while (!stack.isEmpty()) {
            NodeInfo node = stack.pop();
            if (node == null) continue;

            if (viewClazz == null) {
                if (AppUtil.isStringContains(node.clazz, className)) nodes.add(node);
            } else {
                try {
                    Class<?> clazz = Class.forName(node.clazz);
                    if (viewClazz.isAssignableFrom(clazz)) nodes.add(node);
                } catch (ClassNotFoundException ignored) {
                }
            }

            for (NodeInfo child : node.getChildren()) {
                if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                    stack.push(child);
                }
            }
        }
        return nodes;
    }


    public List<NodeInfo> findChildrenByDesc(String desc, Rect area) {
        List<NodeInfo> nodes = new ArrayList<>();

        Stack<NodeInfo> stack = new Stack<>();
        Pattern pattern = AppUtil.getPattern(desc);
        if (area.contains(this.area) || Rect.intersects(area, this.area)) stack.push(this);
        while (!stack.isEmpty()) {
            NodeInfo node = stack.pop();
            if (node == null) continue;

            if (node.desc != null) {
                if (pattern == null) {
                    if (node.desc.toLowerCase().contains(desc.toLowerCase())) nodes.add(node);
                } else {
                    if (pattern.matcher(node.desc).find()) nodes.add(node);
                }
            }

            for (NodeInfo child : node.getChildren()) {
                if (area.contains(child.area) || Rect.intersects(area, child.area)) {
                    stack.push(child);
                }
            }
        }

        return nodes;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(clazz);
        if (id != null && !id.isEmpty()) builder.append("[id=").append(id).append("]");
        if (index > 1) builder.append("[").append(index).append("]");
        return builder.toString();
    }

    public String getPath() {
        StringBuilder builder = new StringBuilder();
        builder.append(clazz);
        if (id != null && !id.isEmpty()) builder.append("[id=").append(id).append("]");
        builder.append("[").append(index).append("]");
        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        NodeInfo nodeInfo = (NodeInfo) o;
        return usable == nodeInfo.usable && visible == nodeInfo.visible && Objects.equals(text, nodeInfo.text) && Objects.equals(desc, nodeInfo.desc) && Objects.equals(area, nodeInfo.area);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), text, desc, usable, visible, area);
    }

    @Override
    public List<ITreeNodeData> getChildrenData() {
        return new ArrayList<>(getChildren());
    }
}
