package top.bogey.touch_tool.ui.tool.task_manager;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

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
import top.bogey.touch_tool.databinding.DialogTaskManagerItemBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;

public class ImportTaskDialogAdapter extends RecyclerView.Adapter<ImportTaskDialogAdapter.ViewHolder> {
    private final List<TaskPackage> taskPackages = new ArrayList<>();
    private final TaskReference taskReference = new TaskReference();

    private final Set<String> importTags = new HashSet<>();

    private boolean importCopy = false;

    public ImportTaskDialogAdapter(List<TaskPackage> taskPackages) {
        this.taskPackages.addAll(taskPackages);
        for (TaskPackage taskPackage : taskPackages) {
            Task savedTask = TaskSaver.getInstance().getTask(taskPackage.getTask().getId());
            if (savedTask == null) taskReference.addTaskPackage(taskPackage);
        }
        AppUtil.chineseSort(this.taskPackages, TaskPackage::getTitle);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        DialogTaskManagerItemBinding binding = DialogTaskManagerItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(taskPackages.get(position));
    }

    @Override
    public int getItemCount() {
        return taskPackages.size();
    }

    public TaskRecord getTaskRecord(boolean importTag) {
        importTags.clear();
        Set<Task> tasks = new HashSet<>();
        for (Task task : taskReference.getTasks()) {
            if (importTag) {
                importTags.addAll(task.getUsedTags());
            } else {
                task.cleanInvalidTag();
            }
            tasks.add(task);
        }

        List<String> allTags = TagSaver.getInstance().getTags();
        Set<Variable> variables = new HashSet<>();
        for (Variable variable : taskReference.getVariables()) {
            if (importTag) {
                importTags.addAll(variable.getTags());
            } else {
                for (String tag : new ArrayList<>(variable.getTags())) {
                    if (allTags.contains(tag)) continue;
                    variable.removeTag(tag);
                }
            }
            variables.add(variable);
        }

        return new TaskRecord(tasks, variables);
    }

    public Set<String> getImportTags() {
        return importTags;
    }

    // 切换导入副本模式，刷新条目上的覆盖提示
    public void setImportCopy(boolean importCopy) {
        this.importCopy = importCopy;
        notifyDataSetChanged();
    }

    public void selectAll() {
        for (TaskPackage aPackage : taskPackages) {
            taskReference.addTaskPackage(aPackage);
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public void unselectAll() {
        for (TaskPackage aPackage : taskPackages) {
            taskReference.removeTaskPackage(aPackage);
        }
        notifyItemRangeChanged(0, getItemCount());
    }

    public void refreshTaskPackages(List<TaskPackage> newTasks) {
        taskPackages.clear();
        taskPackages.addAll(newTasks);
        AppUtil.chineseSort(taskPackages, TaskPackage::getTitle);
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final static int NORMAL_COLOR = androidx.appcompat.R.attr.colorPrimary;
        private final static int SPECIAL_COLOR = androidx.appcompat.R.attr.colorError;

        private final Context context;
        private final DialogTaskManagerItemBinding binding;
        private TaskPackage taskPackage;

        public ViewHolder(@NonNull DialogTaskManagerItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            context = binding.getRoot().getContext();

            binding.getRoot().setOnClickListener(v -> {
                if (!taskReference.removeTaskPackage(taskPackage)) {
                    taskReference.addTaskPackage(taskPackage);
                }
                notifyItemRangeChanged(0, getItemCount());
            });
        }

        public void refresh(TaskPackage taskPackage) {
            this.taskPackage = taskPackage;

            binding.title.setText(taskPackage.getTitle());
            binding.description.setVisibility((taskPackage.getDescription() == null || taskPackage.getDescription().isEmpty()) ? View.GONE : View.VISIBLE);
            binding.description.setText(taskPackage.getDescription());
            binding.errorText.setVisibility(View.GONE);

            int times = taskReference.getTaskUsageTimes(taskPackage.getTask());
            binding.getRoot().setChecked(times > 0);
            binding.getRoot().setEnabled(times <= 1);
            binding.getRoot().setChecked(times > 0);
            Task savedTask = TaskSaver.getInstance().getTask(taskPackage.getTask().getId());
            if (savedTask != null && times > 0) {
                binding.errorText.setVisibility(View.VISIBLE);
                binding.errorText.setText(context.getString(importCopy ? R.string.task_import_copy_tips : R.string.task_import_error_tips, savedTask.getTitle()));
            }

            binding.referenceCard.setVisibility(taskPackage.isEmpty() ? View.GONE : View.VISIBLE);
            binding.referenceBox.removeAllViews();
            for (TaskPackage aPackage : taskPackage.getTaskPackages()) {
                savedTask = TaskSaver.getInstance().getTask(aPackage.getTask().getId());

                int usageTimes = taskReference.getTaskUsageTimes(aPackage.getTask());
                ColorStateList color = ColorStateList.valueOf(DisplayUtil.getAttrColor(context, savedTask == null || usageTimes == 0 ? NORMAL_COLOR : SPECIAL_COLOR));
                Chip chip = new Chip(context);
                chip.setText(aPackage.getTitle());
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setChecked(usageTimes > 0);
                chip.setChipIconResource(R.drawable.icon_assignment);
                chip.setChipIconTint(color);
                chip.setTextColor(color);
                chip.setOnClickListener(v -> {
                    if (usageTimes <= 1) {
                        if (!taskReference.removeTaskPackage(aPackage)) {
                            taskReference.addTaskPackage(aPackage);
                        }
                        notifyItemRangeChanged(0, getItemCount());
                    } else {
                        chip.setChecked(true);
                    }
                });
                binding.referenceBox.addView(chip);
            }

            for (Variable var : taskPackage.getVariables()) {
                Variable variable = VariableSaver.getInstance().getVar(var.getId());
                int usageTimes = taskReference.getVariableTimes(var);
                ColorStateList color = ColorStateList.valueOf(DisplayUtil.getAttrColor(context, variable == null || usageTimes == 0 ? NORMAL_COLOR : SPECIAL_COLOR));

                Chip chip = new Chip(context);
                chip.setText(var.getTitle());
                chip.setCheckable(true);
                chip.setCheckedIconVisible(true);
                chip.setChecked(usageTimes > 0);
                chip.setChipIconResource(R.drawable.icon_note_stack);
                chip.setChipIconTint(color);
                chip.setTextColor(color);
                chip.setOnClickListener(v -> {
                    if (usageTimes <= 1) {
                        if (usageTimes == 0) {
                            taskReference.addVariable(var);
                        } else {
                            taskReference.removeVariable(var);
                        }
                    } else {
                        chip.setChecked(true);
                    }
                });
                binding.referenceBox.addView(chip);
            }
        }
    }
}
