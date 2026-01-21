package top.bogey.touch_tool.ui.blueprint;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import top.bogey.touch_tool.R;
import top.bogey.touch_tool.bean.action.Action;
import top.bogey.touch_tool.bean.action.normal.LoggerAction;
import top.bogey.touch_tool.bean.action.start.StartAction;
import top.bogey.touch_tool.bean.action.task.CustomEndAction;
import top.bogey.touch_tool.bean.action.task.CustomStartAction;
import top.bogey.touch_tool.bean.save.SettingSaver;
import top.bogey.touch_tool.bean.save.task.TaskSaver;
import top.bogey.touch_tool.bean.task.Task;
import top.bogey.touch_tool.databinding.ViewBlueprintBinding;
import top.bogey.touch_tool.ui.MainActivity;
import top.bogey.touch_tool.ui.blueprint.card.ActionCard;
import top.bogey.touch_tool.ui.blueprint.history.HistoryManager;
import top.bogey.touch_tool.ui.blueprint.selecter.select_action.SelectActionDialog;
import top.bogey.touch_tool.ui.tool.log.LogFloatView;
import top.bogey.touch_tool.utils.AppUtil;
import top.bogey.touch_tool.utils.DisplayUtil;

public class BlueprintView extends Fragment {
    private final static List<Action> copyActions = new ArrayList<>();

    private final Stack<Task> taskStack = new Stack<>();
    private final Map<String, HistoryManager> managers = new HashMap<>();

    private ViewBlueprintBinding binding;
    private HistoryManager history;
    private boolean needDelete = false;

    public static void tryPushStack(Task task) {
        Fragment fragment = MainActivity.getCurrentFragment();
        if (fragment instanceof BlueprintView blueprintView) {
            blueprintView.pushStack(task);
        }
    }

    public static void tryFocusAction(Task task, Action action) {
        if (task == null || action == null) return;

        Fragment fragment = MainActivity.getCurrentFragment();
        if (fragment instanceof BlueprintView blueprintView) {
            if (!task.equals(blueprintView.taskStack.peek())) {
                blueprintView.pushStack(task);
                blueprintView.binding.getRoot().postDelayed(() -> blueprintView.binding.cardLayout.focusCard(action.getId()), 100);
            } else {
                blueprintView.binding.cardLayout.focusCard(action.getId());
            }
        }
    }

    public static void tryRefreshPinView() {
        Fragment fragment = MainActivity.getCurrentFragment();
        if (fragment instanceof BlueprintView blueprintView) {
            blueprintView.binding.cardLayout.refreshPinView();
        }
    }

    public static void tryShowFloatingToolBar(boolean show) {
        Fragment fragment = MainActivity.getCurrentFragment();
        if (fragment instanceof BlueprintView blueprintView) {
            blueprintView.binding.floatingToolBar.setVisibility(show ? View.VISIBLE : View.GONE);
            blueprintView.binding.baseToolBar.setVisibility(show ? View.GONE : View.VISIBLE);

            if (show) {
                List<Action> selectedActions = blueprintView.binding.cardLayout.getSelectedActions();
                boolean locked = false;
                for (Action selectedAction : selectedActions) {
                    if (selectedAction.isLocked()) {
                        locked = true;
                        break;
                    }
                }
                blueprintView.binding.lockButton.setIconResource(locked ? R.drawable.icon_lock : R.drawable.icon_lock_open);
                blueprintView.binding.lockButton.setChecked(locked);
            }
        }
    }

