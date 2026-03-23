package top.bogey.touch_tool.ui.custom.float_view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import java.util.List;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.databinding.FloatChoiceListBinding;
import top.bogey.touch_tool.databinding.FloatChoiceListItemBinding;
import top.bogey.touch_tool.ui.custom.float_view.ChoiceExecuteFloatView.Choice;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.StringResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class ListChoiceFloatView extends FrameLayout implements FloatInterface {
    private final FloatChoiceListBinding binding;
    private StringResultCallback callback;

    public static void showChoice(String title, List<Choice> choices, StringResultCallback callback) {
        showChoice(title, choices, callback, EAnchor.CENTER, EAnchor.CENTER, SettingSaver.getInstance().getManualChoiceViewPos());
    }

    public static void showChoice(String title, List<Choice> choices, StringResultCallback callback, EAnchor anchor, EAnchor gravity, Point location) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            ListChoiceFloatView choiceView = new ListChoiceFloatView(keepView.getThemeContext());
            choiceView.show();
            choiceView.innerShowChoice(title, choices, callback, anchor, gravity, location);
        });
    }

    public ListChoiceFloatView(@NonNull Context context) {
        super(context);
        binding = FloatChoiceListBinding.inflate(LayoutInflater.from(context), this, true);

        binding.closeButton.setOnClickListener(v -> {
            if (callback != null) callback.onResult(null);
            dismiss();
        });
    }

    public void innerShowChoice(String title, List<Choice> choices, StringResultCallback callback, EAnchor anchor, EAnchor gravity, Point location) {
        FloatWindow.setLocation(ListChoiceFloatView.class.getName(), anchor, gravity, location);
        binding.title.setText(title);

        this.callback = callback;
        for (Choice choice : choices) {
            FloatChoiceListItemBinding itemBinding = FloatChoiceListItemBinding.inflate(LayoutInflater.from(getContext()), binding.flexBox, true);
            itemBinding.icon.setImageResource(AppUtil.isStringContains(choice.title(), "^[a-z]{1,10}://") ? R.drawable.icon_link : R.drawable.icon_text_fields);
            itemBinding.titleText.setText(choice.title());
            itemBinding.getRoot().setOnClickListener(v -> {
                if (callback != null) callback.onResult(choice.id());
                dismiss();
            });
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        float x = event.getRawX();
        float y = event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int[] location = new int[2];
            binding.scrollView.getLocationOnScreen(location);
            if (new RectF(location[0], location[1], location[0] + binding.scrollView.getWidth(), location[1] + binding.scrollView.getHeight()).contains(x, y)) {
                FloatWindow.setDragAble(ListChoiceFloatView.class.getName(), false);
                return super.onInterceptTouchEvent(event);
            }
            FloatWindow.setDragAble(ListChoiceFloatView.class.getName(), true);
        }
        return super.onInterceptTouchEvent(event);
    }

    @Override
    public void show() {
        Point point = SettingSaver.getInstance().getManualChoiceViewPos();
        FloatWindow.with(MainApplication.getInstance().getService())
                .setLayout(this)
                .setTag(ListChoiceFloatView.class.getName())
                .setSpecial(true)
                .setLocation(EAnchor.CENTER, point.x, point.y)
                .setCallback(new ActionFloatViewCallback(ListChoiceFloatView.class.getName()))
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(ListChoiceFloatView.class.getName());
    }

}
