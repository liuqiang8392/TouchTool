package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.databinding.FloatPickerTouchPreviewBinding;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.ui.custom.float_view.TouchPathFloatView;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.ResultCallback;

@SuppressLint("ViewConstructor")
public class TouchPickerPreview extends BasePicker<PinTouchPath> {
    private final FloatPickerTouchPreviewBinding binding;
    private final PinTouchPath touchPath;

    public TouchPickerPreview(@NonNull Context context, ResultCallback<PinTouchPath> callback, PinTouchPath path) {
        super(context, callback);
        touchPath = new PinTouchPath(path.getPathParts());
        touchPath.setAnchor(path.getAnchor());

        binding = FloatPickerTouchPreviewBinding.inflate(LayoutInflater.from(context), this, true);

        binding.pathView.setPath(touchPath.getPathParts());

        binding.playButton.setOnClickListener(v -> {
            MainAccessibilityService service = MainApplication.getInstance().getService();
            if (service != null && service.isEnabled()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    service.runGesture(touchPath.getStrokesList(1, 0, 0), null);
                } else {
                    service.runGesture(touchPath.getStrokes(1, 0, 0), null);
                }
                TouchPathFloatView.showGesture(touchPath.getPathParts(EAnchor.TOP_LEFT), 1);
            }
        });

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            callback.onResult(touchPath);
            dismiss();
        });

        binding.pickerButton.setOnClickListener(v -> new TouchPicker(context, result -> {
            touchPath.setValue(result.getAnchor(), result.getValue());
            binding.pathView.setPath(result.getPathParts());
        }, touchPath).show());
    }
}
