package ui.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AITaskManager {
    public interface Listener {
        void onTaskAdded(AITask task);
        void onTaskUpdated(AITask task);
    }

    private final List<AITask> tasks = new ArrayList<>();
    private final List<Listener> listeners = new ArrayList<>();

    public synchronized void addListener(Listener listener) {
        listeners.add(listener);
    }

    public synchronized void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized List<AITask> tasks() {
        return Collections.unmodifiableList(new ArrayList<>(tasks));
    }

    public void addTask(AITask task) {
        List<Listener> snapshot;
        synchronized (this) {
            tasks.add(0, task); // newest first
            snapshot = new ArrayList<>(listeners);
        }
        for (Listener l : snapshot) {
            l.onTaskAdded(task);
        }
    }

    public void notifyUpdated(AITask task) {
        List<Listener> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(listeners);
        }
        for (Listener l : snapshot) {
            l.onTaskUpdated(task);
        }
    }
}


