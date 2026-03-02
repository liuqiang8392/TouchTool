package top.bogey.touch_tool.ui.blueprint.pin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.databinding.PinInputConfigBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;

@SuppressLint("ViewConstructor")
public class PinInputConfigView extends PinView {
    private final PinInputConfigBinding binding;

    public PinInputConfigView(@NonNull Context context, ActionCard card, Pin pin) {
        super(context, card, pin, false);
        binding = PinInputConfigBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    public Button getRemoveButton() {
        return null;
    }

    @Override
    public ViewGroup getSlotBox() {
        return null;
    }

    @Override
    public TextView getTitleView() {
        return binding.title;
    }

    @Override
    public ViewGroup getWidgetBox() {
        return binding.pinBox;
    }

    @Override
    public void expand(Action.ExpandType expandType) {

    }

    @Override
    public Button getCopyAndPasteButton() {
        return null;
    }
}
