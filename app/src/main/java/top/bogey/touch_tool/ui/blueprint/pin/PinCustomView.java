package top.bogey.touch_tool.ui.blueprint.pin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.text.Editable;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.PinInfo;
import top.bogey.touch_tool.bean.pin.pin_objects.PinObject;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.bean.task.Variable;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.card.IDynamicPinCard;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public abstract class PinCustomView extends PinView {
    private final static Map<PinType, List<PinInfo>> PIN_INFO_MAP = PinInfo.getCustomPinInfoMap();
    private final static int[] ICON_ARRAY = new int[]{R.drawable.icon_variables, R.drawable.icon_data_array, R.drawable.icon_map};

    private Variable variable;
    protected final Map<PinType, List<PinInfo>> pinInfoMap;

    public PinCustomView(@NonNull Context context, ActionCard card, Pin pin) {
        super(context, card, pin, true);
        pinInfoMap = new LinkedHashMap<>(PIN_INFO_MAP);
        if (pin.getValue() instanceof PinObject pinObject) {
            variable = new Variable((PinObject) pinObject.copy());
        } else {
            variable = null;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void init() {
        super.init();
        MaterialButton keyTypeView = getKeyTypeView();
        if (keyTypeView != null) {
            keyTypeView.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(getContext());
                List<PinInfo> pinInfoList = new ArrayList<>();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item);
                pinInfoMap.forEach((pinType, infoList) -> infoList.forEach(info -> {
                    adapter.add(info.getTitle());
                    pinInfoList.add(info);
                }));
                popup.setAdapter(adapter);
                popup.setAnchorView(keyTypeView);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    PinInfo pinInfo = pinInfoList.get(position);
                    keyTypeView.setText(pinInfo.getTitle());
                    variable.setKeyPinInfo(pinInfo);
                    pin.setValue(card.getTask(), variable.getValue().copy());
                    popup.dismiss();
                });
                popup.show();
            });
        }

        MaterialButton valueTypeView = getValueTypeView();
        if (valueTypeView != null) {
            valueTypeView.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(getContext());
                List<PinInfo> pinInfoList = new ArrayList<>();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item);
                pinInfoMap.forEach((pinType, infoList) -> infoList.forEach(info -> {
                    adapter.add(info.getTitle());
                    pinInfoList.add(info);
                }));
                popup.setAdapter(adapter);
                popup.setAnchorView(valueTypeView);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    PinInfo pinInfo = pinInfoList.get(position);
                    valueTypeView.setText(pinInfo.getTitle());
                    variable.setValuePinInfo(pinInfo);
                    pin.setValue(card.getTask(), variable.getValue().copy());
                    popup.dismiss();
                });
                popup.show();
            });
        }

        MaterialButton typeView = getTypeView();
        if (typeView != null) {
            typeView.setOnClickListener(v -> {
                ListPopupWindow popup = new ListPopupWindow(getContext());
                String[] array = getContext().getResources().getStringArray(R.array.pin_simple_type);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item, array);
                popup.setAdapter(adapter);
                popup.setAnchorView(typeView);
                popup.setModal(true);
                popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
                popup.setOnItemClickListener((parent, view, position, id) -> {
                    typeView.setIconResource(ICON_ARRAY[position]);
                    variable.setType(Variable.VariableType.values()[position]);
                    pin.setValue(card.getTask(), variable.getValue().copy());
                    refreshPin();
                    popup.dismiss();
                });
                popup.show();
            });
        }

        EditText editText = getTitleEdit();
        editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals(pin.getTitle())) return;
                pin.setTitle(s.toString());
            }
        });

        MaterialButton visibleButton = getVisibleButton();
        visibleButton.setOnClickListener(v -> {
            pin.setHide(!pin.isHide());
            visibleButton.setIconResource(pin.isHide() ? R.drawable.icon_visibility_off : R.drawable.icon_visibility);
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 抑制一下recycleView的滚动，让针脚连线能够生效
        if (getCard() instanceof IDynamicPinCard dynamicPinCard) {
            float x = event.getX();
            float y = event.getY();
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                PointF pointF = DisplayUtil.getLocationRelativeToView(this, getCard());
                if (getCard().getLinkAblePinView((x + pointF.x) * getCard().getScaleX(), (y + pointF.y) * getCard().getScaleY()) == this) {
                    dynamicPinCard.suppressLayout();
                }
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void refreshPin() {
        if (pin.getValue() instanceof PinObject pinObject) {
            variable = new Variable((PinObject) pinObject.copy());
        }

        EditText editText = getTitleEdit();
        if (!editText.hasFocus()) {
            editText.setText(pin.getTitle());
        }

        MaterialButton keyTypeView = getKeyTypeView();
        if (keyTypeView != null) {
            PinInfo keyPinInfo = variable.getKeyPinInfo();
            if (keyPinInfo != null) keyTypeView.setText(keyPinInfo.getTitle());
        }

        MaterialButton typeView = getTypeView();
        if (typeView != null) {
            typeView.setIconResource(ICON_ARRAY[variable.getType().ordinal()]);
        }

        MaterialButton valueTypeView = getValueTypeView();
        if (valueTypeView != null) {
            valueTypeView.setVisibility(variable.getType() == Variable.VariableType.MAP ? View.VISIBLE : View.GONE);
            PinInfo valuePinInfo = variable.getValuePinInfo();
            if (valuePinInfo != null) valueTypeView.setText(valuePinInfo.getTitle());
        }

        MaterialButton visibleButton = getVisibleButton();
        visibleButton.setIconResource(pin.isHide() ? R.drawable.icon_visibility_off : R.drawable.icon_visibility);

        super.refreshPin();
    }

    public abstract MaterialButton getKeyTypeView();

    public abstract MaterialButton getValueTypeView();

    public abstract MaterialButton getTypeView();

    public abstract EditText getTitleEdit();

    public abstract MaterialButton getVisibleButton();
}
