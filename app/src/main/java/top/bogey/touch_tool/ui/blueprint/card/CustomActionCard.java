package top.bogey.touch_tool.ui.blueprint.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.view.View;

import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.android.material.button.MaterialButton;

import java.util.List;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinString;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.CardCustomBinding;
import top.bogey.touch_tool.ui.blueprint.pin.PinBottomView;
import top.bogey.touch_tool.ui.blueprint.pin.PinLeftView;
import top.bogey.touch_tool.ui.blueprint.pin.PinRightView;
import top.bogey.touch_tool.ui.blueprint.pin.PinTopView;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.ui.DragViewHolderHelper;

@SuppressLint("ViewConstructor")
public class CustomActionCard extends ActionCard implements IDynamicPinCard {
    private CardCustomBinding binding;
    private CustomActionCardAdapter horizontalAdapter;
    private CustomActionCardAdapter verticalAdapter;

    public CustomActionCard(Context context, Task task, Action action) {
        super(context, task, action);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void init() {
        horizontalAdapter = new CustomActionCardAdapter(this);
        verticalAdapter = new CustomActionCardAdapter(this);
        binding = CardCustomBinding.inflate(LayoutInflater.from(getContext()), this, true);

        DragViewHolderHelper horizontalHelper = new DragViewHolderHelper(DragViewHolderHelper.VERTICAL, horizontalAdapter);
        ItemTouchHelper horizontalTouchHelper = new ItemTouchHelper(horizontalHelper);

        DragViewHolderHelper verticalHelper = new DragViewHolderHelper(DragViewHolderHelper.HORIZONTAL, verticalAdapter);
        ItemTouchHelper verticalTouchHelper = new ItemTouchHelper(verticalHelper);


        boolean isOut = action instanceof CustomStartAction;
        if (isOut) {
            binding.outPinBox.setAdapter(horizontalAdapter);
            horizontalTouchHelper.attachToRecyclerView(binding.outPinBox);

            binding.bottomPinBox.setAdapter(verticalAdapter);
            verticalTouchHelper.attachToRecyclerView(binding.bottomPinBox);

            ((View) binding.inPinBox.getParent()).setVisibility(GONE);
            ((View) binding.topPinBox.getParent()).setVisibility(GONE);

            binding.copyButton.setVisibility(GONE);
        } else {
            binding.inPinBox.setAdapter(horizontalAdapter);
            horizontalTouchHelper.attachToRecyclerView(binding.inPinBox);

            binding.topPinBox.setAdapter(verticalAdapter);
            verticalTouchHelper.attachToRecyclerView(binding.topPinBox);

            ((View) binding.outPinBox.getParent()).setVisibility(GONE);
            ((View) binding.bottomPinBox.getParent()).setVisibility(GONE);
        }

        initCardInfo(binding.icon, binding.title, binding.des);
        initEditDesc(binding.editButton, binding.des);
        initCopy(binding.copyButton);
        initDelete(binding.removeButton);
        initLock(binding.lockButton);
        initPosView(binding.position);

        binding.addButton.setOnClickListener(v -> action.addPin(new Pin(new PinString(), 0, isOut, true)));
        binding.addExecuteButton.setOnClickListener(v -> action.addPin(new Pin(new PinExecute(), 0, isOut, true)));
    }

    @Override
    public void refreshCardInfo() {
        initCardInfo(null, binding.title, binding.des);
    }

    @Override
    public void refreshCardLockState() {
        initLock(binding.lockButton);
    }

    // 展开模式切换会成片隐藏针脚，藏光了的那条列表会被压成 0 高并回收掉针脚视图，
    // 只改针脚可见性不会再触发列表布局，所以这里让列表重新量一次
    @Override
    public void setExpandType(Action.ExpandType expandType) {
        super.setExpandType(expandType);
        requestPinBoxLayout();
    }

    @Override
    public void expand() {
        super.expand();
        requestPinBoxLayout();
    }

    private void requestPinBoxLayout() {
        binding.inPinBox.requestLayout();
        binding.outPinBox.requestLayout();
        binding.topPinBox.requestLayout();
        binding.bottomPinBox.requestLayout();
    }

    @Override
    public boolean check() {
        return true;
    }

    @Override
    public void addPinView(Pin pin, int offset) {
        PinView pinView;
        if (pin.isDynamic()) {
            if (pin.isVertical()) {
                pinView = verticalAdapter.addPin(pin);
            } else {
                pinView = horizontalAdapter.addPin(pin);
            }
        } else {
            if (pin.isOut()) {
                if (pin.isVertical()) {
                    pinView = new PinBottomView(getContext(), this, pin);
                    binding.bottomBox.addView(pinView, binding.bottomBox.getChildCount() - offset);
                } else {
                    pinView = new PinRightView(getContext(), this, pin);
                    binding.outBox.addView(pinView, binding.outBox.getChildCount() - offset);
                }
            } else {
                if (pin.isVertical()) {
                    pinView = new PinTopView(getContext(), this, pin);
                    binding.topBox.addView(pinView, binding.topBox.getChildCount() - offset);
                } else {
                    pinView = new PinLeftView(getContext(), this, pin);
                    binding.inBox.addView(pinView, binding.inBox.getChildCount() - offset);
                }
            }
        }
        pinView.expand(action.getExpandType());
        pinViews.put(pin.getId(), pinView);
    }

    @Override
    public void removePinView(Pin pin) {
        if (pin.isDynamic()) {
            if (pin.isVertical()) {
                verticalAdapter.removePin(pin);
            } else {
                horizontalAdapter.removePin(pin);
            }
        } else {
            super.removePinView(pin);
        }
    }

    @Override
    public boolean isEmptyPosition(float x, float y) {
        float scale = getScaleX();

        List<MaterialButton> buttons = List.of(binding.lockButton, binding.addButton, binding.copyButton, binding.addExecuteButton, binding.removeButton, binding.editButton);
        for (MaterialButton button : buttons) {
            PointF pointF = DisplayUtil.getLocationRelativeToView(button, this);
            float px = pointF.x * scale;
            float py = pointF.y * scale;
            float width = button.getWidth() * scale;
            float height = button.getHeight() * scale;
            if (new RectF(px, py, px + width, py + height).contains(x, y)) return false;
        }
        return super.isEmptyPosition(x, y);
    }

    @Override
    public void suppressLayout() {
        binding.inPinBox.suppressLayout(true);
        binding.outPinBox.suppressLayout(true);
        binding.topPinBox.suppressLayout(true);
        binding.bottomPinBox.suppressLayout(true);
        postDelayed(() -> {
            binding.inPinBox.suppressLayout(false);
            binding.outPinBox.suppressLayout(false);
            binding.topPinBox.suppressLayout(false);
            binding.bottomPinBox.suppressLayout(false);
        }, 100);
    }
}
