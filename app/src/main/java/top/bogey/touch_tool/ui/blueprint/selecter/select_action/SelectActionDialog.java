package top.bogey.touch_tool.ui.blueprint.selecter.select_action;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionMap;
import top.bogey.touch_tool.bean.action.task.CustomEndAction;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.ITagManager;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.databinding.DialogSelectActionBinding;
import top.bogey.touch_tool.databinding.WidgetSettingSelectButtonHorizontalBinding;
import top.bogey.touch_tool.databinding.WidgetSettingSelectButtonVerticalBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.custom.EditTaskDialog;
import top.bogey.touch_tool.ui.custom.EditVariableDialog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

public class SelectActionDialog extends BottomSheetDialog {
    protected final String GLOBAL = getContext().getString(R.string.select_action_group_global);
    protected final String PRIVATE = getContext().getString(R.string.select_action_group_private);
    protected final static String PARENT_PREFIX = "👨";
    protected final static String CHILD_PREFIX = "👶";
    protected final static String TAG_PREFIX = "🔗";
    public final static String GLOBAL_FLAG = "🌍 ";

    public static Object copyObject;

    protected final DialogSelectActionBinding binding;
    protected final Task task;
    protected SelectActionItemRecyclerViewAdapter adapter;

    protected GroupType groupType = GroupType.PRESET;
    protected Map<String, List<Object>> sourceDataMap = new LinkedHashMap<>();
    protected Map<String, List<Object>> dataMap = new LinkedHashMap<>();
    protected String subGroupTag;
    protected Map<String, Object> subGroupMap = new LinkedHashMap<>();
    protected List<Object> dataList = Collections.emptyList();

    public SelectActionDialog(@NonNull Context context, Task task, ResultCallback<Action> callback) {
        super(context);
        this.task = task;

        binding = DialogSelectActionBinding.inflate(LayoutInflater.from(context));
        setContentView(binding.getRoot());

        BottomSheetBehavior<FrameLayout> behavior = getBehavior();
        behavior.setDraggable(false);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);

        initAdapter(callback);

        binding.group.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;

            View view = group.findViewById(checkedId);
            String groupText = ((MaterialButton) view).getText().toString();
            SettingSaver.getInstance().setLastGroup(groupText);

            groupType = (GroupType) view.getTag();
            binding.addButton.setTag(groupType);

