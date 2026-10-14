package top.bogey.touch_tool.ui.blueprint.card;

import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import top.bogey.touch_tool.bean.pin.Pin;
import top.bogey.touch_tool.ui.blueprint.pin.PinLeftView;
import top.bogey.touch_tool.ui.blueprint.pin.PinView;
import top.bogey.touch_tool.utils.ui.DragViewHolderHelper;
import top.bogey.touch_tool.utils.ui.IDragAbleRecycleViewAdapter;

// 列表、字典的输入项列表，长按可以调换顺序
// 列表每行是一个针脚，字典每行是键值两个针脚，拖动时整行一起移动
public class CreateListActionAdapter extends RecyclerView.Adapter<CreateListActionAdapter.ViewHolder> implements IDragAbleRecycleViewAdapter {
    private final List<List<PinView>> rows = new ArrayList<>();
    private final ActionCard card;
    private final ItemTouchHelper touchHelper;

    public CreateListActionAdapter(ActionCard card) {
        this.card = card;
        // 长按整行拖动，拖动柄只作为可拖动位置的视觉提示
        touchHelper = new ItemTouchHelper(new DragViewHolderHelper(DragViewHolderHelper.VERTICAL, this));
    }

    public void attachToRecyclerView(RecyclerView recyclerView) {
        recyclerView.setAdapter(this);
        touchHelper.attachToRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LinearLayout layout = new LinearLayout(parent.getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(layout);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.refresh(rows.get(position));
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    // 新开一行，列表的输入项都独占一行
    public PinView addPin(Pin pin) {
        PinView pinView = createPinView(pin);
        List<PinView> row = new ArrayList<>();
        row.add(pinView);
        rows.add(row);
        notifyItemInserted(rows.size() - 1);
        return pinView;
    }

    // 并入最后一行，字典的值针脚要和它前面的键针脚同一行
    public PinView addPinToLast(Pin pin) {
        PinView pinView = createPinView(pin);
        if (rows.isEmpty()) {
            List<PinView> row = new ArrayList<>();
            row.add(pinView);
            rows.add(row);
            notifyItemInserted(0);
        } else {
            rows.get(rows.size() - 1).add(pinView);
            notifyItemChanged(rows.size() - 1);
        }
        return pinView;
    }

    // 输入项都是左侧针脚
    private PinView createPinView(Pin pin) {
        return new PinLeftView(card.getContext(), card, pin);
    }

    public void removePin(Pin pin) {
        for (int i = 0; i < rows.size(); i++) {
            List<PinView> row = rows.get(i);
            for (int j = 0; j < row.size(); j++) {
                if (row.get(j).getPin().equals(pin)) {
                    row.remove(j);
                    // 字典的键值成对删除，整行空了才移除这一行
                    if (row.isEmpty()) {
                        rows.remove(i);
                        notifyItemRemoved(i);
                    } else {
                        notifyItemChanged(i);
                    }
                    return;
                }
            }
        }
    }

    @Override
    public void swap(int from, int to) {
        // 拖动过程中位置可能失效，越界直接忽略
        if (from == to || from < 0 || to < 0 || from >= rows.size() || to >= rows.size()) return;

        // 移动前先取出现有的输入项，并记下它们在全局针脚里的起点
        List<Pin> oldPins = new ArrayList<>();
        for (List<PinView> row : rows) {
            for (PinView pinView : row) oldPins.add(pinView.getPin());
        }
        List<Pin> pins = card.getAction().getPins();
        int start = pins.indexOf(oldPins.get(0));
        if (start < 0) return;

        rows.add(to, rows.remove(from));

        // 输入项在全局针脚里是连续的一段，按新的行顺序整体重排
        // 字典一行是键值两个针脚，整行一起走，键值配对不会被打乱
        List<Pin> newPins = new ArrayList<>();
        for (List<PinView> row : rows) {
            for (PinView pinView : row) newPins.add(pinView.getPin());
        }
        pins.removeAll(newPins);
        pins.addAll(start, newPins);

        notifyItemMoved(from, to);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout layout;

        public ViewHolder(@NonNull LinearLayout itemView) {
            super(itemView);
            layout = itemView;
        }

        public void refresh(List<PinView> pinViews) {
            layout.removeAllViews();
            for (PinView pinView : pinViews) {
                ViewGroup parent = (ViewGroup) pinView.getParent();
                if (parent != null) parent.removeView(pinView);
                layout.addView(pinView);
            }
        }
    }
}
