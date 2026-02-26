package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.other.NodeInfo;
import top.bogey.touch_tool.databinding.FloatPickerAreaBinding;
import top.bogey.touch_tool.ui.custom.KeepAliveFloatView;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.callback.ResultCallback;
import top.bogey.touch_tool.utils.float_window_manager.FloatWindow;

@SuppressLint("ViewConstructor")
public class AreaPicker extends FullScreenPicker<Rect> {
    private final static int MODE_NONE = 0;
    private final static int MODE_BR = 1;
    private final static int MODE_TL = 2;
    private final static int MODE_MOVE = 3;
    private final static int MODE_LOW_BR = 4;
    private final static int MODE_LOW_TL = 5;

    private final FloatPickerAreaBinding binding;
    private final Rect area = new Rect();
    private final float offset;
    private final Paint bitmapPaint;
    private final Paint markPaint;
    private final Paint markLinePaint;

    private float lastX, lastY;
    private int mode = MODE_NONE;
    private int moveMode = MODE_NONE;

    public static void showPicker(ResultCallback<Rect> callback) {
        KeepAliveFloatView keepView = (KeepAliveFloatView) FloatWindow.getView(KeepAliveFloatView.class.getName());
        if (keepView == null) return;
        new Handler(Looper.getMainLooper()).post(() -> {
            AreaPicker areaPicker = new AreaPicker(keepView.getThemeContext(), callback, DisplayUtil.getScreenArea(keepView.getContext()));
            areaPicker.setFloatCallback(new AreaPickerCallback(areaPicker));
            areaPicker.show();
        });
    }

