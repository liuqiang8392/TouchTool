package top.bogey.touch_tool.ui.setting.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.slider.Slider;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.databinding.WidgetSettingSliderBinding;

public class SettingWidgetSlider extends FrameLayout {
    private final WidgetSettingSliderBinding binding;

    public SettingWidgetSlider(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        binding = WidgetSettingSliderBinding.inflate(LayoutInflater.from(context), this, true);

        try (TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SettingWidgetSlider)) {
            int icon = typedArray.getResourceId(R.styleable.SettingWidgetSlider_icon, 0);
            binding.icon.setImageResource(icon);

            String title = typedArray.getString(R.styleable.SettingWidgetSlider_title);
            binding.title.setText(title);

            String description = typedArray.getString(R.styleable.SettingWidgetSlider_description);
            binding.description.setText(description);
            binding.description.setVisibility(description == null || description.isEmpty() ? GONE : VISIBLE);

            int min = typedArray.getInt(R.styleable.SettingWidgetSlider_min, 0);
            int max = typedArray.getInt(R.styleable.SettingWidgetSlider_max, 100);
            int value = typedArray.getInt(R.styleable.SettingWidgetSlider_value, 50);
            int step = typedArray.getInt(R.styleable.SettingWidgetSlider_step, 1);
            binding.slider.setStepSize(step);
            binding.slider.setValueTo(max);
            binding.slider.setValueFrom(min);
            binding.slider.setValue(value);
        }
    }

    public void setSliderOnChangeListener(Slider.OnChangeListener listener) {
        binding.slider.addOnChangeListener(listener);
    }

    public void setValue(float value) {
        value = Math.clamp(value, binding.slider.getValueFrom(), binding.slider.getValueTo());
        binding.slider.setValue(value);
    }

    public void setValueFormat(String format) {
        binding.slider.setLabelFormatter(value -> String.format(format, (int) value));
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        binding.getRoot().setOnClickListener(listener);
    }

    public void setDescription(String description) {
        binding.description.setText(description);
    }
}
