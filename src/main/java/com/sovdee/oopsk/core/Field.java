package com.sovdee.oopsk.core;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.util.LiteralUtils;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A field is a single typed field in a struct template.
 * It contains a name, a type (single/plural), and an optional default value.
 */
public final class Field<T> {

    public enum Modifier {
        CONSTANT,
        DYNAMIC
    }

    private final String name;
    private final ClassInfo<T> type;
    private final boolean single;
    private final @Nullable String defaultExpressionString;
    private Expression<? extends T> defaultExpression;
    private final Set<Modifier> modifiers;

    /**
     * Creates a new field with the given name, type, and default value.
     *
     * @param name                    The name of the field.
     * @param type                    The type of the field.
     * @param single                  Whether the field is single or plural.
     * @param defaultExpressionString The default value of the field (unparsed).
     * @param modifiers               The modifiers applied to this field.
     */
    public Field(String name, ClassInfo<T> type, boolean single, @Nullable String defaultExpressionString, Modifier... modifiers) {
        this(name, type, single, defaultExpressionString, Arrays.stream(modifiers).collect(Collectors.toCollection(() -> EnumSet.noneOf(Modifier.class))));
    }

    /**
     * Creates a new field with the given name, type, and default value.
     *
     * @param name                    The name of the field.
     * @param type                    The type of the field.
     * @param single                  Whether the field is single or plural.
     * @param defaultExpressionString The default value of the field (unparsed).
     * @param modifiers               The modifiers applied to this field.
     */
    public Field(String name, ClassInfo<T> type, boolean single, @Nullable String defaultExpressionString, Set<Modifier> modifiers) {
        this.name = name;
        this.type = type;
        this.single = single;
        this.modifiers = modifiers;
        Expression<? extends T> expr;
        if (defaultExpressionString == null && (expr = type.getDefaultExpression()) != null) {
            this.defaultExpression = expr;
        }
        this.defaultExpressionString = defaultExpressionString;
    }

    /**
     * Parses the default value string of this field into an expression. Prints errors.
     * @return false if errors are encountered during parsing. True otherwise.
     */
    public boolean parseDefaultValueExpression() {
        if (defaultExpression != null || defaultExpressionString == null)
            return true;

        // parse the default value
        //noinspection unchecked
        defaultExpression = new SkriptParser(defaultExpressionString, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(type.getC());
        if (defaultExpression == null || LiteralUtils.hasUnparsedLiteral(defaultExpression)) {
            Skript.error("Invalid default value for the given type: '" + defaultExpressionString + "'");
            defaultExpression = null;
            return false;
        }
        return true;
    }

    /**
     * @return Whether this field is constant.
     */
    public boolean constant() {
        return modifiers.contains(Modifier.CONSTANT);
    }

    /**
     * @return Whether this field is dynamic.
     */
    public boolean dynamic() {
        return modifiers.contains(Modifier.DYNAMIC);
    }

    /**
     * Evaluates the default value of this field.
     *
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

    /**
     * @return The field's name.
     */
    public String name() {
        return name;
    }

    /**
     * @return The classinfo this field accepts/returns.
     */
    public ClassInfo<T> type() {
        return type;
    }

    /**
     * @return Whether this field accepts/returns a single value (or multiple values if false)
     */
    public boolean single() {
        return single;
    }

    /**
     * @return The unparsed string for the default expression.
     */
    public @Nullable String defaultExpressionString() {
        return defaultExpressionString;
    }

    /**
     * @return An unmodifiable set containing the {@link Modifier}s applicable to this field.
     */
    public @Unmodifiable Set<Modifier> modifiers() {
        return Collections.unmodifiableSet(modifiers);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Field<?> other)) return false;

        return this.name.equals(other.name)
                && this.type.equals(other.type)
                && this.single == other.single
                && modifiers.containsAll(other.modifiers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, single, modifiers);
    }

    @Override
    public String toString() {
        return (constant() ? "constant " : "") +
                (dynamic() ? "dynamic " : "") +
                "field '" + name() + "' (" + type().getName().toString(!single) + ")";
    }


}
