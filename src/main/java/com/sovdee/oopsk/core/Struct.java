package com.sovdee.oopsk.core;

import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.util.ContextlessEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A struct is an instance of a struct template.
 * It contains a reference to the template and a map of field values.
 */
public class Struct {
    private StructTemplate template;
    private final Map<Field<?>, Object[]> fieldValues;

    /**
     * Creates a new struct with the given template and event.
     *
     * @param template The template to create the struct from.
     * @param event    The event to evaluate the default values in.
     * @see StructManager#createStruct(StructTemplate, Event)
     */
    Struct(@NotNull StructTemplate template, @Nullable Event event) {
        this.template = template;
        fieldValues = new HashMap<>();
        for (Field<?> field : template.getFields()) {
            fieldValues.put(field, field.defaultValue(event));
        }
    }

    /**
     * Creates a new struct with the given template and event. Allows a map of initial values to be set in the struct.
     *
     * @param template The template to create the struct from.
     * @param event    The event to evaluate the default values in.
     * @param initialValues The initial values to set in the struct. This is a map of field names to expressions.
     * @see StructManager#createStruct(StructTemplate, Event)
     */
    Struct(@NotNull StructTemplate template, @Nullable Event event, @Nullable Map<String, Expression<?>> initialValues) {
        this.template = template;
        fieldValues = new HashMap<>();
        for (Field<?> field : template.getFields()) {
            // check if the field has an initial value
            if (initialValues != null && initialValues.containsKey(field.name())) {
                Expression<?> expr = initialValues.get(field.name());
                if (expr != null) {
                    // evaluate the expression, ensure the types match, convert if not, and set the field value
                    Object[] value = expr.getArray(event);
                    if (value != null) {
                        Class<?> type = value.getClass().getComponentType();
                        if (!field.type().getC().isAssignableFrom(type)) {
                            // convert the value to the correct type if possible
                            value = Converters.convert(value, field.type().getC());
                        }
                    }
                    // replace null values with empty arrays
                    if (value == null)
                        value = (Object[]) Array.newInstance(field.type().getC(), 0);
                    fieldValues.put(field, value);
                    continue;
                }
            }
            fieldValues.put(field, field.defaultValue(event));
        }
    }

    /**
     * @return The template this struct is based on.
     */
    public StructTemplate getTemplate() {
        return template;
    }

    /**
     * Gets the value of a field in this struct.
     * @param field The field to get the value of.
     * @return The value of the field, or null if the field does not exist in this struct.
     */
    public <T> T[] getFieldValue(Field<T> field) {
        //noinspection unchecked
        return (T[]) fieldValues.get(field);
    }

    /**
     * Sets the value of a field in this struct.
     * @param field The field to set the value of.
     * @param value The value to set the field to. Null values are replaced with an empty array.
     */
    public <T> void setFieldValue(@NotNull Field<T> field, T @Nullable [] value) {
        if (template.hasField(field.name())) {
            if (value == null)
                //noinspection unchecked
                value = (T[]) Array.newInstance(field.type().getC(), 0);
            fieldValues.put(field, value);
        }
    }

    /**
     * Sets the value of a field in this struct.
     * @param field The field to set the value of.
     * @param value The value to set the field to. Null values are replaced with an empty array.
     */
    public <T> void setSingleFieldValue(@NotNull Field<T> field, @Nullable T value) {
        if (template.hasField(field.name())) {
            Object[] valueArray = (Object[]) Array.newInstance(field.type().getC(), value == null ? 0 : 1);
            if (value != null)
                valueArray[0] = value;
            fieldValues.put(field, valueArray);
        }
    }


    /**
     * Resets the value of a field in this struct to its default value.
     * @param field The field to reset the value of.
     * @param event The event to evaluate the default value in.
     */
    public void resetFieldValue(@NotNull Field<?> field, @Nullable Event event) {
        if (template.hasField(field.name())) {
            fieldValues.put(field, field.defaultValue(event));
        }
    }

    /**
     * Updates the fields of this struct to match the given template.
     * @param newTemplate the new template to update to
     * @return whether the struct was modified in a destructive manner
     */
    public boolean updateFromTemplate(@NotNull StructTemplate newTemplate) {
        // remove fields that are not in the new template
        boolean modified = fieldValues.keySet().removeIf(field -> !newTemplate.hasField(field.name()));

        // add new fields from the new template, modify existing fields if necessary
        for (Field<?> newField : newTemplate.getFields()) {
            String name = newField.name();
            // check for existing field to modify
            if (this.template.hasField(name)) {
                Field<?> oldField = this.template.getField(name);
                // if they match, copy the value, replace the field reference
                if (oldField.equals(newField)) {
                    fieldValues.put(newField, fieldValues.remove(oldField));
                } else {
                    // if they don't match, remove the old field and add the new one
                    fieldValues.remove(oldField);
                    fieldValues.put(newField, newField.defaultValue(ContextlessEvent.get()));
                    modified = true;
                }
            } else {
                // new field, add it
                fieldValues.put(newField, newField.defaultValue(ContextlessEvent.get()));
            }
        }
        this.template = newTemplate;
        return modified;
    }

    @Override
    public int hashCode() {
        return Objects.hash(template, fieldValues);
    }

    @Override
    public String toString() {
        return template.getName() + " struct";
    }
}
