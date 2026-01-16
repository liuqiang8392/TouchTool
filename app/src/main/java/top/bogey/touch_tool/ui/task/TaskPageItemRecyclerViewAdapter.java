package top.bogey.touch_tool.ui.task;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.base.Identity;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaveListener;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.ViewTaskPageItemActionBinding;
import top.bogey.touch_tool.databinding.ViewTaskPageItemBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.custom.EditTaskDialog;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.ui.IDragAbleRecycleViewAdapter;

public class TaskPageItemRecyclerViewAdapter extends RecyclerView.Adapter<TaskPageItemRecyclerViewAdapter.ViewHolder> implements TaskSaveListener, IDragAbleRecycleViewAdapter {
    private final TaskView taskView;
    private ItemTouchHelper helper;

    private String tag;
    private List<Task> tasks = new ArrayList<>();

    public TaskPageItemRecyclerViewAdapter(TaskView taskView) {
        this.taskView = taskView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewTaskPageItemBinding binding = ViewTaskPageItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(tasks.get(position));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    @Override
    public void onCreate(Task task) {
        if (TaskSaver.matchTag(tag, task.getTags())) {
            tasks.add(task);
            AppUtil.runOnUiThread(() -> notifyItemInserted(tasks.size() - 1));
        }
    }

    @Override
    public void onUpdate(Task task) {
        int index = tasks.indexOf(task);
        if (index != -1) {
            if (TaskSaver.matchTag(tag, task.getTags())) {
                tasks.set(index, task);
                AppUtil.runOnUiThread(() -> notifyItemChanged(index));
            } else {
                onRemove(task);
            }
        } else {
            onCreate(task);
        }
    }

    @Override
    public void onRemove(Task task) {
        int index = tasks.indexOf(task);
        if (index != -1) {
            tasks.remove(index);
            AppUtil.runOnUiThread(() -> notifyItemRemoved(index));
        }
    }

    public void setTasks(@NonNull String tag, List<Task> tasks) {
        this.tag = tag;
        List<String> order = TagSaver.getInstance().getTaskOrder(tag);
        if (order != null) {
            List<Task> orderTasks = new ArrayList<>();
            for (String id : order) {
                for (Task task : tasks) {
                    if (id.equals(task.getId())) {
                        orderTasks.add(task);
                        tasks.remove(task);
                        break;
                    }
                }
            }
            tasks.addAll(0, orderTasks);
        } else {
            AppUtil.chineseSort(tasks, Identity::getTitle);
        }
        this.tasks = tasks;
        notifyDataSetChanged();
    }

    public void setHelper(ItemTouchHelper helper) {
        this.helper = helper;
    }

    @Override
    public void swap(int from, int to) {
        tasks.add(to, tasks.remove(from));
        List<String> order = new ArrayList<>();
        for (Task task : tasks) {
            order.add(task.getId());
        }
        TagSaver.getInstance().setTaskOrder(tag, order);
        notifyItemMoved(from, to);
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return false;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewTaskPageItemBinding binding;
        private final Context context;

        public ViewHolder(@NonNull ViewTaskPageItemBinding binding) {
            super(binding.getRoot());

            this.binding = binding;
            context = binding.getRoot().getContext();

            binding.getRoot().setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                Task task = tasks.get(position);

                if (taskView.selecting) {
                    if (!taskView.selected.remove(task.getId())) {
                        taskView.selected.add(task.getId());
                    }
                    notifyItemChanged(position);
                } else {
                    if (AppUtil.isRelease(context)) {
                        MainAccessibilityService service = MainApplication.getInstance().getService();
                        if (service == null || !service.isEnabled()) {
                            Toast.makeText(context, R.string.app_setting_enable_desc, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    NavController controller = Navigation.findNavController(MainApplication.getInstance().getActivity(), R.id.conView);
                    controller.navigate(TaskViewDirections.actionTaskToBlueprint(task.getId()));
                }
            });

            binding.getRoot().setOnLongClickListener(v -> {
                int position = getBindingAdapterPosition();
                Task task = tasks.get(position);

                if (!taskView.selecting) {
                    taskView.showBottomBar();
                    taskView.selected.add(task.getId());
                    binding.getRoot().setChecked(true);
                }
                helper.startDrag(this);
                return true;
            });

            binding.editButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                Task task = tasks.get(position);

                EditTaskDialog dialog = new EditTaskDialog(context, task);
                dialog.setTitle(context.getString(R.string.task_update));
                dialog.setCallback(result -> {
                    if (result) task.save();
                });
                dialog.show();
            });

            binding.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                int position = getBindingAdapterPosition();
                Task task = tasks.get(position);
                if (task.isEnable() == isChecked) return;
                task.setEnable(isChecked);
                task.save();
            });

            binding.stopButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                Task task = tasks.get(position);

                MainAccessibilityService service = MainApplication.getInstance().getService();
                if (service != null && service.isEnabled()) {
                    service.stopTask(task);
                }
            });
        }

        public void refresh(Task task) {
            binding.taskName.setText(task.getTitle());
            binding.taskDesc.setVisibility((task.getDescription() == null || task.getDescription().isEmpty()) ? View.GONE : View.VISIBLE);
            binding.taskDesc.setText(task.getDescription());

            binding.actionsBox.removeAllViews();
            for (Action action : task.getActions(StartAction.class)) {
                StartAction startAction = (StartAction) action;
                ViewTaskPageItemActionBinding actionBinding = ViewTaskPageItemActionBinding.inflate(LayoutInflater.from(context), binding.actionsBox, true);
                actionBinding.taskDesc.setText(startAction.getFullDescription());
                actionBinding.enableSwitch.setChecked(startAction.isEnable());
                actionBinding.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked == startAction.isEnable()) return;
                    startAction.setEnable(isChecked);
                    task.save();
                });
            }

            binding.timeText.setText(AppUtil.formatDate(context, task.getCreateTime(), true));

            String tagString = task.getTagString();
            binding.taskTag.setText(tagString);
            binding.taskTag.setVisibility(tagString.isEmpty() ? View.INVISIBLE : View.VISIBLE);

            if (binding.enableSwitch.isChecked() != task.isEnable()) binding.enableSwitch.setChecked(task.isEnable());

            ActionCheckResult result = new ActionCheckResult();
            task.check(result);
            if (result.hasError()) {
                binding.errorText.setText(result.getError().msg());
                binding.errorText.setVisibility(View.VISIBLE);
            } else {
                binding.errorText.setVisibility(View.GONE);
            }

            binding.getRoot().setChecked(taskView.selected.contains(task.getId()));
        }
    }
}
