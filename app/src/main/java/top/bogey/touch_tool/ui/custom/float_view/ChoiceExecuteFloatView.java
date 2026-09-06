package top.bogey.touch_tool.ui.custom.float_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.bean.save.setting.SettingSaver;
import top.bogey.touch_tool.databinding.FloatChoiceExecuteBinding;
import top.bogey.touch_tool.databinding.FloatChoiceExecuteItemBinding;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.StringResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class ChoiceExecuteFloatView extends FrameLayout implements FloatInterface {
    private final FloatChoiceExecuteBinding binding;
    private int timeout;
    private String title;
    private StringResultCallback callback;

    public static void showChoice(String title, List<Choice> choices, int timeout, StringResultCallback callback) {
        showChoice(title, choices, timeout, callback, EAnchor.CENTER, EAnchor.CENTER, SettingSaver.FLOAT_VIEW_POS.get());
    }

    public static void showChoice(String title, List<Choice> choices, int timeout, StringResultCallback callback, EAnchor anchor, EAnchor gravity, Point location) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            ChoiceExecuteFloatView choiceView = new ChoiceExecuteFloatView(keepView.getThemeContext());
            choiceView.show();
            choiceView.innerShowChoice(title, choices, timeout, callback, anchor, gravity, location);
        });
    }

    public ChoiceExecuteFloatView(@NonNull Context context) {
        super(context);
        binding = FloatChoiceExecuteBinding.inflate(LayoutInflater.from(context), this, true);

        binding.closeButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(null);
            dismiss();
        });
    }

    public void innerShowChoice(String title, List<Choice> choices, int timeout, StringResultCallback callback, EAnchor anchor, EAnchor gravity, Point location) {
        FloatWindow.setLocation(ChoiceExecuteFloatView.class.getName(), anchor, gravity, location);
        this.title = title;
        this.timeout = timeout;
        if (timeout > 0) refreshTimeout();
        else binding.title.setText(title);

        this.callback = callback;
        for (Choice choice : choices) {
            FloatChoiceExecuteItemBinding itemBinding = FloatChoiceExecuteItemBinding.inflate(LayoutInflater.from(getContext()), binding.flexBox, true);
            itemBinding.icon.setVisibility(choice.icon() == null ? GONE : VISIBLE);
            itemBinding.icon.setImageBitmap(choice.icon());
            itemBinding.titleText.setText(choice.title());
            itemBinding.getRoot().setOnClickListener(v -> {
                if (callback != null) callback.onResult(choice.id());
                dismiss();
            });
        }
    }

    @SuppressLint("DefaultLocale")
    public void refreshTimeout() {
        if (timeout <= 0) {
            callback.onResult(null);
            dismiss();
            return;
        }
        timeout -= 100;
        binding.title.setText(String.format("%s(%.1fs)", title, timeout / 1000f));
        postDelayed(this::refreshTimeout, 100);
    }

    @Override
    public void show() {
        Point point = SettingSaver.FLOAT_VIEW_POS.get();
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(ChoiceExecuteFloatView.class.getName())
                .setSpecial(true)
                .setLocation(EAnchor.CENTER, point.x, point.y)
                .setCallback(new ActionFloatViewCallback(ChoiceExecuteFloatView.class.getName()))
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(ChoiceExecuteFloatView.class.getName());
    }

    public record Choice(String id, String title, Bitmap icon) {
    }
}
