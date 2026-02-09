package top.bogey.touch_tool.ui.custom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;

import com.google.android.material.card.MaterialCardView;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import top.bogey.touch_tool.MainApplication;
import top.bogey.touch_tool.service.MainAccessibilityService;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.ObjectResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatInterface;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindowHelper;

@SuppressLint("ViewConstructor")
public class ReadClipboardFloatView extends FrameLayout implements FloatInterface {

    public static synchronized Object getClipboardData() {
        CompletableFuture<Object> future = new CompletableFuture<>();

        FloatWindowHelper helper = FloatWindow.getHelper(KeepAliveFloatView.class.getName());
        if (helper != null) {
            helper.viewParent.post(() -> new ReadClipboardFloatView(helper.viewParent.getContext(), future::complete).show());
        } else {
            return null;
        }

        try {
            Object object = future.get(3, TimeUnit.SECONDS);
            FloatWindow.dismiss(ReadClipboardFloatView.class.getName());
            return object;
        } catch (ExecutionException | InterruptedException | TimeoutException ignored) {
            FloatWindow.dismiss(ReadClipboardFloatView.class.getName());
        }
        return null;
    }

    private boolean flag = false;

    public ReadClipboardFloatView(@NonNull Context context, ObjectResultCallback callback) {
        super(context);

        MaterialCardView cardView = new MaterialCardView(context);
        float px = DisplayUtil.dp2px(context, 4);
        DisplayUtil.setViewWidth(cardView, (int) px);
        DisplayUtil.setViewHeight(cardView, (int) px);
        cardView.setRadius(px / 2);
        cardView.setStrokeWidth(0);
        cardView.setCardBackgroundColor(DisplayUtil.getAttrColor(context, com.google.android.material.R.attr.colorTertiary));
        addView(cardView);

        setAlpha(0.5f);

        ViewTreeObserver.OnWindowFocusChangeListener listener = new ViewTreeObserver.OnWindowFocusChangeListener() {
            @Override
            public void onWindowFocusChanged(boolean hasFocus) {
                if (hasFocus) {
                    Object result = AppUtil.readFromClipboard(context);
                    getViewTreeObserver().removeOnWindowFocusChangeListener(this);
                    callback.onResult(result);
                }
            }
        };
        getViewTreeObserver().addOnWindowFocusChangeListener(listener);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (!flag) {
            flag = true;
            FloatWindow.getHelper(ReadClipboardFloatView.class.getName()).setFocusable(true, false);
        }
    }

    @Override
    public void show() {
        MainAccessibilityService service = MainApplication.getInstance().getService();

        FloatWindow.with(service)
                .setLayout(this)
                .setTag(ReadClipboardFloatView.class.getName())
                .setSpecial(true)
                .setDragAble(false)
                .setAnimator(null)
                .setLocation(EAnchor.TOP_CENTER, 0, (int) DisplayUtil.dp2px(getContext(), 2))
                .show();
    }

    @Override
    public void dismiss() {
        FloatWindow.dismiss(ReadClipboardFloatView.class.getName());
    }
}
