package dev.tinyflow.core.chain.runtime;

public class TriggerContext {
    private static final ThreadLocal<Trigger> currentTrigger = new ThreadLocal<>();
    public static Trigger getCurrentTrigger() {
        return currentTrigger.get();
    }
    public static void setCurrentTrigger(Trigger trigger) {
        currentTrigger.set(trigger);
    }
    public static void clearCurrentTrigger() {
        currentTrigger.remove();
    }
}
