package com.sovdee.oopsk.objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;
import org.jetbrains.annotations.UnmodifiableView;
import org.skriptlang.skript.lang.script.Script;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class CustomClass {

    private final String name;
    private final Script script;

    private final HashMap<String, Field> fields;

    public CustomClass(String name, Script script) {
        this.name = name;
        this.script = script;
        this.fields = new HashMap<>();
    }

    public CustomClass(String name, Script script, @NotNull List<Field> fields) {
        this.name = name;
        this.script = script;
        this.fields = new HashMap<>();
        for (Field field : fields) {
            this.fields.put(field.getName(), field);
        }
    }

    public String getName() {
        return name;
    }

    public Script getScript() {
        return script;
    }

    @UnmodifiableView
    public Collection<Field> getFields() {
        return Collections.unmodifiableCollection(fields.values());
    }

    public @UnknownNullability Field getField(String name) {
        return fields.get(name);
    }

    public void addFields(Field @NotNull ... fields) {
        for (Field field : fields) {
            this.fields.put(field.getName(), field);
        }
    }

    public void removeFields(String @NotNull ... names) {
        for (String name : names) {
            fields.remove(name);
        }
    }

    public CustomClassInstance createInstance() {
        return new CustomClassInstance(this);
    }


}
