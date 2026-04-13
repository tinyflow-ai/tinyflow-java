package dev.tinyflow.core.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class IterableUtil {

    public static boolean isEmpty(Iterable<?> iterable) {
        if (iterable == null) {
            return true;
        }
        if (iterable instanceof Collection) {
            return ((Collection<?>) iterable).isEmpty();
        }
        return !iterable.iterator().hasNext();
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

    public static <T> T get(Iterable<T> iterable, int index) {
        if (iterable == null) {
            throw new IllegalArgumentException("iterable must not be null");
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException("index < 0: " + index);
        }

        if (iterable instanceof List) {
            List<T> list = (List<T>) iterable;
            if (index >= list.size()) {
                throw new IndexOutOfBoundsException("index >= size: " + index);
            }
            return list.get(index);
        }

        int i = 0;
        for (T t : iterable) {
            if (i == index) {
                return t;
            }
            i++;
        }

        throw new IndexOutOfBoundsException("index >= size: " + index);
    }
}
