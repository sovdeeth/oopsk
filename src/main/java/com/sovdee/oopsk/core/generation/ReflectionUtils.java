package com.sovdee.oopsk.core.generation;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class ReflectionUtils {

    private static final Field tempClassInfosField;
    private static final Field exactClassInfosField;
    private static final Field classInfosByCodeNameField;
    private static final Field acceptRegistrationsField;
    private static final Field localizedLanguageField;
    private static final Field classInfosField;
    private static final Method sortClassInfosMethod;

    static {
        try {
            // Get the fields once during class initialization
            tempClassInfosField = Classes.class.getDeclaredField("tempClassInfos");
            exactClassInfosField = Classes.class.getDeclaredField("exactClassInfos");
            classInfosByCodeNameField = Classes.class.getDeclaredField("classInfosByCodeName");
            acceptRegistrationsField = Skript.class.getDeclaredField("acceptRegistrations");
            localizedLanguageField = Language.class.getDeclaredField("localizedLanguage");
            classInfosField = Classes.class.getDeclaredField("classInfos");

            // Get the method
            sortClassInfosMethod = Classes.class.getDeclaredMethod("sortClassInfos");


            // Make them accessible
            tempClassInfosField.setAccessible(true);
            exactClassInfosField.setAccessible(true);
            classInfosByCodeNameField.setAccessible(true);
            acceptRegistrationsField.setAccessible(true);
            localizedLanguageField.setAccessible(true);
            classInfosField.setAccessible(true);
            sortClassInfosMethod.setAccessible(true);

        } catch (Exception e) {
            throw new RuntimeException("Failed to access fields", e);
        }
    }

    public static void enableRegistrations() {
        setFieldValue(true);
    }

    public static void disableRegistrations() {
        setFieldValue(false);
    }

    private static void setFieldValue(boolean value) {
        try {
            acceptRegistrationsField.set(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to modify acceptRegistrations field", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static List<ClassInfo<?>> getTempClassInfos() throws Exception {
        return (List<ClassInfo<?>>) tempClassInfosField.get(null);
    }

    @SuppressWarnings("unchecked")
    public static HashMap<Class<?>, ClassInfo<?>> getExactClassInfos() throws Exception {
        return (HashMap<Class<?>, ClassInfo<?>>) exactClassInfosField.get(null);
    }

    @SuppressWarnings("unchecked")
    public static HashMap<String, ClassInfo<?>> getClassInfosByCodeName() throws Exception {
        return (HashMap<String, ClassInfo<?>>) classInfosByCodeNameField.get(null);
    }

    public static void addLanguageNode(String key, String value) {
        try {
            @SuppressWarnings("unchecked")
            HashMap<String, String> langMap = (HashMap<String, String>) localizedLanguageField.get(null);
            System.out.println("Adding language node: " + key + " -> " + value);
            langMap.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add language node.", e);
        }
    }

    public static void removeLanguageNode(String key) {
        try {
            @SuppressWarnings("unchecked")
            HashMap<String, String> langMap = (HashMap<String, String>) localizedLanguageField.get(null);
            System.out.println("Removing language node: " + key + " (was " + langMap.get(key) + ")");
            langMap.remove(key);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add language node.", e);
        }
    }

    public static void resortClassInfos(ClassInfo<?>... exclude) throws Exception {
        var tempClassInfos = getTempClassInfos();
        Collections.addAll(tempClassInfos, (ClassInfo<?>[]) classInfosField.get(null));
        if (exclude != null && exclude.length > 0)
            tempClassInfos.removeAll(List.of(exclude));
        classInfosField.set(null, null);
        sortClassInfos();
    }

    public static void sortClassInfos() throws Exception {
        sortClassInfosMethod.invoke(null);
    }

    public static void addClassInfo(ClassInfo<?> classInfo) {
        enableRegistrations();
        Classes.registerClass(classInfo);
        try {
            resortClassInfos();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Registered custom struct class: " + classInfo.getName());
        disableRegistrations();
    }

    // Remove from all three collections
    public static void removeClassInfo(ClassInfo<?> classInfo) {
        try {
            getExactClassInfos().remove(classInfo.getC());
            getClassInfosByCodeName().remove(classInfo.getCodeName());
            resortClassInfos(classInfo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove classinfo.", e);
        }
    }

}
