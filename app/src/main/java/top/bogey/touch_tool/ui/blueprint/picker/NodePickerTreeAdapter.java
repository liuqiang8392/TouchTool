package top.bogey.touch_tool.ui.blueprint.picker;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.databinding.FloatPickerNodeItemBinding;
import top.bogey.touch_tool.ui.custom.float_view.NodeInfoFloatView;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.tree.NormalTreeNode;
import top.bogey.touch_tool.utils.tree.TreeAdapter;
import top.bogey.touch_tool.utils.tree.TreeNode;

public class NodePickerTreeAdapter extends TreeAdapter {
    private final SelectNode picker;
    private final List<NodeInfo> roots;
    private TreeNode selectedNode;
    private RecyclerView recyclerView;

    public NodePickerTreeAdapter(SelectNode picker, List<NodeInfo> roots) {
        this.picker = picker;
        this.roots = roots;
        searchNodes(null);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public TreeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int layoutId) {
        FloatPickerNodeItemBinding binding = FloatPickerNodeItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    public void setRoots(List<NodeInfo> roots) {
        this.roots.clear();
        this.roots.addAll(roots);
        searchNodes(null);
    }

    public void setSelectedNode(NodeInfo nodeInfo) {
        if (selectedNode != null) {
            int index = treeNodes.indexOf(selectedNode);
            selectedNode = null;
            if (index >= 0) notifyItemChanged(index);
        }

        if (nodeInfo != null) {
            selectedNode = findTreeNode(treeNodes, nodeInfo);
            if (selectedNode != null) {
                expandNode(selectedNode);
                int index = treeNodes.indexOf(selectedNode);
                recyclerView.scrollToPosition(index);
            }
        }
    }

    public void searchNodes(String search) {
        List<TreeNode> treeNodes = new ArrayList<>();
        Pattern pattern = AppUtil.getPattern(search);
        for (NodeInfo root : roots) {
            TreeNode tree;
            if (pattern == null) {
                tree = new NormalTreeNode(root);
            } else {
                tree = createTree(root, pattern);
                if (tree != null) tree.setDepth(0);
            }
            if (tree != null) treeNodes.add(tree);
        }
        setTreeNodes(treeNodes);
        if (pattern != null) expandAll();
        else collapseAll();
    }

    private TreeNode createTree(NodeInfo node, @NonNull Pattern pattern) {
        boolean found = false;
        if (node.text != null && pattern.matcher(node.text).find()) found = true;
        else if (node.desc != null && pattern.matcher(node.desc).find()) found = true;
        else if (node.id != null && pattern.matcher(node.id).find()) found = true;
        else if (node.clazz != null && pattern.matcher(node.clazz).find()) found = true;

        TreeNode treeNode = new NormalTreeNode(node, Collections.emptyList());

        for (NodeInfo child : node.getChildren()) {
            TreeNode tree = createTree(child, pattern);
            if (tree != null) treeNode.addChild(tree);
        }

        if (treeNode.getChildren().isEmpty() && !found) return null;
        return treeNode;
    }

    private static TreeNode findTreeNode(List<TreeNode> treeNodes, Object value) {
        for (TreeNode treeNode : treeNodes) {
            if (treeNode.getData() == value) return treeNode;
            TreeNode childNode = findTreeNode(treeNode.getChildren(), value);
            if (childNode != null) return childNode;
        }
        return null;
    }

    public interface SelectNode {
        void selectNode(NodeInfo nodeInfo);
    }

    private class ViewHolder extends TreeAdapter.ViewHolder {
        private final FloatPickerNodeItemBinding binding;


        public ViewHolder(@NonNull FloatPickerNodeItemBinding binding) {
            super(NodePickerTreeAdapter.this, binding.getRoot());
            this.binding = binding;

            binding.visibleButton.setOnClickListener(v -> {
                NodeInfo nodeInfo = (NodeInfo) node.getData();
                if (nodeInfo == null) return;
                nodeInfo.visible = !nodeInfo.visible;
                notifyItemChanged(getBindingAdapterPosition());
            });

            binding.infoButton.setOnClickListener(v -> {
                NodeInfo nodeInfo = (NodeInfo) node.getData();
                NodeInfoFloatView.showInfo(nodeInfo, info -> {
                    picker.selectNode(info);
                    setSelectedNode(info);
                });
            });
        }

        @Override
        public boolean onLongClicked(View view) {
            NodeInfo nodeInfo = (NodeInfo) node.getData();
            picker.selectNode(nodeInfo);
            setSelectedNode(nodeInfo);
            return true;
        }

        @Override
        public void refresh(TreeNode node) {
            super.refresh(node);
            NodeInfo nodeInfo = (NodeInfo) node.getData();
            if (nodeInfo == null) return;

            binding.titleText.setText(getNodeTitle(nodeInfo));

            int color;
            if (node == selectedNode)
                color = DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorTertiaryContainer);
            else {
                color = DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorSurfaceContainerHighest);
            }
            binding.getRoot().setCardBackgroundColor(color);

            if (nodeInfo.usable && nodeInfo.visible) {
                color = DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorPrimaryVariant);
            } else {
                color = DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorOnSurface);
            }
            binding.titleText.setTextColor(color);

            binding.imageView.setImageTintList(ColorStateList.valueOf(color));
            binding.imageView.setVisibility(nodeInfo.getChildCount() == 0 ? View.INVISIBLE : View.VISIBLE);
            binding.imageView.setImageResource(node.isExpanded() ? R.drawable.icon_keyboard_arrow_up : R.drawable.icon_keyboard_arrow_down);

            binding.visibleButton.setIconResource(nodeInfo.visible ? R.drawable.icon_visibility : R.drawable.icon_visibility_off);
            binding.visibleButton.setAlpha(nodeInfo.visible ? 0.3f : 1);
        }

        private String getNodeTitle(NodeInfo nodeInfo) {
            StringBuilder builder = new StringBuilder();
            if (nodeInfo.text != null && !nodeInfo.text.isEmpty()) builder.append(nodeInfo.text).append(" | ");
            builder.append(nodeInfo);
            return builder.toString();
        }
    }
}
