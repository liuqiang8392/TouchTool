package top.bogey.touch_tool.bean.pin.pin_objects.pin_scale_able;

import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Point;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import top.bogey.touch_tool.bean.pin.pin_objects.PinBase;
import top.bogey.touch_tool.bean.pin.pin_objects.PinType;
import top.bogey.touch_tool.utils.EAnchor;
import top.bogey.touch_tool.utils.GsonUtil;

public class PinTouchPath extends PinScaleAble<String> {
    private transient List<PathPart> pathParts = new ArrayList<>();

    public PinTouchPath() {
        super(PinType.TOUCH);
    }

    public PinTouchPath(List<PathPart> paths) {
        super(PinType.TOUCH);
        pathParts = paths;
        value = serialize(pathParts);
    }
    public PinTouchPath(JsonObject jsonObject) {
        super(jsonObject);
        value = GsonUtil.getAsString(jsonObject, "value", null);
        pathParts = deserialize(value);
    }

    // 返回手势路径，这个手势是平滑的，不会出现突然跳变的情况
    public Set<GestureDescription.StrokeDescription> getStrokes(float timeScale, int offset) {
        List<PathPart> paths = getPathParts(EAnchor.TOP_LEFT);

        int offsetX = (int) (Math.random() * 2 * offset - offset);
        int offsetY = (int) (Math.random() * 2 * offset - offset);

        List<Path> pathList = new ArrayList<>();
        List<Integer> times = new ArrayList<>();
        Map<Integer, Path> prePathMap = new HashMap<>();
        Map<Integer, Integer> preTimeMap = new HashMap<>();

        for (PathPart pathPart : paths) {
            for (PathPoint point : pathPart.getPoints()) {
                int x = point.x + offsetX;
                int y = point.y + offsetY;

                Path path = prePathMap.get(point.getId());
                int time = preTimeMap.computeIfAbsent(point.getId(), k -> 0);
                if (path == null) {
                    path = new Path();
                    path.moveTo(x, y);
                    prePathMap.put(point.getId(), path);
                } else {
                    path.lineTo(x, y);
                    preTimeMap.put(point.getId(), time + pathPart.getTime());
                }

                if (point.isEnd()) {
                    pathList.add(path);
                    times.add(time);
                    prePathMap.remove(point.getId());
                    preTimeMap.remove(point.getId());
                }
            }
        }

        Set<GestureDescription.StrokeDescription> strokes = new HashSet<>();
        for (int i = 0; i < pathList.size(); i++) {
            Path path = pathList.get(i);
            int time = Math.max(1, (int) (times.get(i) / timeScale));
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, time);
            strokes.add(stroke);
        }

