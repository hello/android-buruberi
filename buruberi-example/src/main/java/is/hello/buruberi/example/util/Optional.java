package is.hello.buruberi.example.util;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import rx.functions.Action1;
import rx.functions.Func0;
import rx.functions.Func1;

public final class Optional<T> {
    @SuppressWarnings("unchecked")
    private static final Optional EMPTY = new Optional(null);
    private final @Nullable T value;

    //region Creation

    public static <T> Optional<T> of(T value) {
        if (value == null) {
            throw new NullPointerException("value cannot be null");
        } else {
            return new Optional<>(value);
        }
    }

    public static <T> Optional<T> ofNullable(T value) {
        if (value == null) {
            return empty();
        } else {
            return of(value);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> empty() {
        return EMPTY;
    }

    private Optional(@Nullable T value) {
        this.value = value;
    }

    //endregion


    //region Accessing

    public boolean isPresent() {
        return (value != null);
    }

    public @NonNull T get() throws NullPointerException {
        if (value == null) {
            throw new NullPointerException("value is not present");
        } else {
            return value;
        }
    }

    public @NonNull T orElse(@NonNull T other) {
        if (isPresent()) {
            return get();
        } else {
            return other;
        }
    }

    public @NonNull T orElseGet(@NonNull Func0<? extends T> supplier) {
        if (isPresent()) {
            return get();
        } else {
            return supplier.call();
        }
    }

    public @NonNull <X extends Throwable> T orElseThrow(@NonNull Func0<? extends X> supplier) throws X {
        if (isPresent()) {
            return get();
        } else {
            throw supplier.call();
        }
    }

    public void ifPresent(@NonNull Action1<T> consumer) {
        if (isPresent()) {
            consumer.call(get());
        }
    }

    //endregion


    //region Operations

    public Optional<T> filter(@NonNull Func1<? super T, Boolean> predicate) {
        if (isPresent() && predicate.call(get())) {
            return this;
        } else {
            return empty();
        }
    }

    public <U> Optional<U> map(@NonNull Func1<? super T, U> mapper) {
        if (isPresent()) {
            return Optional.of(mapper.call(get()));
        } else {
            return empty();
        }
    }

    public <U> Optional<U> flatMap(@NonNull Func1<? super T, Optional<U>> mapper) {
        if (isPresent()) {
            return mapper.call(get());
        } else {
            return empty();
        }
    }

    //endregion


    //region Identity

    @Override
    public int hashCode() {
        return value != null ? value.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Optional<?> optional = (Optional<?>) o;
        return !(value != null ? !value.equals(optional.value) : optional.value != null);
    }

    @Override
    public String toString() {
        if (isPresent()) {
            return "{Optional '" + get() + "'}";
        } else {
            return "{Optional absent}";
        }
    }

    //endregion
}
