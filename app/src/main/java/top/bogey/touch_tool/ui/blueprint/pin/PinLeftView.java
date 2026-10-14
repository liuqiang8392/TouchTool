package top.bogey.touch_tool.ui.blueprint.pin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.databinding.PinLeftBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;

@SuppressLint("ViewConstructor")
public class PinLeftView extends PinView {
    private final PinLeftBinding binding;

    public PinLeftView(@NonNull Context context, ActionCard card, Pin pin) {
        super(context, card, pin, false);
        binding = PinLeftBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    public void refreshPin() {
        super.refreshPin();
        binding.pinBox.setVisibility(pin.isLinked() ? GONE : VISIBLE);
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
        return binding.cpButton;
    }
}
