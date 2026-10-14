package top.bogey.touch_tool.ui.blueprint.card;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.appcompat.widget.ListPopupWindow;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.ActionCheckResult;
import top.bogey.touch_tool.bean.action.list.ListActionLinkEventHandler;
import top.bogey.touch_tool.bean.action.list.MakeListAction;
import top.bogey.touch_tool.bean.action.map.MakeMapAction;
import top.bogey.touch_tool.bean.action.map.MapActionLinkEventHandler;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinAdd;
import top.bogey.touch_tool.bean.pin.pin_objects.PinMap;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinSubType;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_list.PinList;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.CardCreateListBinding;
import top.bogey.touch_tool.ui.blueprint.pin.PinBottomView;
import top.bogey.touch_tool.ui.blueprint.pin.PinLeftView;
import top.bogey.touch_tool.ui.blueprint.pin.PinRightView;
import top.bogey.touch_tool.ui.blueprint.pin.PinTopView;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.DisplayUtil;

@SuppressLint("ViewConstructor")
public class CreateListActionCard extends ActionCard implements IDynamicPinCard {
    private final static Map<PinType, List<PinInfo>> PIN_INFO_MAP = PinInfo.getCustomPinInfoMap();
    private CardCreateListBinding binding;
    private CreateListActionAdapter pinAdapter;

