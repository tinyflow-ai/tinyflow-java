package dev.tinyflow.core.chain.runtime;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryTriggerStore implements TriggerStore {

    private final ConcurrentHashMap<String, Trigger> store = new ConcurrentHashMap<>();

    @Override
    public Trigger save(Trigger trigger) {
        if (trigger.getId() == null) {
            trigger.setId(UUID.randomUUID().toString());
        }
        store.put(trigger.getId(), trigger);
        return trigger;
    }

    @Override
    public boolean remove(String triggerId) {
        return store.remove(triggerId) != null;
    }

    @Override
    public Trigger find(String triggerId) {
        return store.get(triggerId);
    }

    @Override
    public List<Trigger> findDue(long uptoTimestamp) {
        return null;
    }

    @Override
    public List<Trigger> findAllPending() {
        return new ArrayList<>(store.values());
    }
}

