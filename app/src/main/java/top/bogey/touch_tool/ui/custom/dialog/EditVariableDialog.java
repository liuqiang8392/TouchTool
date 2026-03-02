package top.bogey.touch_tool.ui.custom.dialog;

import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.TagSaver;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.databinding.DialogCreateTaskBinding;
import top.bogey.touch_tool.databinding.ViewTagListItemBinding;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.callback.BooleanResultCallback;

public class EditVariableDialog extends MaterialAlertDialogBuilder {
    private final DialogCreateTaskBinding binding;
    private final List<String> selectedTags = new ArrayList<>();
    private BooleanResultCallback callback;

    public EditVariableDialog(@NonNull Context context, Variable var) {
        super(context);

        binding = DialogCreateTaskBinding.inflate(LayoutInflater.from(context), null, false);
        setView(binding.getRoot());

        binding.titleEdit.setText(var.getTitle());
        binding.desEdit.setText(var.getDescription());

        binding.addTagBtn.setOnClickListener(v -> AppUtil.showEditDialog(context, R.string.task_tag_add, "", result -> {
            if (result != null && !result.isEmpty()) {
                TagSaver.getInstance().addTag(result);
                createChip(result);
            }
        }));

        List<String> tags = TagSaver.getInstance().getTags();

        List<String> currTags = var.getTags();
        if (currTags != null) {
            for (String tag : currTags) {
                if (tags.contains(tag)) selectedTags.add(tag);
            }
        }

        for (String tag : tags) {
            createChip(tag);
        }

        setPositiveButton(R.string.enter, (dialog, which) -> {
            dialog.dismiss();
            if (getTitle().isEmpty()) {
                callback.onResult(false);
                return;
            }
            var.setTitle(getTitle());
            var.setDescription(getDescription());
            var.setTags(selectedTags);
            callback.onResult(true);
        });

        setNegativeButton(R.string.cancel, (dialog, which) -> {
            dialog.dismiss();
            callback.onResult(false);
        });
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        binding.titleEdit.postDelayed(() -> {
            binding.titleEdit.requestFocus();
            InputMethodManager imm = (InputMethodManager) dialog.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(binding.titleEdit, InputMethodManager.SHOW_IMPLICIT);
        }, 100);
        return dialog;
    }

    public void setCallback(BooleanResultCallback callback) {
        this.callback = callback;
    }

    private String getTitle() {
        Editable text = binding.titleEdit.getText();
        if (text != null && text.length() > 0) return text.toString();
        return "";
    }

    private String getDescription() {
        Editable text = binding.desEdit.getText();
        if (text != null && text.length() > 0) return text.toString();
        return "";
    }

    private void createChip(String tag) {
        ViewTagListItemBinding itemBinding = ViewTagListItemBinding.inflate(LayoutInflater.from(getContext()), binding.tagBox, true);
        Chip chip = itemBinding.getRoot();
        chip.setOnCloseIconClickListener(v -> AppUtil.showDialog(getContext(), R.string.tag_remove, result -> {
            if (result) {
                TagSaver.getInstance().removeTag(tag);
                selectedTags.remove(tag);
                binding.tagBox.removeView(chip);
            }
        }));

        chip.setText(tag);
        chip.setChecked(selectedTags.contains(tag));

        chip.setOnClickListener(v -> {
            if (!selectedTags.remove(tag)) {
                selectedTags.add(tag);
            }
        });
    }
}
