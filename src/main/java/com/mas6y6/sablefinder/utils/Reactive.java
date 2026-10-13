package com.mas6y6.sablefinder.utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class Reactive<T> {
    private @Nullable T value;
    private final ArrayList<Listener<T>> listeners = new ArrayList<>();

    private Reactive(T value) {
        this.value = value;
    }

    public static <T> @NotNull Reactive<T> of(T value) {
        return new Reactive<>(value);
    }

    public T get() {
        return value;
    }

    public @Nullable T set(T value) {
        T oldValue = this.value;

        if (Objects.equals(oldValue, value)) {
            return value;
        }

        this.value = value;
        notifyListeners(value, oldValue);

        return value;
    }

    private void notifyListeners(T newValue, T oldValue) {
        for (Listener<T> listener : listeners) {
            listener.onChange(newValue, oldValue);
        }
    }

    public void addListener(Listener<T> listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener<T> listener) {
        listeners.remove(listener);
    }

    @FunctionalInterface
    public interface Listener<T> {
        void onChange(T newValue, T oldValue);
    }
}
