package top.bogey.touch_tool.ui.blueprint.picker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able.PinTouchPath;
import top.bogey.touch_tool.databinding.FloatPickerTouchBinding;
import top.bogey.touch_tool.utils.DisplayUtil;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.callback.ResultCallback;

@SuppressLint("ViewConstructor")
public class TouchPicker extends FullScreenPicker<PinTouchPath> {
    private final static int MODE_NONE = 0;
    private final static int MODE_MARKING = 1;
    private final static int MODE_MARKED = 2;
    private final static int MODE_MOVE = 3;
    private final static int MODE_LOW_MOVE = 4;

    private final FloatPickerTouchBinding binding;
    private final Handler handler;
    private final Paint paint;
    private final int padding;

    private final List<PinTouchPath.PathPart> pathParts = new ArrayList<>();
    private EAnchor anchor;
    private final Map<Integer, Path> pathMap = new HashMap<>();

    private int mode;
    private float lastX, lastY;
    private long lastTime;
    private boolean moved, toLine;

    public TouchPicker(@NonNull Context context, ResultCallback<PinTouchPath> callback, PinTouchPath path) {
        super(context, callback);
        pathParts.addAll(path.getPathParts());
        anchor = path.getAnchor();
        mode = pathParts.isEmpty() ? MODE_NONE : MODE_MARKED;

        binding = FloatPickerTouchBinding.inflate(LayoutInflater.from(context), this, true);

        binding.topLeftButton.setOnClickListener(v -> setAnchor(EAnchor.TOP_LEFT));
        binding.topRightButton.setOnClickListener(v -> setAnchor(EAnchor.TOP_RIGHT));
        binding.bottomLeftButton.setOnClickListener(v -> setAnchor(EAnchor.BOTTOM_LEFT));
        binding.bottomRightButton.setOnClickListener(v -> setAnchor(EAnchor.BOTTOM_RIGHT));
        setAnchor(anchor);

        binding.backButton.setOnClickListener(v -> dismiss());

        binding.saveButton.setOnClickListener(v -> {
            pathParts.forEach(pathPart -> pathPart.offset(location[0], location[1]));
            PinTouchPath touchPath = new PinTouchPath(pathParts);
            touchPath.setAnchor(anchor);
            callback.onResult(touchPath);
            dismiss();
        });

        handler = new Handler();

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(DisplayUtil.getAttrColor(getContext(), R.attr.colorPrimaryLight));
        paint.setStrokeWidth(10);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);