            updateGroupData(groupType);
            refreshSubGroup(dataMap);
            setCopyObject(copyObject);
        });

        binding.subGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked) return;
            View view = group.findViewById(checkedId);
            String subText = ((MaterialButton) view).getText().toString();
            SettingSaver.getInstance().setLastSubGroup(subText);
            subGroupTag = (String) view.getTag();
            List<Object> list = dataMap.get(subGroupTag);
            dataList = (list == null) ? Collections.emptyList() : list;
            adapter.setData(dataList, groupType != GroupType.PRESET);
            boolean showAdd = Objects.equals(subGroupTag, GLOBAL) || Objects.equals(subGroupTag, PRIVATE);
            binding.addButton.setVisibility(showAdd ? View.VISIBLE : View.GONE);
        });

        String[] groupName = getContext().getResources().getStringArray(R.array.group_type);
        String lastGroup = SettingSaver.getInstance().getLastGroup();
        int index = 0;
        GroupType[] groupTypes = getGroupTypes();
        for (int i = 0; i < groupTypes.length; i++) {
            GroupType gt = groupTypes[i];
            WidgetSettingSelectButtonHorizontalBinding buttonBinding =
                    WidgetSettingSelectButtonHorizontalBinding.inflate(LayoutInflater.from(getContext()), binding.group, true);
            MaterialButton btn = buttonBinding.getRoot();
            btn.setId(View.generateViewId());
            btn.setText(groupName[gt.ordinal()]);
            btn.setTag(gt);
            if (lastGroup.equals(groupName[gt.ordinal()])) index = i;
        }
        if (binding.group.getChildCount() > index) {
            binding.group.check(binding.group.getChildAt(index).getId());
        }
        binding.searchEdit.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                search();
            }
        });

        binding.pasteButton.setOnClickListener(v -> {
            if (copyObject == null) return;
            saveToSubGroup(copyObject);
            adapter.addData(copyObject);
            setCopyObject(null);
        });

        binding.searchButton.setOnClickListener(v -> {
            boolean showing = binding.searchBox.getVisibility() == View.VISIBLE;
            setSearchBoxVisible(!showing);
        });

        binding.addButton.setOnClickListener(v -> {
            GroupType gt = (GroupType) binding.addButton.getTag();
            switch (gt) {
                case TASK -> showNewTaskDialog();
                case VARIABLE -> showNewVariableDialog();
            }
        });

        setCopyObject(copyObject);

        MainActivity activity = MainApplication.getInstance().getActivity();
        View decorView = activity.getWindow().getDecorView();
        behavior.setMaxWidth(decorView.getWidth());
        DisplayUtil.setViewHeight(binding.getRoot(), (int) (decorView.getHeight() * 0.75f));
    }

    protected void initAdapter(ResultCallback<Action> callback) {
        adapter = new SelectActionItemRecyclerViewAdapter(this, callback);
        binding.actionsBox.setAdapter(adapter);
    }

    private void showNewVariableDialog() {
        Variable variable = new Variable(new PinString());
        EditVariableDialog dialog = new EditVariableDialog(getContext(), variable);
        dialog.setTitle(R.string.variable_add);
        dialog.setCallback(result -> {
            if (!result) return;

            String tag = getCheckedSubGroupTag();
            if (tag == null) return;

            if (PRIVATE.equals(tag)) task.addVariable(variable);

            variable.save();
            if (!(dataList instanceof ArrayList)) dataList = new ArrayList<>(dataList);
            dataList.add(0, variable);
            adapter.notifyItemInserted(0);
        });
        dialog.show();
    }

    private void showNewTaskDialog() {
        Task newTask = new Task();
        EditTaskDialog dialog = new EditTaskDialog(getContext(), newTask);
        dialog.setTitle(R.string.task_add);
        dialog.setCallback(result -> {
            if (!result) return;

            String tag = getCheckedSubGroupTag();
            if (tag == null) return;

            if (PRIVATE.equals(tag)) {
                task.addTask(newTask);
                newTask.addAction(new CustomStartAction());
                CustomEndAction customEndAction = new CustomEndAction();
                customEndAction.setPos(0, 30);
                newTask.addAction(customEndAction);
            }

            newTask.save();
            if (!(dataList instanceof ArrayList)) dataList = new ArrayList<>(dataList);
            dataList.add(0, newTask);
            adapter.notifyItemInserted(0);
        });
        dialog.show();
    }

    private Map<String, List<Object>> calculateTagGroup(Map<String, List<Object>> dataMap) {
        Map<String, List<Object>> map = new HashMap<>();
        dataMap.forEach((key, value) -> {
            if (GLOBAL.equals(key) || PRIVATE.equals(key)) {
                for (Object o : value) {
                    if (o instanceof ITagManager) {
                        List<String> tags = ((ITagManager) o).getTags();
                        for (String tag : tags) {
                            map.computeIfAbsent(tag, k -> new ArrayList<>()).add(o);
                        }
                    }
                }
            }
        });

        ArrayList<String> keys = new ArrayList<>(map.keySet());
        AppUtil.chineseSort(keys, tag -> tag);

        LinkedHashMap<String, List<Object>> linkedHashMap = new LinkedHashMap<>();
        for (String key : keys) {
            linkedHashMap.put(TAG_PREFIX + key, map.get(key));
        }
        return linkedHashMap;
    }

    protected void refreshSubGroup(Map<String, List<Object>> dataMap) {
        Map<String, List<Object>> merged = new LinkedHashMap<>(dataMap);
        merged.putAll(calculateTagGroup(merged));
        this.dataMap = merged;

        refreshSubGroup(merged.keySet().toArray(new String[0]));
    }

    private void refreshSubGroup(String[] chips) {
        binding.subGroup.clearChecked();
        binding.subGroup.removeAllViews();

        if (chips == null || chips.length == 0) {
            dataList = Collections.emptyList();
            adapter.setData(dataList, groupType != GroupType.PRESET);
            binding.addButton.setVisibility(View.GONE);
            return;
        }

        String subGroup = SettingSaver.getInstance().getLastSubGroup();
        int index = 0;

        for (int i = 0; i < chips.length; i++) {
            String s = chips[i];
            WidgetSettingSelectButtonVerticalBinding buttonBinding =
                    WidgetSettingSelectButtonVerticalBinding.inflate(LayoutInflater.from(getContext()), binding.subGroup, true);
            MaterialButton button = buttonBinding.getRoot();
            button.setId(View.generateViewId());
            button.setText(s);
            button.setTag(s);
            if (subGroup.equals(s)) index = i;
        }

        binding.subGroup.check(binding.subGroup.getChildAt(index).getId());
    }

    protected GroupType[] getGroupTypes() {
        return new GroupType[]{GroupType.PRESET, GroupType.TASK, GroupType.VARIABLE};
    }

    protected void deleteSameObject(Object object) {
        dataMap.forEach((key, value) -> {
            for (int i = value.size() - 1; i >= 0; i--) {
                Object o = value.get(i);
                if (o.equals(object)) {
                    value.remove(i);
                }
            }
        });
    }

    protected Map<String, List<Object>> getGroupData(GroupType groupType) {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        subGroupMap.clear();

        switch (groupType) {
            case PRESET -> {
                for (ActionMap.ActionGroupType actionGroupType : ActionMap.ActionGroupType.values()) {
                    List<Object> types = new ArrayList<>(ActionMap.getTypes(actionGroupType));
                    map.put(actionGroupType.getName(), types);
                    subGroupMap.put(actionGroupType.getName(), actionGroupType.getName());
                }
            }
            case TASK -> {
                // 私有任务
                List<Object> privateTasks = new ArrayList<>(task.getTasks());
                map.put(PRIVATE, privateTasks);
                subGroupMap.put(PRIVATE, task);

                // 公共任务
                List<Object> publicTasks = new ArrayList<>(TaskSaver.getInstance().getTasks());
                map.put(GLOBAL, publicTasks);
                subGroupMap.put(GLOBAL, GLOBAL);

                // 父任务
                Task parent = task.getParent();
                while (parent != null) {
                    List<Object> list = new ArrayList<>(parent.getTasks());
                    if (!list.isEmpty()) {
                        String key = PARENT_PREFIX + parent.getTitle();
                        map.put(key, list);
                        subGroupMap.put(key, parent);
                    }
                    parent = parent.getParent();
                }

                // 子任务（BFS）
                Queue<Task> queue = new LinkedList<>(task.getTasks());
                while (!queue.isEmpty()) {
                    Task poll = queue.poll();
                    if (poll == null) continue;

                    List<Task> tasks = poll.getTasks();
                    if (!tasks.isEmpty()) {
                        String key = CHILD_PREFIX + poll.getTitle();
                        map.put(key, new ArrayList<>(tasks));
                        subGroupMap.put(key, poll);
                        queue.addAll(tasks);
                    }
                }
            }
            case VARIABLE -> {
                // 私有变量
                List<Object> privateVars = new ArrayList<>(task.getVariables());
                map.put(PRIVATE, privateVars);
                subGroupMap.put(PRIVATE, task);

                // 全局变量
                List<Object> publicVars = new ArrayList<>(VariableSaver.getInstance().getVars());
                map.put(GLOBAL, publicVars);
                subGroupMap.put(GLOBAL, GLOBAL);

                // 父级变量
                Task parent = task.getParent();
                while (parent != null) {
                    List<Object> list = new ArrayList<>(parent.getVariables());
                    if (!list.isEmpty()) {
                        String key = PARENT_PREFIX + parent.getTitle();
                        map.put(key, list);
                        subGroupMap.put(key, parent);
                    }
                    parent = parent.getParent();
                }

                // 子级变量（BFS）
                Queue<Task> queue = new LinkedList<>(task.getTasks());
                while (!queue.isEmpty()) {
                    Task poll = queue.poll();
                    if (poll == null) continue;

                    List<Object> list = new ArrayList<>(poll.getVariables());
                    if (!list.isEmpty()) {
                        String key = CHILD_PREFIX + poll.getTitle();
                        map.put(key, list);
                        subGroupMap.put(key, poll);
                    }
                    queue.addAll(poll.getTasks());
                }
            }
        }
        return map;
    }

    public void search() {
        if (adapter == null) return;

        Editable text = binding.searchEdit.getText();
        String q = text == null ? "" : text.toString().trim();

        if (q.isEmpty()) {
            dataMap = new LinkedHashMap<>(sourceDataMap);
            refreshSubGroup(dataMap);
            return;
        }

        Map<String, List<Object>> filteredMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<Object>> entry : sourceDataMap.entrySet()) {
            String groupKey = entry.getKey();
            List<Object> list = entry.getValue();

            if (list == null || list.isEmpty()) continue;

            List<Object> filteredList = new ArrayList<>();
            for (Object object : list) {
                String name = SelectActionItemRecyclerViewAdapter.getObjectTitle(object);
                if (AppUtil.isStringContainsWithPinyin(name, q)) {
                    filteredList.add(object);
                }
            }

            if (!filteredList.isEmpty()) {
                if (GLOBAL.equals(groupKey)) {
                    // GLOBAL 内部去重（保序），避免“全局标签重复项”
                    filteredList = new ArrayList<>(new LinkedHashSet<>(filteredList));
                }
                filteredMap.put(groupKey, filteredList);
            }
        }

        dataMap = filteredMap;
        refreshSubGroup(dataMap);
    }

    public void setCopyObject(Object object) {
        copyObject = object;

        binding.pasteButton.setVisibility(View.GONE);
        if (groupType == GroupType.TASK && object instanceof Task) {
            binding.pasteButton.setVisibility(View.VISIBLE);
        }
        if (groupType == GroupType.VARIABLE && object instanceof Variable) {
            binding.pasteButton.setVisibility(View.VISIBLE);
        }
    }

    protected enum GroupType {
        PRESET, TASK, VARIABLE
    }

    private void updateGroupData(GroupType gt) {
        sourceDataMap = getGroupData(gt);
        dataMap = new LinkedHashMap<>(sourceDataMap);
    }

    private String getCheckedSubGroupTag() {
        int id = binding.subGroup.getCheckedButtonId();
        if (id == View.NO_ID) return null;
        View view = binding.subGroup.findViewById(id);
        if (view == null) return null;
        Object tag = view.getTag();
        return (tag instanceof String) ? (String) tag : null;
    }

    private void setSearchBoxVisible(boolean visible) {
        binding.searchBox.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) {
            binding.searchEdit.setText("");
            return;
        }
        binding.searchEdit.requestFocus();
        InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.searchEdit, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void saveToSubGroup(Object copied) {
        if (groupType == GroupType.TASK && copied instanceof Task copy) {
            Object o = subGroupMap.get(subGroupTag);
            if (o instanceof Task parent) {
                parent.addTask(copy);
            } else if (o instanceof String tag) {
                if (GLOBAL.equals(tag)) TaskSaver.getInstance().saveTask(copy);
            } else if (o == null) {
                copy.getTags().clear();
                copy.addTag(subGroupTag.replace(TAG_PREFIX, ""));
                TaskSaver.getInstance().saveTask(copy);
            }
            return;
        }

        if (groupType == GroupType.VARIABLE && copied instanceof Variable copy) {
            Object o = subGroupMap.get(subGroupTag);
            if (o instanceof Task parent) {
                parent.addVariable(copy);
            } else if (o instanceof String tag) {
                if (GLOBAL.equals(tag)) VariableSaver.getInstance().saveVar(copy);
            } else if (o == null) {
                copy.getTags().clear();
                copy.addTag(subGroupTag.replace(TAG_PREFIX, ""));
                VariableSaver.getInstance().saveVar(copy);
            }
        }
    }
}
