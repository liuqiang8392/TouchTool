package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBoolean;
import top.bogey.touch_tool.databinding.PinWidgetBooleanBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;

@SuppressLint("ViewConstructor")
public class PinWidgetBoolean extends PinWidget<PinBoolean> {
    private final PinWidgetBooleanBinding binding;

    public PinWidgetBoolean(@NonNull Context context, ActionCard card, PinView pinView, PinBoolean pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetBooleanBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    protected void initBase() {
        binding.enableSwitch.setChecked(pinBase.getValue());
        binding.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (pinBase.getValue() == isChecked) return;
            pinBase.setValue(isChecked);
            pinView.getPin().notifyValueUpdated(card.getTask());
        });
    }

    @Override
    protected void initCustom() {

    }
}
