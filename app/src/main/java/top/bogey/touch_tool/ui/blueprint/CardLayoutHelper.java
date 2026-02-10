package top.bogey.touch_tool.ui.blueprint;

import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.RectF;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.bean.pin.pin_objects.pin_execute.PinExecute;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;

public class CardLayoutHelper {
    public static final int CORNER_GRID_COUNT = 2;

    public static Path calculateLinkPath(float gridSize, PointF start, PointF end, boolean vertical) {
        float dx = Math.abs(start.x - end.x);
        float dy = Math.abs(start.y - end.y);
        float minOffset = gridSize * CORNER_GRID_COUNT;
        if (dx <= minOffset && dy <= minOffset) {
            Path path = new Path();
            path.moveTo(start.x, start.y);
            path.lineTo(end.x, end.y);
            return path;
        }

        List<PointF> points = new ArrayList<>();
        points.add(start);

        PointF startPoint = new PointF(start.x, start.y);
        PointF endPoint = new PointF(end.x, end.y);
        if (vertical) {
            startPoint.y += gridSize * CORNER_GRID_COUNT;
            endPoint.y -= gridSize * CORNER_GRID_COUNT;
        } else {
            startPoint.x += gridSize * CORNER_GRID_COUNT;
            endPoint.x -= gridSize * CORNER_GRID_COUNT;
        }

        points.add(startPoint);

        boolean isXPositive = startPoint.x < endPoint.x;
        boolean isYPositive = startPoint.y < endPoint.y;
        int xScale = isXPositive ? 1 : -1;
        int yScale = isYPositive ? 1 : -1;

        dx = Math.round(Math.abs(endPoint.x - startPoint.x) * 100) / 100f;
        dy = Math.round(Math.abs(endPoint.y - startPoint.y) * 100) / 100f;
        boolean xLong = dx > dy;
        float halfLen = Math.abs(dx - dy) / 2;

        /*
        vertical:
            xLong:
                isYPositive: ↓ ← ↓, ↓ → ↓
                ! isYPositive: ← ↖ ←, → ↗ →
            ! xLong:
                isYPositive: ↓ ↙ ↓, ↓ ↘ ↓
                ! isYPositive: ← ↑ ←, → ↑ →
            dx < gridSize / 2:
                isYPositive: ↓
                ! isYPositive: ← ↑ →
        ! vertical:
            xLong:
                isXPositive:  → ↗ →, → ↘ →
                ! isXPositive: ↑ ← ↑, ↓ ← ↓
            ! xLong:
                isXPositive:  → ↑ →, → ↓ →
                ! isXPositive: ↑ ↖ ↑, ↓ ↙ ↓
            dy < gridSize / 2:
                isXPositive:  →
                ! isXPositive: ↓ ← ↑
        */

        boolean flag = true;
        float gridOffset = gridSize * (2 + CORNER_GRID_COUNT);
        if (vertical) {
            if (!isYPositive) {
                if (dx < gridOffset && dy > gridOffset) { //← ↑ →
                    float x = Math.min(endPoint.x, startPoint.x) - gridOffset;
                    points.add(new PointF(x, startPoint.y));
                    points.add(new PointF(x, endPoint.y));
                    flag = false;
                }
            }

            if (flag) {
                if (xLong) {
                    if (isYPositive) {  //↓ ← ↓, ↓ → ↓
                        points.add(new PointF(startPoint.x, startPoint.y + dy / 2));
                        points.add(new PointF(endPoint.x, endPoint.y - dy / 2));
                    } else {            //← ↖ ←, → ↗ →
                        points.add(new PointF(startPoint.x + halfLen * xScale, startPoint.y));
                        points.add(new PointF(endPoint.x - halfLen * xScale, endPoint.y));
                    }
                } else {
                    if (isYPositive) {  //↓ ↙ ↓, ↓ ↘ ↓
                        points.add(new PointF(startPoint.x, startPoint.y + halfLen));
                        points.add(new PointF(endPoint.x, endPoint.y - halfLen));
                    } else {            //← ↑ ←, → ↑ →
                        points.add(new PointF(startPoint.x + dx * xScale / 2, startPoint.y));
                        points.add(new PointF(endPoint.x - dx * xScale / 2, endPoint.y));
                    }
                }
            }
        } else {
            if (!isXPositive) {
                if (dy < gridOffset && dx > gridOffset) { //↓ ← ↑
                    float y = Math.max(endPoint.y, startPoint.y) + gridSize * (CORNER_GRID_COUNT + 4);
                    points.add(new PointF(startPoint.x, y));
                    points.add(new PointF(endPoint.x, y));
                    flag = false;
                }
            }

            if (flag) {
                if (xLong) {
                    if (isXPositive) {  //→ ↗ →, → ↘ →
                        points.add(new PointF(startPoint.x + halfLen, startPoint.y));
                        points.add(new PointF(endPoint.x - halfLen, endPoint.y));
                    } else {            //↑ ← ↑, ↓ ← ↓
                        points.add(new PointF(startPoint.x, startPoint.y + dy * yScale / 2));
                        points.add(new PointF(endPoint.x, endPoint.y - dy * yScale / 2));
                    }
                } else {
                    if (isXPositive) {  //→ ↑ →, → ↓ →
                        points.add(new PointF(startPoint.x + dx / 2, startPoint.y));
                        points.add(new PointF(endPoint.x - dx / 2, endPoint.y));
                    } else {            //↑ ↖ ↑, ↓ ↙ ↓
                        points.add(new PointF(startPoint.x, startPoint.y + halfLen * yScale));
                        points.add(new PointF(endPoint.x, endPoint.y - halfLen * yScale));
                    }
                }
            }
        }

        points.add(endPoint);
        points.add(end);

        points = keepCorners(points);
        Path path = new Path();
        for (int i = 0; i < points.size(); i++) {
            if (i == 0) {
                path.moveTo(points.get(i).x, points.get(i).y);
            } else {
                path.lineTo(points.get(i).x, points.get(i).y);
            }
        }

        return path;
    }


