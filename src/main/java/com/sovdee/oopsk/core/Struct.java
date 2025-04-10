package com.sovdee.oopsk.core;

import ch.njol.skript.lang.util.ContextlessEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Struct {
    private StructTemplate template;
    private final Map<Field<?>, Object[]> fieldValues;

    public Struct(@NotNull StructTemplate template, @Nullable Event event) {
        this.template = template;
        fieldValues = new HashMap<>();
        for (Field<?> field : template.getFields().values()) {
            fieldValues.put(field, field.defaultValue(event));
        }
    }

    public StructTemplate getTemplate() {
        return template;
    }

    public Object[] getFieldValue(Field<?> field) {
        return fieldValues.get(field);
    }

    public void setFieldValue(@NotNull Field<?> field, Object[] value) {
        if (value == null)
            value = (Object[]) Array.newInstance(field.type().getC(), 0);
        if (template.hasField(field.name())) {
            fieldValues.put(field, value);
        }
    }

    public void resetFieldValue(@NotNull Field<?> field, @Nullable Event event) {
        if (template.hasField(field.name())) {
            fieldValues.put(field, field.defaultValue(event));
        }
    }

    public boolean updateFromTemplate(StructTemplate newTemplate) {
        // remove fields that are not in the new template
        boolean modified = fieldValues.keySet().removeIf(field -> !newTemplate.hasField(field.name()));

        // add new fields from the new template, modify existing fields if necessary
        for (Field<?> newField : newTemplate.getFields().values()) {
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
