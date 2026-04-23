package top.bogey.touch_tool.ui.task;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.ViewTagListItemBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.ui.IDragAbleRecycleViewAdapter;

public class TagViewAdapter extends RecyclerView.Adapter<TagViewAdapter.ViewHolder> implements IDragAbleRecycleViewAdapter {

    private final TaskView taskView;
    private final Set<String> selectedTags = new HashSet<>();
    private final List<String> tags = new ArrayList<>();

    public TagViewAdapter(TaskView taskView) {
        this.taskView = taskView;
        tags.addAll(TagSaver.getInstance().getTags());
        if (taskView.selecting) {
            for (String id : taskView.selected) {
                Task task = TaskSaver.getInstance().getTask(id);
                if (selectedTags.isEmpty()) {
                    if (task != null && task.getTags() != null) selectedTags.addAll(task.getTags());
                } else {
                    if (task != null && task.getTags() != null) selectedTags.retainAll(task.getTags());
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ViewTagListItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(tags.get(position));
    }

    @Override
    public int getItemCount() {
        return tags.size();
    }

    public void addTag(String tag) {
        tags.add(tag);
        notifyItemInserted(tags.size() - 1);
    }

    @Override
    public void swap(int from, int to) {
        tags.add(to, tags.remove(from));
        notifyItemMoved(from, to);
        TagSaver.getInstance().setTags(tags);
        taskView.resetTags();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ViewTagListItemBinding binding;
        private final Context context;

        public ViewHolder(@NonNull ViewTagListItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            context = binding.getRoot().getContext();

            binding.getRoot().setCloseIconVisible(true);
            binding.getRoot().setCheckable(taskView.selecting);

            binding.getRoot().setOnCloseIconClickListener(v -> {
                int index = getBindingAdapterPosition();
                String tag = tags.get(index);
                AppUtil.showDialog(context, R.string.tag_remove, result -> {
                    if (result) {
                        TagSaver.getInstance().removeTag(tag);
                        if (tags.remove(tag)) notifyItemRemoved(index);
                    }
                });
            });

            binding.getRoot().setOnClickListener(v -> {
                int index = getBindingAdapterPosition();
                String tag = tags.get(index);
                if (taskView.selecting) {
                    if (selectedTags.remove(tag)) {
                        for (String id : taskView.selected) {
                            Task task = TaskSaver.getInstance().getTask(id);
                            task.removeTag(tag);
                            task.save();
                        }
                    } else {
                        selectedTags.add(tag);
                        for (String id : taskView.selected) {
                            Task task = TaskSaver.getInstance().getTask(id);
                            task.addTag(tag);
                            task.save();
                        }
                    }
                    notifyItemChanged(index);
                } else {
                    taskView.gotoTargetTag(tag);
                }
            });
        }

        public void refresh(String tag) {
            binding.getRoot().setText(tag);
            if (taskView.selecting) {
                binding.getRoot().setChecked(selectedTags.contains(tag));
            } else {
                binding.getRoot().setChecked(false);
            }
        }
    }
}