    private final OnBackPressedCallback callback = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            popStack();
        }
    };

    private Menu menu;
    private final MenuProvider menuProvider = new MenuProvider() {
        @Override
        public void onCreateMenu(@NonNull Menu currMenu, @NonNull MenuInflater menuInflater) {
            menu = currMenu;
            menuInflater.inflate(R.menu.menu_blueprint, currMenu);
//            if (history != null) {
//                menu.findItem(R.id.back).setEnabled(history.canBack());
//                menu.findItem(R.id.forward).setEnabled(history.canForward());
//            }
        }

        @Override
        public void onPrepareMenu(@NonNull Menu menu) {
            Task task = taskStack.peek();
            MenuItem item = menu.findItem(R.id.taskDetailLog);
            item.setChecked(task.hasFlag(Task.FLAG_DEBUG));
            item.setVisible(task.getParent() == null);

            boolean logFlag = false;
            Queue<Task> queue = new LinkedList<>();
            queue.add(task);
            while (!queue.isEmpty()) {
                Task pool = queue.poll();
                if (pool == null) continue;
                for (Action action : pool.getActions(LoggerAction.class)) {
                    LoggerAction logger = (LoggerAction) action;
                    logFlag |= logger.getLogSwitch();
                }
                queue.addAll(pool.getTasks());
            }
            MenuItem logSwitchItem = menu.findItem(R.id.taskRunningLogSwitch);
            logSwitchItem.setChecked(logFlag);
        }

        @Override
        public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
            int itemId = menuItem.getItemId();
            /*if (itemId == R.id.back) {
                if (history != null) history.back(binding.cardLayout);
                menu.findItem(R.id.back).setEnabled(history.canBack());
                menu.findItem(R.id.forward).setEnabled(history.canForward());
                return true;
            } else if (itemId == R.id.forward) {
                if (history != null) history.forward(binding.cardLayout);
                menu.findItem(R.id.back).setEnabled(history.canBack());
                menu.findItem(R.id.forward).setEnabled(history.canForward());
                return true;
            } else */if (itemId == R.id.save) {
                Task task = taskStack.peek();
                task.save();
                return true;
            } else if (itemId == R.id.taskRunningLog) {
                Task task = taskStack.peek();
                while (task.getParent() != null) task = task.getParent();
                new LogFloatView(requireContext(), task).show();
                return true;
            } else if (itemId == R.id.taskRunningLogSwitch) {
                boolean logFlag = false;
                Task task = taskStack.peek();
                Queue<Task> queue = new LinkedList<>();
                queue.add(task);
                while (!queue.isEmpty()) {
                    Task pool = queue.poll();
                    if (pool == null) continue;
                    for (Action action : pool.getActions(LoggerAction.class)) {
                        LoggerAction logger = (LoggerAction) action;
                        logFlag |= logger.switchLog();
                    }
                    queue.addAll(pool.getTasks());
                }
                task.save();
                menuItem.setChecked(logFlag);
                return true;
            } else if (itemId == R.id.taskDetailLog) {
                Task task = taskStack.peek();
                task.toggleFlag(Task.FLAG_DEBUG);
                task.save();
                menuItem.setChecked(task.hasFlag(Task.FLAG_DEBUG));
                return true;
            } else if (itemId == R.id.taskCapture) {
                Bitmap bitmap = binding.cardLayout.takeTaskCapture();
                ShapeableImageView imageView = new ShapeableImageView(requireContext());
                imageView.setImageBitmap(bitmap);

                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.task_capture)
                        .setView(imageView)
                        .setPositiveButton(R.string.save, (dialog, which) -> {
                            dialog.dismiss();
                            AppUtil.saveImage(requireContext(), bitmap);
                        })
                        .setNegativeButton(R.string.share_to_action, (dialog, which) -> {
                            dialog.dismiss();
                            AppUtil.shareImage(requireContext(), bitmap);
                        })
                        .setNeutralButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                        .show();

                Point size = DisplayUtil.getScreenSize(requireContext());
                DisplayUtil.setViewWidth(imageView, ViewGroup.LayoutParams.MATCH_PARENT);
                DisplayUtil.setViewHeight(imageView, size.y / 2);
                return true;
            }
            return false;
        }
    };

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (DisplayUtil.isPortrait(requireContext())) {
            DisplayUtil.setViewHeight(binding.toolBar, (int) DisplayUtil.dp2px(requireContext(), 56));
            DisplayUtil.setViewMargin(binding.floatingToolBar, 0, 0, 0, (int) DisplayUtil.dp2px(requireContext(), 48));
            DisplayUtil.setViewMargin(binding.baseToolBar, 0, 0, 0, (int) DisplayUtil.dp2px(requireContext(), 48));
        } else {
            DisplayUtil.setViewHeight(binding.toolBar, (int) DisplayUtil.dp2px(requireContext(), 48));
            DisplayUtil.setViewMargin(binding.floatingToolBar, 0, 0, 0, (int) DisplayUtil.dp2px(requireContext(), 24));
            DisplayUtil.setViewMargin(binding.baseToolBar, 0, 0, 0, (int) DisplayUtil.dp2px(requireContext(), 24));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Bundle arguments = getArguments();
        if (arguments == null) throw new IllegalArgumentException();
        BlueprintViewArgs args = BlueprintViewArgs.fromBundle(arguments);
        Task task = TaskSaver.getInstance().getTask(args.getTaskId());
        if (task == null) throw new IllegalArgumentException();

        binding = ViewBlueprintBinding.inflate(inflater, container, false);

        binding.toolBar.addMenuProvider(menuProvider, getViewLifecycleOwner());
        binding.toolBar.setNavigationOnClickListener(v -> {
            if (taskStack.size() > 1) {
                popStack();
            } else {
                requireActivity().getOnBackPressedDispatcher().onBackPressed();
            }
        });

        binding.addButton.setOnClickListener(v -> new SelectActionDialog(requireContext(), taskStack.peek(), action -> {
            ActionCard card = binding.cardLayout.addCard(action);
            if (card == null) return;
            binding.cardLayout.initCardPos(card);
        }).show());

        binding.sortButton.setOnClickListener(v -> {
            if (!binding.cardLayout.isLoaded()) return;
            Task currTask = taskStack.peek();
            List<Action> startActions = currTask.getActions(StartAction.class);
            List<Action> actions = currTask.getActions(CustomStartAction.class);
            startActions.addAll(actions);
            CardLayoutHelper.ActionArea actionArea = new CardLayoutHelper.ActionArea(binding.cardLayout, new HashSet<>(), startActions);
            actionArea.arrange(binding.cardLayout, new Point(), null);
            binding.cardLayout.updateCardsPos();
            currTask.save();
        });

        binding.editButton.setOnClickListener(v -> {
            boolean editable = !binding.editButton.isChecked();
            SettingSaver.getInstance().setBlueprintEditable(editable);
            binding.cardLayout.setEditable(editable);
        });
        binding.editButton.setChecked(!SettingSaver.getInstance().isBlueprintEditable());
        binding.cardLayout.setEditable(SettingSaver.getInstance().isBlueprintEditable());

        binding.pasteButton.setOnClickListener(v -> {
            binding.cardLayout.cleanSelectedCards();
            boolean first = true;
            int offsetX = 0, offsetY = 0;
            copyActions.sort(Comparator.comparingInt(o -> o.getPos().y));

            for (Action copyAction : copyActions) {
                ActionCard card = binding.cardLayout.addCard(copyAction);
                if (card == null) continue;
                if (first) {
                    first = false;
                    int x = copyAction.getPos().x;
                    int y = copyAction.getPos().y;
                    binding.cardLayout.initCardPos(card);
                    offsetX = card.getAction().getPos().x - x;
                    offsetY = card.getAction().getPos().y - y;
                } else {
                    Point pos = copyAction.getPos();
                    copyAction.setPos(pos.x + offsetX, pos.y + offsetY);
                    binding.cardLayout.updateCardPos(card);
                }
                binding.cardLayout.addSelectedCard(copyAction);
            }
            copyActions.clear();
            binding.pasteButton.setVisibility(View.GONE);
        });
        binding.pasteButton.setVisibility(copyActions.isEmpty() ? View.GONE : View.VISIBLE);

        binding.exchangeButton.setOnClickListener(v -> AppUtil.showEditDialog(getContext(), R.string.task_exchange_to_custom, "", result -> {
            if (result.isEmpty()) return;

            Task currTask = taskStack.peek();
            Task innerTask = new Task();
            binding.cardLayout.getSelectedActionsCopy().forEach(innerTask::addAction);
            innerTask.addAction(new CustomStartAction());
            innerTask.addAction(new CustomEndAction());
            innerTask.setTitle(result);
            currTask.addTask(innerTask);
            currTask.save();
            pushStack(innerTask);
        }));

        binding.lockButton.setOnClickListener(v -> {
            boolean locked = binding.lockButton.isChecked();
            binding.lockButton.setIconResource(locked ? R.drawable.icon_lock : R.drawable.icon_lock_open);
            binding.lockButton.setChecked(locked);
            List<Action> selectedActions = binding.cardLayout.getSelectedActions();
            selectedActions.forEach(action -> {
                action.setLocked(locked);
                ActionCard card = binding.cardLayout.getActionCard(action);
                if (card != null) card.refreshCardLockState();
            });
        });

        binding.copyButton.setOnClickListener(v -> {
            List<Action> selectedActions = binding.cardLayout.getSelectedActionsCopy();
            binding.cardLayout.cleanSelectedCards();
            selectedActions.forEach(action -> {
                binding.cardLayout.addCard(action);
                binding.cardLayout.addSelectedCard(action);
            });
        });

        binding.copyContentButton.setOnClickListener(v -> {
            copyActions.clear();
            copyActions.addAll(binding.cardLayout.getSelectedActionsCopy());
            binding.pasteButton.setVisibility(copyActions.isEmpty() ? View.GONE : View.VISIBLE);
            binding.cardLayout.cleanSelectedCards();
        });

        binding.deleteButton.setOnClickListener(v -> {
            if (needDelete) {
                for (ActionCard card : new HashSet<>(binding.cardLayout.selectedCards)) {
                    binding.cardLayout.removeCard(card);
                }
                binding.cardLayout.selectedCards.clear();
                binding.floatingToolBar.setVisibility(View.GONE);
            } else {
                binding.deleteButton.setChecked(true);
                needDelete = true;
                binding.deleteButton.postDelayed(() -> {
                    binding.deleteButton.setChecked(false);
                    needDelete = false;
                }, 1500);
            }
        });


        pushStack(task);
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), callback);

        return binding.getRoot();
    }

    public void pushStack(Task task) {
        if (task == null) return;

        if (!taskStack.empty()) taskStack.peek().save();

        taskStack.remove(task);
        taskStack.push(task);

        setTask(task);
    }

    public void popStack() {
        if (taskStack.empty()) return;
        Task task = taskStack.pop();
        task.save();

        if (!taskStack.empty()) {
            task = taskStack.peek();
            setTask(task);
        }
    }

    public void setTask(Task task) {
        history = managers.computeIfAbsent(task.getId(), s -> new HistoryManager());
        binding.cardLayout.setTask(task, history);

//        if (menu != null) {
//            menu.findItem(R.id.back).setEnabled(history.canBack());
//            menu.findItem(R.id.forward).setEnabled(history.canForward());
//        }

        binding.toolBar.setTitle(task.getTitle());

        callback.setEnabled(taskStack.size() > 1);
    }
}
