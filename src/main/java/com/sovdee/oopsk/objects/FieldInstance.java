package com.sovdee.oopsk.objects;

public class FieldInstance {

    private Object[] value;
    Field parent;

    public FieldInstance(Field parent) {
        this.parent = parent;
        this.value = parent.getDefaultValue();
    }

    public FieldInstance(Field parent, Object[] value) {
        this.parent = parent;
        this.value = value;
    }

    public void setValue(Object[] value) {
        this.value = value;
    }

    public Object[] getValue() {
        return value;
    }
}
