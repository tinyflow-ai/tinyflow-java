package dev.tinyflow.core.chain.runtime;

public enum TriggerType {
    NEXT,
    LOOP,
    RETRY,
    TIMER,
    CRON,
    EVENT,
    DELAY,
    MANUAL
}