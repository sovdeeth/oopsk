package com.sovdee.oopsk.core;

import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.Objects;

/**
 * A field is a single typed field in a struct template.
 * It contains a name, a type (single/plural), and an optional default value.
 */
public record Field<T>(String name, ClassInfo<T> type, boolean single, boolean constant, @Nullable Expression<? extends T> defaultExpression) {

    /**
     * Creates a new field with the given name, type, and default value.
     *
     * @param name            The name of the field.
     * @param type            The type of the field.
     * @param single          Whether the field is single or plural.
     * @param defaultExpression The default value of the field (unevaluated).
     */
    public Field(String name, ClassInfo<T> type, boolean single, boolean constant, @Nullable Expression<? extends T> defaultExpression) {
        this.name = name;
        this.type = type;
        this.single = single;
        this.constant = constant;
        Expression<? extends T> expr;
        if (defaultExpression == null && (expr = type.getDefaultExpression()) != null) {
            this.defaultExpression = expr;
        } else {
            this.defaultExpression = defaultExpression;
        }
    }

    /**
     * Evaluates the default value of this field.
     * @param event The event to evaluate the default value in.
     * @return The default value of this field, or an empty array if no default value is set.
     */
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
