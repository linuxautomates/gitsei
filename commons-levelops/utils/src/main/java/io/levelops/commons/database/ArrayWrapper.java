package io.levelops.commons.database;

import com.google.common.base.Objects;

import java.util.List;

public class ArrayWrapper<T> {
    private final String type;
    private final List<T> data;

    public ArrayWrapper(String type, List<T> data) {
        this.type = type;
        this.data = data;
    }

    public static <T> ArrayWrapper<T> fromList(String type, List<T> data) {
        return new ArrayWrapper<>(type, data);
    }

    public String getType() {
        return type;
    }

    public List<T> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayWrapper<?> that = (ArrayWrapper<?>) o;
        return Objects.equal(type, that.type) &&
                Objects.equal(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(type, data);
    }

    @Override
    public String toString() {
        return "ArrayWrapper{" +
                "type='" + type + '\'' +
                ", data=" + data +
                '}';
    }
}
