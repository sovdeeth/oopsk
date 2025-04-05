package com.sovdee.oopsk.objects;

import ch.njol.skript.registrations.Classes;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

public class CustomClassManager {
    // TODO local classes
    private static final HashMap<String, CustomClass> GLOBAL_CLASSES = new HashMap<>();

    public static @Nullable CustomClass getClassByName(String name) {
        return GLOBAL_CLASSES.get(name);
    }

    public static void registerClass(CustomClass clazz) {
        GLOBAL_CLASSES.put(clazz.getName(), clazz);
        var classInfo = Classes.getClassInfoNoError(clazz.getName());
        if (classInfo != null) {
            throw new IllegalArgumentException("type name '" + clazz.getName() + "' already in use.");
        }

        Classes.registerClass();

    }

    public static void unregisterClass(String name) {
        GLOBAL_CLASSES.remove(name);
    }
}
