package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinColor;
import top.bogey.touch_tool.databinding.PinWidgetColorBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.picker.ColorPickerPreview;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.DisplayUtil;

@SuppressLint("ViewConstructor")
public class PinWidgetColor extends PinWidget<PinColor> {
    private final PinWidgetColorBinding binding;

    public PinWidgetColor(@NonNull Context context, ActionCard card, PinView pinView, PinColor pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetColorBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    protected void initBase() {
        PinColor.ColorInfo colorInfo = pinBase.getValue();
        pinBase.setValue(colorInfo);
        binding.colorText.setText(colorInfo.getColorString());
        binding.color.setBackgroundColor(colorInfo.getColor());

        binding.pickButton.setOnClickListener(v -> new ColorPickerPreview(getContext(), result -> {
            colorInfo.setColor(result.getColor());
            colorInfo.setMinArea(result.getMinArea());
            colorInfo.setMaxArea(result.getMaxArea());
            pinView.getPin().notifyValueUpdated(card.getTask());
            binding.colorText.setText(colorInfo.getColorString());
            binding.colorText.setTextColor(DisplayUtil.getTextColor(colorInfo.getColor()));
            binding.color.setBackgroundColor(colorInfo.getColor());
        }, colorInfo).show());
    }

    @Override
    protected void initCustom() {

    }
}