    private static final float FUSION_THRESHOLD = 5;

    private static List<PointF> keepCorners(List<PointF> points) {
        if (points.size() <= 2) return points;

        // ===== 第一步：平均融合近距离点 =====
        List<PointF> fused = new ArrayList<>();
        float sumX = points.get(0).x;
        float sumY = points.get(0).y;
        int count = 1;

        for (int i = 1; i < points.size(); i++) {
            PointF current = points.get(i);
            // 计算到当前融合组“平均点”的距离（用最新平均值）
            float avgX = sumX / count;
            float avgY = sumY / count;
            float dx = current.x - avgX;
            float dy = current.y - avgY;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= FUSION_THRESHOLD) {
                // 融入当前组
                sumX += current.x;
                sumY += current.y;
                count++;
            } else {
                // 输出当前组的平均点
                fused.add(new PointF(sumX / count, sumY / count));
                // 开启新组
                sumX = current.x;
                sumY = current.y;
                count = 1;
            }
        }
        // 添加最后一组
        fused.add(new PointF(sumX / count, sumY / count));

        // ===== 第二步：移除共线中间点 =====
        if (fused.size() <= 2) {
            return new ArrayList<>(fused);
        }

        List<PointF> result = new ArrayList<>();
        result.add(fused.get(0));

        for (int i = 1; i < fused.size() - 1; i++) {
            PointF a = fused.get(i - 1);
            PointF b = fused.get(i);
            PointF c = fused.get(i + 1);

            // 叉积判断三点是否共线
            float cross = (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x);
            if (cross != 0) {
                result.add(b);
            }
        }

