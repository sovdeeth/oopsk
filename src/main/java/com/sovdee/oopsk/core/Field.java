package com.sovdee.oopsk.core;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * A field is a single typed field in a struct.
 * It contains a name, a type, and an optional default value.
 */
public record Field<T>(String name, ClassInfo<T> type, boolean single, @Nullable Expression<? extends T> defaultExpression) {

    public Field(String name, ClassInfo<T> type, boolean single, @Nullable Expression<? extends T> defaultExpression) {
        this.name = name;
        this.type = type;
        this.single = single;
        Expression<? extends T> expr;
        if (defaultExpression == null && (expr = type.getDefaultExpression()) != null) {
            this.defaultExpression = expr;
        } else {
            this.defaultExpression = defaultExpression;
        }
    }

    public T[] defaultValue(Event event) {
        if (defaultExpression == null) {
            //noinspection unchecked
            return (T[]) Array.newInstance(type.getC(), 0);
        }
        return defaultExpression.getArray(event);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Field<?> other)) return false;

        return this.name.equals(other.name)
                && this.type.equals(other.type)
                && this.single == other.single;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, single);
    }

    @Override
    public String toString() {
        return "field '" + name() + "' (" + type().getName().toString(!single) + ")";
    }
}
