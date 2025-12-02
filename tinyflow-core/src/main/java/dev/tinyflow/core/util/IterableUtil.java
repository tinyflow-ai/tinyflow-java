package dev.tinyflow.core.util;

import java.util.Collection;
import java.util.Iterator;

public class IterableUtil {

    public static boolean isEmpty(Iterable<?> iterable) {
        return iterable == null || !iterable.iterator().hasNext();
    }

    public static boolean isNotEmpty(Iterable<?> iterable) {
        return !isEmpty(iterable);
    }

    public static int size(Iterable<?> iterable) {
        if (iterable == null) {
            return 0;
        }
        if (iterable instanceof Collection) {
            return ((Collection<?>) iterable).size();
        }
        int size = 0;
        for (Iterator<?> it = iterable.iterator(); it.hasNext(); it.next()) {
            size++;
        }
        return size;
    }
}
