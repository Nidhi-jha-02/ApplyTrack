package com.applytrack.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the generic DataContainer utility class.
 *
 * Demonstrates:
 * - Generics: Type safety and unbounded/bounded wildcards - Unit 3
 * - Collections Framework operations - Unit 3
 */
public class DataContainerTest {

    @Test
    @DisplayName("Verify generic addition, retrieval, and size counting")
    public void testBasicContainerOperations() {
        DataContainer<String> container = new DataContainer<>();
        assertTrue(container.isEmpty());
        assertEquals(0, container.size());

        container.add("Google");
        container.add("Microsoft");
        container.add("Amazon");

        assertFalse(container.isEmpty());
        assertEquals(3, container.size());
        assertEquals("Google", container.get(0));
        assertEquals("Microsoft", container.get(1));
        assertEquals("Amazon", container.get(2));
        assertTrue(container.contains("Microsoft"));
        assertFalse(container.contains("Apple"));
    }

    @Test
    @DisplayName("Verify null addition throws IllegalArgumentException")
    public void testNullAddition() {
        DataContainer<String> container = new DataContainer<>();
        assertThrows(IllegalArgumentException.class, () -> container.add(null));
    }

    @Test
    @DisplayName("Verify out of bounds access throws IndexOutOfBoundsException")
    public void testOutOfBounds() {
        DataContainer<Integer> container = new DataContainer<>();
        assertThrows(IndexOutOfBoundsException.class, () -> container.get(0));
        container.add(10);
        assertThrows(IndexOutOfBoundsException.class, () -> container.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> container.get(-1));
    }

    @Test
    @DisplayName("Verify defensive copy of getAll()")
    public void testDefensiveGetAll() {
        DataContainer<String> container = new DataContainer<>();
        container.add("Item 1");

        List<String> list = container.getAll();
        assertEquals(1, list.size());

        // Modifying the returned list directly must fail
        assertThrows(UnsupportedOperationException.class, () -> list.add("Unauthorized Item"));
    }

    @Test
    @DisplayName("Verify addAll using bounded wildcard (? extends T)")
    public void testAddAll() {
        DataContainer<Number> container = new DataContainer<>();
        List<Integer> integers = Arrays.asList(1, 2, 3);
        List<Double> doubles = Arrays.asList(4.5, 5.5);

        container.addAll(integers);
        container.addAll(doubles);

        assertEquals(5, container.size());
    }

    @Test
    @DisplayName("Verify functional filtering with Predicate")
    public void testFiltering() {
        DataContainer<Integer> container = new DataContainer<>();
        container.addAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

        List<Integer> evens = container.filter(n -> n % 2 == 0);
        assertEquals(5, evens.size());
        assertTrue(evens.contains(2));
        assertTrue(evens.contains(10));
        assertFalse(evens.contains(1));

        Optional<Integer> firstGreaterThanFive = container.findFirst(n -> n > 5);
        assertTrue(firstGreaterThanFive.isPresent());
        assertEquals(6, firstGreaterThanFive.get());
    }

    @Test
    @DisplayName("Verify container removal and clear")
    public void testRemovalAndClear() {
        DataContainer<String> container = new DataContainer<>();
        container.add("Alpha");
        container.add("Beta");

        assertTrue(container.remove("Alpha"));
        assertEquals(1, container.size());
        assertFalse(container.contains("Alpha"));

        container.clear();
        assertTrue(container.isEmpty());
        assertEquals(0, container.size());
    }
}

