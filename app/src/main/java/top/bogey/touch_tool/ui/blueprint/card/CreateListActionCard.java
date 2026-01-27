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
public class CreateListActionCard extends ActionCard {
    private final static Map<PinType, List<PinInfo>> PIN_INFO_MAP = PinInfo.getCustomPinInfoMap();
    private CardCreateListBinding binding;

    public CreateListActionCard(Context context, Task task, Action action) {
        super(context, task, action);
    }

    @Override
    public void init() {
        binding = CardCreateListBinding.inflate(LayoutInflater.from(getContext()), this, true);

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
            binding.errorText.setBackgroundColor(DisplayUtil.getAttrColor(getContext(), importantResult.type() == ActionCheckResult.ResultType.ERROR ? com.google.android.material.R.attr.colorErrorContainer : com.google.android.material.R.attr.colorTertiaryContainer));
            binding.errorText.setTextColor(DisplayUtil.getAttrColor(getContext(), importantResult.type() == ActionCheckResult.ResultType.ERROR ? com.google.android.material.R.attr.colorOnErrorContainer : com.google.android.material.R.attr.colorOnTertiaryContainer));
        } else {
            binding.errorText.setVisibility(GONE);
        }
        return importantResult == null || importantResult.type() != ActionCheckResult.ResultType.ERROR;
    }

    @Override
    public void addPinView(Pin pin, int offset) {
        PinView pinView;
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
        pinView.expand(action.getExpandType());
        pinViews.put(pin.getId(), pinView);
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
