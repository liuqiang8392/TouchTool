package top.bogey.touch_tool.ui.blueprint.pin_widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Editable;
import android.view.LayoutInflater;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.ListPopupWindow;

import java.util.ArrayList;
import java.util.Arrays;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_string.PinSingleSelect;
import top.bogey.touch_tool.databinding.PinWidgetSelectBinding;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.card.InputConfigActionFloatCard;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.listener.TextChangedListener;

@SuppressLint("ViewConstructor")
public class PinWidgetSelect extends PinWidget<PinSingleSelect> {
    private final PinWidgetSelectBinding binding;
    private ArrayAdapter<String> adapter;

    public PinWidgetSelect(@NonNull Context context, ActionCard card, PinView pinView, PinSingleSelect pinBase, boolean custom) {
        super(context, card, pinView, pinBase, custom);
        binding = PinWidgetSelectBinding.inflate(LayoutInflater.from(context), this, true);
        init();
    }

    @Override
    protected void initBase() {
        adapter = new ArrayAdapter<>(getContext(), R.layout.widget_textview_item, pinBase.getOptions());
        binding.spinner.setOnClickListener(v -> {
            ListPopupWindow popup = new ListPopupWindow(getContext());
            popup.setAdapter(adapter);
            popup.setAnchorView(binding.spinner);
            popup.setModal(true);
            popup.setWidth(DisplayUtil.measureArrayAdapterContentWidth(getContext(), adapter));
            popup.setOnItemClickListener((parent, view, position, id) -> {
                pinBase.setValue(adapter.getItem(position));
                binding.spinner.setText(pinBase.getValue());
                pinView.getPin().notifyValueUpdated(card.getTask());
                popup.dismiss();
            });
            popup.show();

            if (card instanceof InputConfigActionFloatCard) {
                post(() -> DisplayUtil.setListPopupWindowAutoHeight(popup));
            }
        });
        binding.spinner.setText(pinBase.getValue());
    }

    @Override
    protected void initCustom() {
        binding.editTextBox.setVisibility(VISIBLE);
        binding.editText.setText(pinBase.getOptionsString());
        binding.editText.addTextChangedListener(new TextChangedListener() {
            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.length() == 0) {
                    pinBase.setOptions(new ArrayList<>());
                    pinBase.setValue(null);
                } else {
                    String value = s.toString();
                    String[] split = value.split("[,，]");
                    pinBase.setOptions(new ArrayList<>(Arrays.asList(split)));
                }
                adapter.clear();
                adapter.addAll(pinBase.getOptions());
                binding.spinner.setText(pinBase.getValue());
            }
        });
    }
}
