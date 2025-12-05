package dev.tinyflow.core.chain;

import dev.tinyflow.core.util.Maps;

import java.util.HashMap;
import java.util.Map;

public class ChainConsts {

    public static final String SCHEDULE_NEXT_NODE_DISABLED_KEY = "__schedule_next_node_disabled";

    public static Map<String, Object> SCHEDULE_NEXT_NODE_DISABLED_RESULT = new HashMap<String, Object>(
            Maps.of(SCHEDULE_NEXT_NODE_DISABLED_KEY, true)) {
        @Override
        public Object put(String key, Object value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    };
}
