package com.applytrack.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Reusable generic container class for managing collections of objects in memory.
 *
 * Demonstrates:
 * - Generics: Type parameters (T) and Bounded Wildcards (? extends T) - Unit 3
 * - Collections Framework: ArrayList and defensive unmodifiable views - Unit 3
 * - Functional programming: Predicate<T> filtering - Unit 3
 * - Optional<T> return types for null-safe lookups - Unit 3
 *
 * @param <T> The entity or model type stored within the container
 */
public class DataContainer<T> {

    private final List<T> items;

    /**
     * Default constructor creating an empty container.
     */
    public DataContainer() {
        this.items = new ArrayList<>();
    }

    /**
     * Overloaded constructor accepting an initial collection.
     */
    public DataContainer(Collection<? extends T> initialItems) {
        this.items = new ArrayList<>();
        if (initialItems != null) {
            this.items.addAll(initialItems);
        }
    }

    /**
     * Adds an item to the container.
     *
     * @param item The element to append
     * @throws IllegalArgumentException if item is null
     */
    public void add(T item) {
        if (item == null) {
            throw new IllegalArgumentException("Container cannot store null items.");
        }
        items.add(item);
    }

    /**
     * Appends all items from a collection using a generic wildcard (? extends T).
     */
    public void addAll(Collection<? extends T> collection) {
        if (collection != null) {
            for (T item : collection) {
                if (item != null) {
                    items.add(item);
                }
            }
        }
    }

    /**
     * Removes a specific item from the container.
     */
    public boolean remove(T item) {
        return items.remove(item);
    }

    /**
     * Retrieves an item at a specific index.
     */
    public T get(int index) {
        if (index < 0 || index >= items.size()) {
            throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + items.size());
        }
        return items.get(index);
    }

    /**
     * Returns an unmodifiable snapshot copy of all items in the container.
     */
    public List<T> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    /**
     * Returns the number of elements in the container.
     */
    public int size() {
        return items.size();
    }

    /**
     * Checks if the container has zero elements.
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }

    /**
     * Checks if the container contains a specific item.
     */
    public boolean contains(T item) {
        return items.contains(item);
    }

    /**
     * Removes all elements from the container.
     */
    public void clear() {
        items.clear();
    }

    /**
     * Filters items based on a Predicate condition.
     */
    public List<T> filter(Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        if (predicate == null) {
            return getAll();
        }
        for (T item : items) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * Finds the first element matching a Predicate.
     */
    public Optional<T> findFirst(Predicate<T> predicate) {
        if (predicate == null) {
            return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
        }
        for (T item : items) {
            if (predicate.test(item)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "DataContainer{size=" + items.size() + ", items=" + items + '}';
    }
}