        return strokes;
    }

    // 返回手势路径，这个手势是不平滑的，会出现突然跳变的情况，能更好的模拟真人手势
    @RequiresApi(api = Build.VERSION_CODES.O)
    public List<Set<GestureDescription.StrokeDescription>> getStrokesList(float timeScale, int offset) {
        List<PathPart> paths = getPathParts(EAnchor.TOP_LEFT);

        List<Set<GestureDescription.StrokeDescription>> strokes = new ArrayList<>();
        Map<Integer, Point> prePointMap = new HashMap<>();
        Map<Integer, GestureDescription.StrokeDescription> preStrokeMap = new HashMap<>();

        int offsetX = (int) (Math.random() * 2 * offset - offset);
        int offsetY = (int) (Math.random() * 2 * offset - offset);

        for (int i = 0; i < paths.size(); i++) {
            boolean end = paths.size() - 1 == i;
            PathPart pathPart = paths.get(i);

            Set<GestureDescription.StrokeDescription> strokeSet = new HashSet<>();

            int lastX, lastY;
            for (PathPoint point : pathPart.getPoints()) {
                int x = point.x + offsetX;
                int y = point.y + offsetY;
                boolean willContinue = !point.isEnd() && !end;

                Point prePoint = prePointMap.get(point.getId());
                Path path = new Path();
                if (prePoint == null) {
                    path.moveTo(x, y);
                    lastX = x;
                    lastY = y;
                } else {
                    path.moveTo(prePoint.x, prePoint.y);
                    lastX = prePoint.x;
                    lastY = prePoint.y;
                }
                // 起始位置和结束位置不能一样，否则手势不生效
                if (lastX == x && lastY == y) x++;
                path.lineTo(x, y);

                int time = Math.max(1, (int) (pathPart.getTime() / timeScale));
                GestureDescription.StrokeDescription stroke = preStrokeMap.get(point.getId());
                if (stroke == null) {
                    stroke = new GestureDescription.StrokeDescription(path, 0, time, willContinue);
                } else {
                    stroke = stroke.continueStroke(path, 0, time, willContinue);
                }

                preStrokeMap.put(point.getId(), stroke);
                prePointMap.put(point.getId(), new Point(x, y));

                strokeSet.add(stroke);

                if (point.isEnd()) {
                    preStrokeMap.remove(point.getId());
                    prePointMap.remove(point.getId());
                }
            }

            if (!strokeSet.isEmpty()) strokes.add(strokeSet);
        }
        return strokes;
    }

    @Override
    public void reset() {
        super.reset();
        value = null;
        pathParts.clear();
    }

    @Override
    public void sync(PinBase value) {
        super.sync(value);
        pathParts = deserialize(this.value);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString() + "[" + value + "]";
    }

    @Override
    public String getValue(EAnchor anchor) {
        List<PathPart> pathParts = getPathParts(anchor);
        return serialize(pathParts);
    }

    @Override
    public void setValue(String value) {
        setValue(EAnchor.TOP_LEFT, value);
    }

    @Override
    public void setValue(EAnchor anchor, String value) {
        setPathParts(anchor, deserialize(value));
    }

    public List<PathPart> getPathParts() {
        Point anchorPoint = anchor.getAnchorPoint();
        List<PathPart> pathParts = new ArrayList<>();
        float scale = getScale();
        this.pathParts.forEach(part -> {
            PathPart newPart = new PathPart(part);
            if (scale != 1) newPart.scale(scale);
            newPart.offset(anchorPoint.x, anchorPoint.y);
            pathParts.add(newPart);
        });
        return pathParts;
    }

    public void setPathParts(List<PathPart> pathParts) {
        setPathParts(EAnchor.TOP_LEFT, pathParts);
    }

    public List<PathPart> getPathParts(EAnchor anchor) {
        List<PathPart> pathParts = getPathParts();
        if (anchor == this.anchor) return pathParts;
        Point thisAnchorPoint = this.anchor.getAnchorPoint();
        pathParts.forEach(part -> part.offset(-thisAnchorPoint.x, -thisAnchorPoint.y));
        Point thatAnchorPoint = anchor.getAnchorPoint();
        pathParts.forEach(part -> part.offset(thatAnchorPoint.x, thatAnchorPoint.y));
        return pathParts;
    }

    public void setPathParts(EAnchor anchor, List<PathPart> pathParts) {
        setAnchor(anchor);
        if (pathParts == null || pathParts.isEmpty()) {
            reset();
            return;
        }
        Point anchorPoint = anchor.getAnchorPoint();
        pathParts.forEach(part -> part.offset(-anchorPoint.x, -anchorPoint.y));
        this.pathParts = pathParts;
        value = serialize(pathParts);
    }

    private static String serialize(List<PathPart> pathParts) {
        if (pathParts == null || pathParts.isEmpty()) return "";
        StringBuilder builder = new StringBuilder();
        for (PathPart part : pathParts) {
            builder.append(part.toString()).append("|");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    private static List<PathPart> deserialize(String info) {
        List<PathPart> list = new ArrayList<>();
        if (info == null || info.isEmpty()) return list;
        String[] split = info.split("\\|");
        for (String s : split) {
            list.add(new PathPart(s));
        }
        return list;
    }

    // 路径片段，格式为 time;[id.x.y,id.x.y,...]
    public static class PathPart {
        private int time;
        private final Set<PathPoint> points = new HashSet<>();

        public PathPart(int time) {
            this.time = time;
        }

        public PathPart(int time, int x, int y) {
            this(time);
            points.add(new PathPoint(0, x, y));
        }

        public PathPart(PathPart part) {
            this.time = part.time;
            for (PathPoint point : part.points) {
                points.add(new PathPoint(point));
            }
        }

        public PathPart(String info) {
            String[] split = info.split(";");
            time = Integer.parseInt(split[0]);
            String substring = split[1].substring(1, split[1].length() - 1);
            for (String s : substring.split(",")) {
                points.add(new PathPoint(s));
            }
        }

        public void addPoint(PathPoint point) {
            points.add(point);
        }

        public void addPoint(int id, int x, int y) {
            points.add(new PathPoint(id, x, y));
        }

        public void removePoint(int id) {
            points.remove(getPoint(id));
        }

        public PathPoint getPoint(int id) {
            for (PathPoint point : points) {
                if (point.getId() == id) {
                    return point;
                }
            }
            return null;
        }

        public boolean existEndPoint() {
            for (PathPoint point : points) {
                if (point.isEnd()) {
                    return true;
                }
            }
            return false;
        }

        public boolean isEmpty() {
            return points.isEmpty();
        }

        public void scale(float scale) {
            for (PathPoint point : points) {
                point.scale(scale);
            }
        }

        public void offset(int dx, int dy) {
            for (PathPoint point : points) {
                point.offset(dx, dy);
            }
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append(time).append(";");
            builder.append("[");
            for (PathPoint point : points) {
                builder.append(point.toString()).append(",");
            }
            builder.deleteCharAt(builder.length() - 1);
            builder.append("]");
            return builder.toString();
        }

        @Override
        public final boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof PathPart pathPart)) return false;

            return getTime() == pathPart.getTime() && getPoints().equals(pathPart.getPoints());
        }

        @Override
        public int hashCode() {
            int result = getTime();
            result = 31 * result + getPoints().hashCode();
            return result;
        }

        public int getTime() {
            return time;
        }

        public void setTime(int time) {
            this.time = time;
        }

        public Set<PathPoint> getPoints() {
            return points;
        }
    }

    // 路径点 格式为 id.x.y
    public static class PathPoint extends Point {
        private int id;
        private boolean end;

        public PathPoint(int id, int x, int y) {
            super(x, y);
            this.id = id;
        }

        public PathPoint(PathPoint point) {
            super(point.x, point.y);
            this.id = point.id;
            this.end = point.end;
        }

        public PathPoint(String info) {
            String[] strings = info.split("\\.");
            id = Integer.parseInt(strings[0]);
            x = Integer.parseInt(strings[1]);
            y = Integer.parseInt(strings[2]);
            end = strings.length == 4;
        }

        public void scale(float scale) {
            x = (int) (x * scale);
            y = (int) (y * scale);
        }

        @NonNull
        @Override
        public String toString() {
            if (end) return id + "." + x + "." + y + "." + 1;
            return id + "." + x + "." + y;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public boolean isEnd() {
            return end;
        }

        public void setEnd(boolean end) {
            this.end = end;
        }

        @Override
        public final boolean equals(Object object) {
            if (this == object) return true;
            if (!(object instanceof PathPoint pathPoint)) return false;
            if (!super.equals(object)) return false;

            return getId() == pathPoint.getId() && isEnd() == pathPoint.isEnd();
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + getId();
            result = 31 * result + Boolean.hashCode(isEnd());
            return result;
        }
    }
}
