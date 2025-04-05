package com.sovdee.oopsk.objects;

public class Field {
    private final String name;

    private final Object[] defaultValue;

    public Field(String name) {
        this(name, new Object[0]);
    }

    public Field(String name, Object[] value) {
        this.name = name;
        this.defaultValue = value;
    }

    public String getName() {
        return name;
    }

    public Object[] getDefaultValue() {
        return defaultValue;
    }

    public FieldInstance createInstance() {
        return new FieldInstance(this);
    }
}
