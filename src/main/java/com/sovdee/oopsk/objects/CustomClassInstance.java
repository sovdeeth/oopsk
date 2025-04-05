package com.sovdee.oopsk.objects;

import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomClassInstance {

    private final CustomClass parent;
    private final Map<String, FieldInstance> fields;

    public CustomClassInstance(CustomClass customClass) {
        this.parent = customClass;
        this.fields = new HashMap<>();
    }

    public String getName() {
        return parent.getName();
    }

    public @UnknownNullability FieldInstance getField(String name) {
        if (!fields.containsKey(name)) {
            Field field = parent.getField(name);
            if (field == null)
                return null;
            fields.put(name, field.createInstance());
        }
        return fields.get(name);
    }

    @UnmodifiableView
    public Map<String,FieldInstance> getFields() {
        return Collections.unmodifiableMap(fields);
    }

}
