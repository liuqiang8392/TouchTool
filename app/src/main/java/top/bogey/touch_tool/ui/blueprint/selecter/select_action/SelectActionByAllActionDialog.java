package top.bogey.touch_tool.ui.blueprint.selecter.select_action;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.utils.callback.ResultCallback;

public class SelectActionByAllActionDialog extends SelectActionDialog {

    public SelectActionByAllActionDialog(@NonNull Context context, Task task, ResultCallback<Action> callback) {
        super(context, task, callback);
    }

    @Override
    protected void initAdapter(ResultCallback<Action> callback) {
        adapter = new SelectActionItemRecyclerViewAdapter(this, callback, true);
        binding.actionsBox.setAdapter(adapter);
    }

    @Override
    protected GroupType[] getGroupTypes() {
        return new GroupType[]{GroupType.TASK};
    }

    @Override
    protected Map<String, List<Object>> getGroupData(GroupType groupType) {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        if (groupType == GroupType.TASK) {
            // 私有任务
            List<Object> privateTasks = new ArrayList<>(task.getTasks());
            map.put(PRIVATE, privateTasks);
            subGroupMap.put(PRIVATE, task);

            // 公共任务
            List<Object> publicTasks = new ArrayList<>(TaskSaver.getInstance().getOrderTasks());
            map.put(GLOBAL, publicTasks);
            subGroupMap.put(GLOBAL, GLOBAL);

            // 父任务
            Task parent = task.getParent();
            while (parent != null) {
                List<Object> list = new ArrayList<>(parent.getTasks());
                if (!list.isEmpty()) {
                    map.put(PARENT_PREFIX + parent.getTitle(), list);
                    subGroupMap.put(PARENT_PREFIX + parent.getTitle(), parent);
                }
                parent = parent.getParent();
            }
        }
        return map;
    }
}