    public AreaPicker(@NonNull Context context, ResultCallback<Rect> callback, Rect rect) {
        super(context, callback);
        binding = FloatPickerAreaBinding.inflate(LayoutInflater.from(context), this, true);
        area.set(rect);
        if (rect.equals(DisplayUtil.getScreenArea(context))) {
            area.set(new Rect());
        }
        offset = DisplayUtil.dp2px(context, 4);

        markPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markPaint.setStyle(Paint.Style.FILL);
        markPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));

        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setFilterBitmap(true);
        bitmapPaint.setDither(true);

        markLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        markLinePaint.setStyle(Paint.Style.STROKE);
        markLinePaint.setStrokeWidth(1);
        markLinePaint.setColor(DisplayUtil.getAttrColor(context, R.attr.colorPrimaryLight));

        binding.backButton.setOnClickListener(v -> {
            callback.onResult(null);
            dismiss();
        });

        binding.saveButton.setOnClickListener(v -> {
            area.offset(location[0], location[1]);
            if (area.isEmpty()) area.set(rect);
            callback.onResult(area);
            dismiss();
        });

        binding.fullButton.setOnClickListener(v -> {
            if (area.isEmpty()) area.set(0, 0, getWidth(), getHeight());
            else area.set(0, 0, 0, 0);
            refreshUI();
        });
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int action = event.getAction();
        if (action == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;
            mode = MODE_NONE;
            moveMode = MODE_NONE;

            if (area.isEmpty()) {
                area.set((int) x, (int) y, (int) x, (int) y);
            } else {
                View[] views = new View[]{binding.moveRight, binding.moveLeft, binding.markBox, binding.bottomArea, binding.topArea};
                for (int i = 0; i < views.length; i++) {
                    View view = views[i];
                    if (view.getVisibility() != VISIBLE) continue;
                    PointF pos = DisplayUtil.getLocationRelativeToView(view, this);
                    RectF rect = new RectF(pos.x, pos.y, pos.x + view.getWidth(), pos.y + view.getHeight());
                    if (rect.contains(x, y)) {
                        moveMode = i + 1;
                        break;
                    }
                }
            }
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX;
            float dy = y - lastY;
            if (dx == 0 && dy == 0) return true;
            mode = moveMode;
            boolean flag = true;
            if (mode == MODE_NONE) {
                mode = MODE_BR;
            }
            if (mode == MODE_BR) {
                area.right = (int) x;
                area.bottom = (int) y;
            }
            if (mode == MODE_TL) {
                area.left = (int) x;
                area.top = (int) y;
            }
            if (mode == MODE_MOVE) {
                area.offset((int) dx, (int) dy);
            }

            int xOffset = (int) dx / 5;
            int yOffset = (int) dy / 5;
            if (mode == MODE_LOW_BR) {
                flag = xOffset + yOffset != 0;
                if (flag) {
                    area.right += xOffset;
                    area.bottom += yOffset;
                }
            }
            if (mode == MODE_LOW_TL) {
                flag = xOffset + yOffset != 0;
                if (flag) {
                    area.offset(xOffset, yOffset);
                }
            }
            if (flag) {
                area.sort();
                lastX = x;
                lastY = y;
            }
        }

        if (action == MotionEvent.ACTION_UP) {
            if (mode == MODE_NONE) {
                if (area.isEmpty()) {
                    for (int i = screenInfo.getRootNodes().size() - 1; i >= 0; i--) {
                        NodeInfo nodeInfo = screenInfo.getRootNodes().get(i);
                        NodeInfo child = nodeInfo.findUsableChild((int) x, (int) y);
                        if (child != null) {
                            area.set(child.area);
                            break;
                        }
                    }
                } else {
                    area.setEmpty();
                }
            }
            mode = MODE_NONE;
        }

        refreshUI();
        return true;
    }

    @Override
    protected void realShow() {
        area.offset(-location[0], -location[1]);
        refreshUI();
    }

    private void refreshUI() {
        area.sort();
        area.left = Math.max(0, area.left);
        area.top = Math.max(0, area.top);
        area.right = Math.min(getWidth(), area.right);
        area.bottom = Math.min(getHeight(), area.bottom);

        if (area.isEmpty()) {
            binding.fullButton.setIconResource(R.drawable.icon_zoom_out_map);
            binding.markBox.setVisibility(GONE);
            binding.moveLeft.setVisibility(GONE);
            binding.moveRight.setVisibility(GONE);
            binding.buttonBox.setX(getWidth() / 2f - binding.buttonBox.getWidth() / 2f);
            binding.buttonBox.setY(getHeight() - binding.buttonBox.getHeight() - DisplayUtil.dp2px(getContext(), 64));
        } else {
            binding.fullButton.setIconResource(R.drawable.icon_zoom_in_map);
            int doubleOffset = (int) (offset * 2);
            binding.markBox.setVisibility(VISIBLE);
            binding.moveLeft.setVisibility(VISIBLE);
            binding.moveRight.setVisibility(VISIBLE);
            DisplayUtil.setViewWidth(binding.markBox, area.width() + doubleOffset);
            DisplayUtil.setViewHeight(binding.markBox, area.height() + doubleOffset);
            binding.markBox.setX(area.left - offset);
            binding.markBox.setY(area.top - offset);

            float x = area.left + (area.width() - binding.buttonBox.getWidth()) / 2f;
            x = Math.max(0, Math.min(getWidth() - binding.buttonBox.getWidth(), x));
            binding.buttonBox.setX(x);

            if (getHeight() < area.height() + binding.buttonBox.getHeight() + doubleOffset) {
                // 内部
                binding.buttonBox.setY(area.bottom - binding.buttonBox.getHeight() - doubleOffset);
            } else if (getHeight() < area.bottom + binding.buttonBox.getHeight() + doubleOffset) {
                // 外部 顶上
                binding.buttonBox.setY(area.top - binding.buttonBox.getHeight() - doubleOffset);
            } else {
                // 外部 底下
                binding.buttonBox.setY(area.bottom + doubleOffset);
            }
        }
        invalidate();
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        if (mode == MODE_LOW_BR || mode == MODE_LOW_TL) {
            float wScale = getWidth() * 1f / area.width();
            float hScale = getHeight() * 1f / area.height();
            float scale = Math.min(5, Math.min(wScale, hScale) * 2 / 3);
            if (scale > 1) {
                float x = getWidth() / 2f - area.width() * scale / 2 - area.left * scale;
                float y = getHeight() / 2f - area.height() * scale / 2 - area.top * scale;
                canvas.translate(x, y);
                canvas.scale(scale, scale);
            }
        }

        Bitmap screenShot = screenInfo.getScreenShot();
        if (screenShot != null) canvas.drawBitmap(screenShot, 0, 0, bitmapPaint);

        canvas.saveLayer(0, 0, getWidth(), getHeight(), bitmapPaint);
        super.dispatchDraw(canvas);
        canvas.drawRect(area, markPaint);
        canvas.restore();
        canvas.drawRect(area, markLinePaint);

        drawChild(canvas, binding.buttonBox, getDrawingTime());
    }

    private static class AreaPickerCallback extends FullScreenPickerCallback {

        public AreaPickerCallback(FullScreenPicker<?> picker) {
            super(picker);
        }

        @Override
        public void onDismiss() {

        }
    }
}
