package top.bogey.touch_tool.ui.tool.model_manager;

import android.content.Context;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.model.LiteRTModel;
import top.bogey.touch_tool.bean.save.model.ModelSaver;
import top.bogey.touch_tool.bean.save.model.YoloModel;
import top.bogey.touch_tool.databinding.ViewToolModelManagerItemBinding;
import top.bogey.touch_tool.utils.AppUtil;

public class ModelManagerAdapter extends RecyclerView.Adapter<ModelManagerAdapter.ViewHolder> {
    private final List<LiteRTModel> models = new ArrayList<>();

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ViewToolModelManagerItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(models.get(position));
    }

    @Override
    public int getItemCount() {
        return models.size();
    }

    public void refresh() {
        models.clear();
        models.addAll(ModelSaver.getInstance().getModelList());
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ModelSaver modelSaver = ModelSaver.getInstance();
        private final Context context;
        private final ViewToolModelManagerItemBinding binding;
        private final Handler handler;
        private boolean delete;
        private LiteRTModel model;

        public ViewHolder(ViewToolModelManagerItemBinding binding) {
            super(binding.getRoot());
            this.context = binding.getRoot().getContext();
            this.binding = binding;
            this.handler = new Handler();

            binding.delete.setOnClickListener(v -> {
                if (delete) {
                    modelSaver.removeModel(context, model.getId());
                    int index = getBindingAdapterPosition();
                    models.remove(index);
                    notifyItemRemoved(index);
                } else {
                    delete = true;
                    binding.delete.setChecked(true);
                    handler.postDelayed(() -> {
                        delete = false;
                        binding.delete.setChecked(false);
                    }, 2000);
                }
            });

            binding.getRoot().setOnClickListener(v -> {
                if (model instanceof YoloModel yoloModel) {
                    List<String> labels = yoloModel.getLabels();
                    String[] labelsArray = new String[labels.size()];
                    labels.toArray(labelsArray);

                    new MaterialAlertDialogBuilder(context)
                            .setItems(labelsArray, (dialog, which) -> AppUtil.copyToClipboard(context, labels.get(which)))
                            .setTitle(R.string.model_manager_model_labels)
                            .show();
                }
            });
        }

        public void refresh(LiteRTModel modelInfo) {
            this.model = modelInfo;

            binding.name.setText(modelInfo.getName());
            binding.desc.setText(modelInfo.getDescription());
            binding.time.setText(modelInfo.getType().toString() + "-" + AppUtil.formatDate(context, modelInfo.getTime(), true));
            binding.version.setText(modelInfo.getAuthor());
        }
    }
}