    public CreateListActionCard(Context context, Task task, Action action) {
        super(context, task, action);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void init() {
        binding = CardCreateListBinding.inflate(LayoutInflater.from(getContext()), this, true);

        // 列表、字典的输入项可以拖动调换顺序
        if (action instanceof MakeListAction || action instanceof MakeMapAction) {
            pinAdapter = new CreateListActionAdapter(this);
            pinAdapter.attachToRecyclerView(binding.inPinBox);
        }

        initCardInfo(binding.icon, binding.title, binding.des);
        initEditDesc(binding.editButton, binding.des);
        initDelete(binding.removeButton);
        initCopy(binding.copyButton);
        initLock(binding.lockButton);
        initExpand(binding.expandButton);
        initPosView(binding.position);

        if (action instanceof MakeListAction) binding.valueSlot.setVisibility(GONE);
        binding.keySlot.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(getContext());
            List<PinInfo> pinInfoList = new ArrayList<>();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item);
            PIN_INFO_MAP.forEach((pinType, infoList) -> infoList.forEach(info -> {
                adapter.add(info.getTitle());
                pinInfoList.add(info);
            }));
            popup.setAdapter(adapter);
            popup.setAnchorView(binding.keySlot);
            popup.setModal(true);
            popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
            popup.setOnItemClickListener((parent, view, position, id) -> {
                PinInfo pinInfo = pinInfoList.get(position);
                binding.keySlot.setText(pinInfo.getTitle());
                if (action instanceof MakeListAction makeListAction) {
                    Pin listPin = makeListAction.getListPin();
                    if (!listPin.getValue().isDynamic()) makeListAction.getDynamicTypePins().forEach(pin -> pin.clearLinks(task));
                    ListActionLinkEventHandler.onLinkedTo(makeListAction.getDynamicTypePins(), task, listPin, new Pin(pinInfo.newInstance()), true);
                }

                if (action instanceof MakeMapAction makeMapAction) {
                    Pin mapPin = makeMapAction.getMapPin();
                    PinMap pinMap = mapPin.getValue();
                    if (!pinMap.isDynamicKey()) {
                        makeMapAction.getDynamicKeyTypePins().forEach(pin -> pin.clearLinks(task));
                        mapPin.clearLinks(task);
                    }

                    MapActionLinkEventHandler.onLinkedTo(makeMapAction.getDynamicTypePins(), makeMapAction.getDynamicKeyTypePins(), makeMapAction.getDynamicValueTypePins(), task, makeMapAction.getMapPin(), new Pin(new PinMap((PinObject) pinInfo.newInstance(), new PinObject(PinSubType.DYNAMIC))), true);
                }
                popup.dismiss();
            });
            popup.show();
        });

        binding.valueSlot.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(getContext());
            List<PinInfo> pinInfoList = new ArrayList<>();
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item);
            PIN_INFO_MAP.forEach((pinType, infoList) -> infoList.forEach(info -> {
                adapter.add(info.getTitle());
                pinInfoList.add(info);
            }));
            popup.setAdapter(adapter);
            popup.setAnchorView(binding.valueSlot);
            popup.setModal(true);
            popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
            popup.setOnItemClickListener((parent, view, position, id) -> {
                PinInfo pinInfo = pinInfoList.get(position);
                binding.valueSlot.setText(pinInfo.getTitle());
                if (action instanceof MakeMapAction makeMapAction) {
                    Pin mapPin = makeMapAction.getMapPin();
                    PinMap pinMap = mapPin.getValue();
                    if (!pinMap.isDynamicValue()) {
                        makeMapAction.getDynamicValueTypePins().forEach(pin -> pin.clearLinks(task));
                        mapPin.clearLinks(task);
                    }

                    MapActionLinkEventHandler.onLinkedTo(makeMapAction.getDynamicTypePins(), makeMapAction.getDynamicKeyTypePins(), makeMapAction.getDynamicValueTypePins(), task, mapPin, new Pin(new PinMap(new PinObject(PinSubType.DYNAMIC), (PinObject) pinInfo.newInstance())), true);
                }
                popup.dismiss();
            });
            popup.show();
        });
        refreshKeyValueType();
    }

    @Override
    public void refreshCardInfo() {
        initCardInfo(binding.icon, binding.title, binding.des);
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
    }

    private void refreshKeyValueType() {
        if (action instanceof MakeListAction makeListAction) {
            Pin listPin = makeListAction.getListPin();
            PinList pinList = listPin.getValue(PinList.class);
            PinInfo pinInfo = PinInfo.getPinInfo(pinList.getValueType());
            binding.keySlot.setText(pinInfo.getTitle());
        }

        if (action instanceof MakeMapAction makeMapAction) {
            Pin mapPin = makeMapAction.getMapPin();
            PinMap pinMap = mapPin.getValue(PinMap.class);
            PinInfo pinInfo = PinInfo.getPinInfo(pinMap.getKeyType());
            binding.keySlot.setText(pinInfo.getTitle());
            pinInfo = PinInfo.getPinInfo(pinMap.getValueType());
            binding.valueSlot.setText(pinInfo.getTitle());
        }
    }

    @Override
    public boolean check() {
        refreshKeyValueType();

        ActionCheckResult result = new ActionCheckResult();
        action.check(result, task);
        ActionCheckResult.Result importantResult = result.getImportantResult();
        if (importantResult != null) {
            binding.errorText.setVisibility(VISIBLE);
            binding.errorText.setText(importantResult.msg());
            binding.errorText.setBackgroundColor(DisplayUtil.getAttrColor(getContext(), importantResult.type().getBackgroundColor()));
            binding.errorText.setTextColor(DisplayUtil.getAttrColor(getContext(), importantResult.type().getTextColor()));
        } else {
            binding.errorText.setVisibility(GONE);
        }
        return importantResult == null || importantResult.type() != ActionCheckResult.ResultType.ERROR;
    }

    @Override
    public void addPinView(Pin pin, int offset) {
        PinView pinView;
        if (isListItem(pin)) {
            // 字典的值针脚要和前面的键针脚同一行，其余输入项各占一行
            boolean sameRow = action instanceof MakeMapAction makeMapAction && makeMapAction.getDynamicValueTypePins().contains(pin);
            pinView = sameRow ? pinAdapter.addPinToLast(pin) : pinAdapter.addPin(pin);
        } else if (pin.isOut()) {
            if (pin.isVertical()) {
                pinView = new PinBottomView(getContext(), this, pin);
                binding.bottomBox.addView(pinView, binding.bottomBox.getChildCount() - offset);
            } else {
                pinView = new PinRightView(getContext(), this, pin);
                binding.outBox.addView(pinView, binding.outBox.getChildCount() - offset);
            }
        } else if (pin.isVertical()) {
            pinView = new PinTopView(getContext(), this, pin);
            binding.topBox.addView(pinView, binding.topBox.getChildCount() - offset);
        } else {
            pinView = new PinLeftView(getContext(), this, pin);
            binding.inBox.addView(pinView, binding.inBox.getChildCount() - offset);
        }
        pinView.expand(action.getExpandType());
        pinViews.put(pin.getId(), pinView);
    }

    @Override
    public void removePinView(Pin pin) {
        if (isListItem(pin)) {
            pinViews.remove(pin.getId());
            pinAdapter.removePin(pin);
        } else {
            super.removePinView(pin);
        }
    }

    // 列表的输入项：由适配器管理、可拖动排序的左侧针脚，添加针脚除外
    private boolean isListItem(Pin pin) {
        if (pinAdapter == null) return false;
        if (pin.getValue() instanceof PinAdd) return false;
        return !pin.isOut() && !pin.isVertical();
    }

    @Override
    public void suppressLayout() {
        // 抑制列表滚动，让针脚连线能够生效
        binding.inPinBox.suppressLayout(true);
        postDelayed(() -> binding.inPinBox.suppressLayout(false), 100);
    }

    @Override
    public boolean isEmptyPosition(float x, float y) {
        float scale = getScaleX();

        List<MaterialButton> buttons = Arrays.asList(binding.editButton, binding.lockButton, binding.expandButton, binding.copyButton, binding.removeButton, binding.keySlot, binding.valueSlot);
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
}
