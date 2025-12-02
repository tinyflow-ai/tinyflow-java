package dev.tinyflow.core.chain.runtime;

import java.util.List;

public interface TriggerStore {
    Trigger save(Trigger trigger);

    boolean remove(String triggerId);

    Trigger find(String triggerId);

    List<Trigger> findDue(long uptoTimestamp);

    List<Trigger> findAllPending();
}
