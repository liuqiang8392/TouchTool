package top.bogey.touch_tool.ui.tool.task_manager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.save.variable.VariableSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.databinding.DialogTaskManagerBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.GsonUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public class ImportTaskDialog extends FrameLayout {
    private static void showDialog(Context context, ImportTaskDialog view) {
        new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.task_import)
                .setView(view)
                .setPositiveButton(R.string.import_task, (dialog, which) -> view.importTask())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    public static void showDialog(Context context, Uri uri) {
        if (uri == null) return;

        byte[] bytes = AppUtil.readFile(context, uri);
        TaskRecord taskRecord = null;
        try {
            taskRecord = GsonUtil.getAsObject(new String(bytes), TaskRecord.class, null);
        } catch (Exception ignored) {
        }
        if (taskRecord == null) return;
        ImportTaskDialog view = new ImportTaskDialog(context, taskRecord);
        showDialog(context, view);
    }

    private final DialogTaskManagerBinding binding;
    private final ImportTaskDialogAdapter adapter;


    public ImportTaskDialog(@NonNull Context context, TaskRecord taskRecord) {
        super(context);
        binding = DialogTaskManagerBinding.inflate(LayoutInflater.from(context), this, true);

        List<TaskPackage> taskPackages = new ArrayList<>();
        for (Task task : taskRecord.tasks()) {
            TaskPackage taskPackage = new TaskPackage(taskRecord, task);
            taskPackages.add(taskPackage);
        }

        adapter = new ImportTaskDialogAdapter(taskPackages);
        binding.selectionBox.setAdapter(adapter);

        binding.searchEdit.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                String searchString = s.toString();
                if (searchString.isEmpty()) {
                    adapter.refreshTaskPackages(taskPackages);
                } else {
                    List<TaskPackage> searchTasks = new ArrayList<>();
                    for (TaskPackage taskPackage : taskPackages) {
                        if (AppUtil.isStringContains(taskPackage.getTitle(), searchString)) searchTasks.add(taskPackage);
                    }
                    adapter.refreshTaskPackages(searchTasks);
                }
            }
        });

        binding.selectAllButton.setOnClickListener(v -> {
            if (binding.selectAllButton.isChecked()) {
                adapter.selectAll();
            } else {
                adapter.unselectAll();
            }
        });

        // 打开副本模式后，条目下方的提示文案也跟着变
        binding.importCopy.setOnCheckedChangeListener((buttonView, isChecked) -> adapter.setImportCopy(isChecked));

        TaskRecord record = adapter.getTaskRecord(true);
        if (record.tasks() != null && record.variables() != null) {
            binding.selectAllButton.setChecked(record.tasks().size() == taskRecord.tasks().size() && record.variables().size() == taskRecord.variables().size());
        } else {
            binding.selectAllButton.setChecked(false);
        }
    }

    public void importTask() {
        TaskSaver taskSaver = TaskSaver.getInstance();
        VariableSaver variableSaver = VariableSaver.getInstance();

        boolean importCopy = binding.importCopy.isChecked();
        TaskRecord taskRecord = adapter.getTaskRecord(binding.importTag.isChecked());
        if (importCopy) {
            // 本地已有任务的标题，用来给副本错开名字
            Set<String> localTitles = new HashSet<>();
            for (Task task : taskSaver.getTasks()) {
                localTitles.add(task.getTitle());
            }

            taskRecord = taskRecord.duplicate();
            for (Task task : taskRecord.tasks()) {
                task.setTitle(getUniqueTitle(task.getTitle(), localTitles));
            }
        }

        for (Task task : taskRecord.tasks()) {
            taskSaver.saveTask(task);
        }
        for (Variable variable : taskRecord.variables()) {
            // 副本模式不覆盖本地已有的全局变量
            if (importCopy && variableSaver.getVar(variable.getId()) != null) continue;
            variableSaver.saveVar(variable);
        }

        if (!binding.importTag.isChecked()) return;
        TagSaver tagSaver = TagSaver.getInstance();
        List<String> tags = tagSaver.getTags();
        for (String tag : adapter.getImportTags()) {
            if (!tags.contains(tag)) {
                tags.add(tag);
            }
        }
        tagSaver.setTags(tags);
    }

    // 标题与本地已有任务冲突时，依次尝试 “x_复制”、“x_复制_2”
    private String getUniqueTitle(String title, Set<String> titles) {
        if (title == null) title = "";
        if (!titles.contains(title)) return title;

        String copyTitle = getContext().getString(R.string.copy_title, title);
        String result = copyTitle;
        int index = 2;
        while (titles.contains(result)) {
            result = copyTitle + "_" + index;
            index++;
        }
        return result;
    }
}
