package com.sovdee.oopsk.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A struct contains a collection of typed fields.
 */
public class StructTemplate {
    private final String name;
    private final Map<String, Field<?>> fields;

    public StructTemplate(String name, List<Field<?>> fields) {
        this.name = name;
        this.fields = new HashMap<>();
        for (Field<?> field : fields) {
            this.fields.put(field.name(), field);
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Field<?>> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    public Field<?> getField(String name) {
        return fields.get(name);
    }

    public boolean hasField(String name) {
        return fields.containsKey(name);
    }

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
