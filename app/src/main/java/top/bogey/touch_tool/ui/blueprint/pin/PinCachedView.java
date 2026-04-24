package top.bogey.touch_tool.ui.blueprint.pin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.databinding.PinCachedBinding;
import top.bogey.touch_tool.utils.DisplayUtil;

@SuppressLint("ViewConstructor")
public class PinCachedView extends PinView {
    private final PinCachedBinding binding;

    public PinCachedView(@NonNull Context context, Pin pin) {
        super(context, null, pin, false);

        binding = PinCachedBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    public ViewGroup getSlotBox() {
        return binding.pinSlotBox;
    }

    public View getSlotDragBox() {
        return binding.pinSlotDragBox;
    }

    @Override
    public TextView getTitleView() {
        return binding.title;
    }

    @Override
    public Button getRemoveButton() {
        return null;
    }

    @Override
    public ViewGroup getWidgetBox() {
        return null;
    }

    @Override
    public Button getCopyAndPasteButton() {
        return null;
    }

    @Override
    public void expand(Action.ExpandType expandType) {
    }

    @Override
    public PointF getSlotPosInLayout(float scale) {
        return new PointF();
    }
}