        result.add(fused.get(fused.size() - 1));
        return result;
    }

    public static class ActionArea {
        private final int ORIGIN_OFFSET = SettingSaver.getInstance().getArrangeCardOffset();
        private final int OFFSET = ORIGIN_OFFSET * 2;

        public Action action;
        public boolean execute;

        public final Point pos;

        public RectF allArea = new RectF(); // 动作使用的所有参数+参数使用的参数+自身共同占据的区域大小
        public RectF actionArea = new RectF(); // 动作对应卡片区域
        public RectF paramsArea = new RectF(); // 参数区域
        public final RectF commonParamsArea; // 公共参数区域，完整包含参数区域
        public final RectF occupiedArea = new RectF(); // 执行真实占用区域，包含子树

        public List<ActionArea> executes = new ArrayList<>();
        public List<ActionArea> params = new ArrayList<>();

        private final float gridSize;

        public ActionArea(CardLayoutView cardLayoutView, Set<Action> handledActions, Action action, RectF commonParamsArea, boolean execute) {
            this.action = action;
            this.execute = execute;
            this.commonParamsArea = commonParamsArea;

            pos = new Point(action.getPos());
            gridSize = cardLayoutView.getGridSize();
            float gridOffset = gridSize * OFFSET;

            ActionCard card = cardLayoutView.getActionCard(action);
            actionArea = new RectF(0, 0, formatToGridPx(card.getWidth()), formatToGridPx(card.getHeight()));

            // 统计动作参数连接和向下执行连接
            Task task = cardLayoutView.getTask();
            List<Action> executes = new ArrayList<>();
            List<Action> params = new ArrayList<>();
            action.getPins().forEach(pin -> {
                if (!pin.isLinked()) return;
                boolean isExecute = pin.getValue() instanceof PinExecute;
                if (isExecute && !pin.isOut()) return;
                if (pin.isOut() && !(isExecute)) return;

                Pin linkedPin = pin.getLinkedPin(task);
                if (linkedPin == null) return;

                Action linkedAction = task.getAction(linkedPin.getOwnerId());
                if (linkedAction == null) return;

                if (handledActions.contains(linkedAction)) return;
                handledActions.add(linkedAction);

                if (isExecute) {
                    executes.add(linkedAction);
                } else {
                    params.add(linkedAction);
                }
            });

            // 计算参数区域，递归计算
            for (int i = 0; i < params.size(); i++) {
                Action paramAction = params.get(i);
                ActionArea area = new ActionArea(cardLayoutView, handledActions, paramAction, new RectF(), false);
                this.params.add(area);
                paramsArea.right = Math.max(paramsArea.width(), area.allArea.width());
                paramsArea.bottom += (area.allArea.height() + (i == params.size() - 1 ? 0 : gridSize));
            }

            commonParamsArea.right = Math.max(commonParamsArea.width(), paramsArea.width());
            commonParamsArea.bottom += Math.max(actionArea.height(), paramsArea.height());

            allArea.right = paramsArea.width() + gridOffset + actionArea.width();
            allArea.bottom = Math.max(actionArea.height(), paramsArea.height());

            // 继续计算向下的执行
            for (int i = 0, executesSize = executes.size(); i < executesSize; i++) {
                Action executeAction = executes.get(i);
                // 第一个执行共用通用参数区域
                ActionArea area = new ActionArea(cardLayoutView, handledActions, executeAction, i == 0 ? commonParamsArea : new RectF(), true);
                this.executes.add(area);
            }
        }

        public ActionArea(CardLayoutView cardLayoutView, List<Action> startActions) {
            gridSize = cardLayoutView.getGridSize();
            commonParamsArea = new RectF();
            pos = new Point();

            Set<Action> handledActions = new HashSet<>();

            for (Action action : startActions) {
                if (handledActions.contains(action)) return;
                handledActions.add(action);

                ActionArea area = new ActionArea(cardLayoutView, handledActions, action, new RectF(), true);
                executes.add(area);
            }
        }

        /**
         * 整理动作区域
         *
         * @param start  动作区域的开始位置
         * @param offset 偏移
         * @return 动作区域最终占据的宽度
         */
        public RectF arrange(PointF start, float offset) {
            float gridOffset = gridSize * OFFSET;

            // 设置动作位置
            if (action != null) {
                float actionStartX = start.x + offset;
                float actionStartY = start.y;
                int x = formatToGrid(actionStartX);
                int y = formatToGrid(actionStartY);
                pos.x = x;
                pos.y = y;
                if (!action.isLocked()) action.setPos(x, y);
            }

            // 每个子执行需要互不干扰，所以需要每次偏移之前的执行宽度
            float areaTotalWidth = 0;
            float areaTotalHeight = 0;

            float executeStartX = start.x;
            float executeStartY = start.y + allArea.height() + (action == null ? 0 : gridOffset);
            for (int i = 0; i < executes.size(); i++) {
                ActionArea area = executes.get(i);
                RectF areaOccupiedArea = area.arrange(new PointF(executeStartX, executeStartY), area.commonParamsArea.width());

                float realExecuteWidth = area.commonParamsArea.width() + areaOccupiedArea.width() + gridOffset;
                executeStartX += realExecuteWidth;

                if (i == 0) areaTotalWidth += areaOccupiedArea.width();
                else areaTotalWidth += realExecuteWidth;

                areaTotalHeight = Math.max(areaTotalHeight, areaOccupiedArea.height());
            }

            float paramStartX = start.x;
            float paramStartY = start.y;
            for (ActionArea area : params) {
                float paramOffset = commonParamsArea.width() - area.actionArea.width() - gridOffset;
                if (!execute) paramOffset = offset - area.actionArea.width() - gridOffset;
                area.arrange(new PointF(paramStartX, paramStartY), paramOffset);
                paramStartY += (area.allArea.height() + gridSize);
            }

            areaTotalHeight += (allArea.height() + gridOffset);

            occupiedArea.right = Math.max(actionArea.width(), areaTotalWidth);
            occupiedArea.bottom = Math.max(allArea.height(), areaTotalHeight);
            return occupiedArea;
        }

        // 计算当前支线块
        public void compact() {
            float gridOffset = gridSize * OFFSET;

            List<RectF> rectList = new ArrayList<>();
            for (int i = 0; i < executes.size(); i++) {
                ActionArea area = executes.get(i);
                area.compact();

                if (i != 0) {
                    ActionArea prevArea = executes.get(i - 1);
                    getAllActionArea(rectList, prevArea);
                    RectF rectF = calculateRealActionArea(area);
                    float left = rectF.left - area.commonParamsArea.width();
                    float top = rectF.top;
                    float right = rectF.left + area.occupiedArea.width();
                    float bottom = Math.max(rectF.bottom, rectF.top + area.occupiedArea.height());
                    RectF blockArea = new RectF(left, top, right, bottom);

                    float minLeft = findMinLeft(rectList, blockArea);
                    float targetLeft = minLeft + gridOffset + area.commonParamsArea.width();

                    offsetActionArea(area, formatToGrid(targetLeft - rectF.left));
                }
            }
        }

        private static void offsetActionArea(ActionArea area, int grid) {
            if (grid == 0) return;
            area.pos.offset(grid, 0);
            if (!area.action.isLocked()) area.action.getPos().offset(grid, 0);

            for (ActionArea execute : area.executes) {
                offsetActionArea(execute, grid);
            }

            for (ActionArea param : area.params) {
                offsetActionArea(param, grid);
            }
        }

        private static float findMinLeft(List<RectF> rectList, RectF blockArea) {
            float minLeft = 0;
            for (RectF rectF : rectList) {
                // 跳过不在同一行的
                if (rectF.bottom < blockArea.top || rectF.top > blockArea.bottom) continue;
                minLeft = Math.max(rectF.right, minLeft);
            }
            return minLeft;
        }

        private static void getAllActionArea(List<RectF> rectList, ActionArea area) {
            rectList.add(calculateRealActionArea(area));

            for (ActionArea execute : area.executes) {
                getAllActionArea(rectList, execute);
            }
            for (ActionArea param : area.params) {
                getAllActionArea(rectList, param);
            }
        }

        private static RectF calculateRealActionArea(ActionArea area) {
            Point pos = area.pos;
            float x = pos.x * area.gridSize;
            float y = pos.y * area.gridSize;
            RectF rectF = new RectF(area.actionArea);
            rectF.offset(x, y);
            return rectF;
        }

        private int formatToGridPx(float size) {
            return (int) (Math.round(size / gridSize) * gridSize);
        }

        private int formatToGrid(float size) {
            return (int) (size / gridSize);
        }
    }
}
