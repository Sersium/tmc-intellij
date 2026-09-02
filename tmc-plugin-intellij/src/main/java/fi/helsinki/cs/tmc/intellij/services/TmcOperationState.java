package fi.helsinki.cs.tmc.intellij.services;

import com.intellij.ide.ActivityTracker;
import com.intellij.openapi.application.ApplicationManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Coordinates concurrency across TMC operations (Run, Test, Submit).
 * Freezes/disables conflicting buttons while any operation is in progress.
 */
public class TmcOperationState {

    public enum Operation {
        NONE(""),
        RUNNING("running project"),
        TESTING("running tests"),
        SUBMITTING("submitting exercise");

        private final String description;

        Operation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private static final AtomicReference<Operation> currentOperation =
            new AtomicReference<>(Operation.NONE);

    private static final List<Consumer<Boolean>> listeners = new CopyOnWriteArrayList<>();

    public static boolean isOperationRunning() {
        return currentOperation.get() != Operation.NONE;
    }

    public static Operation getCurrentOperation() {
        return currentOperation.get();
    }

    public static boolean tryStartOperation(Operation op) {
        boolean started = currentOperation.compareAndSet(Operation.NONE, op);
        if (started) {
            notifyStateChanged(true);
        }
        return started;
    }

    public static void finishOperation() {
        if (currentOperation.getAndSet(Operation.NONE) != Operation.NONE) {
            notifyStateChanged(false);
        }
    }

    public static void addStateListener(Consumer<Boolean> listener) {
        listeners.add(listener);
        // Immediately notify of current state
        listener.accept(isOperationRunning());
    }

    public static void removeStateListener(Consumer<Boolean> listener) {
        listeners.remove(listener);
    }

    private static void notifyStateChanged(boolean isBusy) {
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                ActivityTracker.getInstance().inc();
            } catch (Throwable ignored) {
            }
            for (Consumer<Boolean> listener : listeners) {
                try {
                    listener.accept(isBusy);
                } catch (Throwable ignored) {
                }
            }
        });
    }
}
