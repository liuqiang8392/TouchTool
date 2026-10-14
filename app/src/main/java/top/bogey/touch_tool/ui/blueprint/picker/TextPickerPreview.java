package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.databinding.FloatPickerTextPreviewBinding;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public class TextPickerPreview extends BasePicker<String> {
    private String value;

    public TextPickerPreview(@NonNull Context context, ResultCallback<String> callback, String defaultValue) {
        super(context, callback);
        editable = true;
        value = defaultValue;

        FloatPickerTextPreviewBinding binding = FloatPickerTextPreviewBinding.inflate(LayoutInflater.from(context), this, true);
        binding.editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                value = s == null ? "" : s.toString();
            }
        });

        binding.saveButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(value);
            dismiss();
        });

        binding.backButton.setOnClickListener(v -> dismiss());
    }
}