        padding = Math.round(DisplayUtil.dp2px(context, 20));
    }

    @Override
    protected void realShow() {
        pathParts.forEach(pathPart -> pathPart.offset(-location[0], -location[1]));
        pathMap.putAll(getPathMap());
        refreshUI();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            moved = false;
            if (mode == MODE_MARKED) {
                float boxX = binding.markBox.getX();
                float boxY = binding.markBox.getY();
                int width = binding.markBox.getWidth();
                int height = binding.markBox.getHeight();
                if ((new RectF(boxX, boxY, boxX + width, boxY + height)).contains(x, y)) {
                    mode = MODE_MOVE;
                } else {
                    mode = MODE_LOW_MOVE;
                }
            } else {
                lastTime = System.currentTimeMillis();
                addPathPart(event, -1);
                mode = MODE_MARKING;
            }
            lastX = x;
            lastY = y;
        }

        if (action == MotionEvent.ACTION_MOVE) {
            float dx = x - lastX;
            float dy = y - lastY;
            if (dx == 0 && dy == 0) return true;
            moved = true;
            if (mode == MODE_MARKING) {
                addPathPart(event, -1);
            }

            if (mode == MODE_MOVE) {
                pathParts.forEach(pathPart -> pathPart.offset((int) dx, (int) dy));
                pathMap.values().forEach(path -> path.offset((int) dx, (int) dy));
            }

            if (mode == MODE_LOW_MOVE) {
                pathParts.forEach(pathPart -> pathPart.offset((int) (dx / 5), (int) (dy / 5)));
                pathMap.values().forEach(path -> path.offset((int) (dx / 5), (int) (dy / 5)));
            }
            lastX = x;
            lastY = y;
        }

        if (action == MotionEvent.ACTION_UP) {
            if (mode == MODE_MARKING) {
                addPathPart(event, event.getPointerId(event.getActionIndex()));
                longTouchSupport(null);
                mode = pathParts.isEmpty() ? MODE_NONE : MODE_MARKED;
            }

            if (mode == MODE_MOVE) {
                if (!moved) {
                    if (toLine) {
                        toLine();
                    } else {
                        toLine = true;
                        postDelayed(() -> toLine = false, 300);
                    }
                }
                mode = MODE_MARKED;
            }

            if (mode == MODE_LOW_MOVE) {
                if (!moved) {
                    pathParts.clear();
                    pathMap.clear();
                    mode = MODE_NONE;
                } else {
                    mode = MODE_MARKED;
                }
            }
        }

        if (action == MotionEvent.ACTION_POINTER_DOWN) {
            if (mode == MODE_MARKING) {
                pathParts.clear();
                pathMap.clear();
                lastTime = System.currentTimeMillis();
                addPathPart(event, -1);
            }
        }

        if (action == MotionEvent.ACTION_POINTER_UP) {
            if (mode == MODE_MARKING) {
                addPathPart(event, event.getPointerId(event.getActionIndex()));
            }
        }

        refreshUI();
        return true;
    }

    private void addPathPart(MotionEvent event, int endId) {
        long currTime = System.currentTimeMillis();
        PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart((int) (currTime - lastTime));
        for (int i = 0; i < event.getPointerCount(); i++) {
            int pointerId = event.getPointerId(i);
            float currX = event.getX(i), currY = event.getY(i);
            for (int j = 0; j < event.getHistorySize(); j++) {
                currX = event.getHistoricalX(i, j);
                currY = event.getHistoricalY(i, j);
            }
            PinTouchPath.PathPoint pathPoint = new PinTouchPath.PathPoint(pointerId, (int) currX, (int) currY);
            pathPoint.setEnd(pointerId == endId);
            pathPart.addPoint(pathPoint);

            Path path = pathMap.get(pointerId);
            if (path == null) {
                path = new Path();
                pathMap.put(pointerId, path);
                path.moveTo(currX, currY);
            } else {
                path.lineTo(currX, currY);
            }
        }
        pathParts.add(pathPart);
        longTouchSupport(pathPart);
        lastTime = currTime;
    }

    private void longTouchSupport(PinTouchPath.PathPart part) {
        handler.removeCallbacksAndMessages(null);
        if (part == null) return;
        handler.postDelayed(() -> {
            PinTouchPath.PathPart pathPart = new PinTouchPath.PathPart(part);
            pathPart.setTime(100);
            pathParts.add(pathPart);
            lastTime = System.currentTimeMillis();
            longTouchSupport(pathPart);
        }, 100);
    }

    private void toLine() {
        List<PinTouchPath.PathPart> parts = new ArrayList<>();
        int time = 0;
        for (PinTouchPath.PathPart pathPart : pathParts) {
            if (parts.isEmpty()) parts.add(pathPart);
            else {
                time += pathPart.getTime();
                if (pathPart.existEndPoint()) {
                    PinTouchPath.PathPart part = new PinTouchPath.PathPart(pathPart);
                    part.setTime(time);
                    parts.add(part);
                }
            }
        }
        pathParts.clear();
        pathParts.addAll(parts);
        pathMap.clear();
        pathParts.forEach(pathPart -> {
            for (PinTouchPath.PathPoint point : pathPart.getPoints()) {
                Path path = pathMap.get(point.getId());
                if (path == null) {
                    path = new Path();
                    pathMap.put(point.getId(), path);
                    path.moveTo(point.x, point.y);
                } else {
                    path.lineTo(point.x, point.y);
                }
            }
        });
    }

    private void refreshUI() {
        if (mode == MODE_MARKED) {
            binding.buttonBox.setVisibility(VISIBLE);
            binding.markBox.setVisibility(VISIBLE);

            List<Point> points = new ArrayList<>();
            for (PinTouchPath.PathPart part : pathParts) {
                points.addAll(part.getPoints());
            }
            Rect area = DisplayUtil.getPointsArea(points);

            int width = area.width() + padding * 2;
            int height = area.height() + padding * 2;
            DisplayUtil.setViewWidth(binding.markBox, width);
            DisplayUtil.setViewHeight(binding.markBox, height);
            binding.markBox.setX(area.left - padding);
            binding.markBox.setY(area.top - padding);

            binding.topRightButton.setX(width - binding.topRightButton.getWidth());
            binding.bottomLeftButton.setY(height - binding.bottomLeftButton.getHeight());
            binding.bottomRightButton.setX(width - binding.bottomRightButton.getWidth());
            binding.bottomRightButton.setY(height - binding.bottomRightButton.getHeight());

            float x = area.left + (area.width() - binding.buttonBox.getWidth()) / 2f;
            x = Math.max(0, Math.min(getWidth() - binding.buttonBox.getWidth(), x));
            binding.buttonBox.setX(x);

            int doubleOffset = padding * 2;
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
        } else {
            binding.buttonBox.setVisibility(INVISIBLE);
            binding.markBox.setVisibility(INVISIBLE);
        }
        postInvalidate();
    }

    private void setAnchor(EAnchor anchor) {
        this.anchor = anchor;
        binding.topLeftButton.setIconResource(anchor == EAnchor.TOP_LEFT ? R.drawable.icon_radio_button_checked : R.drawable.icon_radio_button_unchecked);
        binding.topRightButton.setIconResource(anchor == EAnchor.TOP_RIGHT ? R.drawable.icon_radio_button_checked : R.drawable.icon_radio_button_unchecked);
        binding.bottomLeftButton.setIconResource(anchor == EAnchor.BOTTOM_LEFT ? R.drawable.icon_radio_button_checked : R.drawable.icon_radio_button_unchecked);
        binding.bottomRightButton.setIconResource(anchor == EAnchor.BOTTOM_RIGHT ? R.drawable.icon_radio_button_checked : R.drawable.icon_radio_button_unchecked);
    }

    private Map<Integer, Path> getPathMap() {
        Map<Integer, Path> pathMap = new HashMap<>();
        for (PinTouchPath.PathPart part : pathParts) {
            for (PinTouchPath.PathPoint point : part.getPoints()) {
                Path path = pathMap.get(point.getId());
                if (path == null) {
                    path = new Path();
                    pathMap.put(point.getId(), path);
                    path.moveTo(point.x, point.y);
                } else {
                    path.lineTo(point.x, point.y);
                }
            }
        }
        return pathMap;
    }

    @Override
    protected void dispatchDraw(@NonNull Canvas canvas) {
        super.dispatchDraw(canvas);

        for (Path path : pathMap.values()) {
            canvas.drawPath(path, paint);
        }
    }
}
