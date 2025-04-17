package com.sovdee.oopsk.core;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A struct template contains a collection of typed fields.
 */
public class StructTemplate {
    private final String name;
    private final Map<String, Field<?>> fields;

    /**
     * Creates a new struct template with the given name and fields.
     *
     * @param name   The name of the template.
     * @param fields The fields of the template.
     */
    public StructTemplate(String name, @NotNull List<Field<?>> fields) {
        this.name = name;
        this.fields = new HashMap<>();
        for (Field<?> field : fields) {
            this.fields.put(field.name(), field);
        }
    }

    /**
     * @return The name of this template.
     */
    public String getName() {
        return name;
    }

    /**
     * Parses all the default value expressions for this struct. Prints errors.
     * @return true if no errors were encountered. False otherwise.
     */
    public boolean parseFields() {
        for (Field<?> field : fields.values()) {
            if (!field.parseDefaultValueExpression())
                return false;
        }
        return true;
    }

    /**
     * @return The fields of this template. Unmodifiable.
     */
    public Collection<Field<?>> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    /**
     * Gets a field by name.
     * @param name The name of the field.
     * @return The field, or null if it does not exist.
     */
    public Field<?> getField(String name) {
        return fields.get(name);
    }

    /**
     * Checks if this template has a field with the given name.
     * @param name The name of the field.
     * @return True if the field exists, false otherwise.
     */
    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

    /**
     * Checks if this template has a field with the given name and type.
     * @param name The name of the field.
     * @param type The type of the field.
     * @return True if the field exists and has the given type, false otherwise.
     */
    public boolean hasField(String name, Class<?> type) {
        Field<?> field = fields.get(name);
        return field != null && field.type().getC().isAssignableFrom(type);
    }

    @Override
    public String toString() {
        return "StructTemplate{" +
                "name='" + name + '\'' +
                ", fields=" + fields +
                '}';
    }

}
