package top.bogey.touch_tool.ui.blueprint.pin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.special_pin.AlwaysShowPin;
import top.bogey.touch_tool.databinding.PinRightBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;

@SuppressLint("ViewConstructor")
public class PinRightView extends PinView {
    private final PinRightBinding binding;

    public PinRightView(@NonNull Context context, ActionCard card, Pin pin) {
        super(context, card, pin, false);

        binding = PinRightBinding.inflate(LayoutInflater.from(context), this, true);
        // 输出针脚不需要UI，除非是常显针脚
        binding.pinBox.setVisibility(pin instanceof AlwaysShowPin ? VISIBLE : GONE);

        init();
    }

    @Override
    public ViewGroup getSlotBox() {
        return binding.pinSlotBox;
    }

    @Override
    public TextView getTitleView() {
        return binding.title;
    }

    @Override
    public Button getDragButton() {
        return binding.dragButton;
    }

    @Override
    public Button getRemoveButton() {
        return binding.removeButton;
    }

    @Override
    public ViewGroup getWidgetBox() {
        return binding.pinBox;
    }

    @Override
    public Button getCopyAndPasteButton() {
        return null;
    }
}
