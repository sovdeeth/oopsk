package com.sovdee.oopsk.core.generation;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.localization.Language;
import ch.njol.skript.registrations.Classes;
import com.sovdee.oopsk.core.Struct;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.ConverterInfo;
import org.skriptlang.skript.lang.converter.Converters;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ReflectionUtils {

    private static final Field tempClassInfosField;
    private static final Field exactClassInfosField;
    private static final Field classInfosByCodeNameField;
    private static final Field acceptRegistrationsField;
    private static final Field localizedLanguageField;
    private static final Field classInfosField;
    private static final Field convertersField;
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
            convertersField = Converters.class.getDeclaredField("CONVERTERS");

            // Get the method
            sortClassInfosMethod = Classes.class.getDeclaredMethod("sortClassInfos");


            // Make them accessible
            tempClassInfosField.setAccessible(true);
            exactClassInfosField.setAccessible(true);
            classInfosByCodeNameField.setAccessible(true);
            acceptRegistrationsField.setAccessible(true);
            localizedLanguageField.setAccessible(true);
            classInfosField.setAccessible(true);
            convertersField.setAccessible(true);
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
    public static List<ConverterInfo<?,?>> getConverters() throws Exception {
        return (List<ConverterInfo<?, ?>>) convertersField.get(null);
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
            langMap.put(key, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to add language node.", e);
        }
    }

    public static void removeLanguageNode(String key) {
        try {
            @SuppressWarnings("unchecked")
            HashMap<String, String> langMap = (HashMap<String, String>) localizedLanguageField.get(null);
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

    public static ClassInfo<? extends Struct> addClassInfo(Class<? extends Struct> customClass, String name) {

        // get the classinfo if it exists
        String codeName = name.toLowerCase(Locale.ENGLISH) + "struct";
        codeName = codeName.replaceAll("_", "underscore");

        //noinspection unchecked
        ClassInfo<? extends Struct> customClassInfo = (ClassInfo<? extends Struct>) Classes.getClassInfoNoError(codeName);
        if (customClassInfo != null) {
            if (!customClassInfo.getC().equals(customClass)) {
                // conflict, remove the old one and try again
                removeClassInfo(customClassInfo);
                return addClassInfo(customClass, name);
            }
            // already exists, adopt it.
            return customClassInfo;
        }

        // get by class
        customClassInfo = Classes.getExactClassInfo(customClass);
        if (customClassInfo != null) {
            if (!customClassInfo.getCodeName().equals(codeName)) {
                // conflict, remove the old one and try again
                removeClassInfo(customClassInfo);
                return addClassInfo(customClass, name);
            }
            // already exists, adopt it.
            return customClassInfo;
        }

        addLanguageNode("types." + codeName , name + " struct");
        customClassInfo = new ClassInfo<>(customClass, codeName)
                .user(name + " structs?( types?)?");

        enableRegistrations();
        Classes.registerClass(customClassInfo);
        try {
            resortClassInfos();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // converters
        Converter<Struct, Struct> castingConverter = struct -> {
            if (customClass.isInstance(struct))
                return customClass.cast(struct);
            return null;
        };
        //noinspection unchecked
        Converters.registerConverter(Struct.class, (Class<Struct>) customClass, castingConverter);

        // chained converters
        try {
            getConverters().forEach(converterInfo -> {
                if (converterInfo.getTo().equals(Struct.class))
                    //noinspection unchecked
                    Converters.registerConverter(converterInfo.getFrom(), (Class<Struct>) customClass, from -> {
                        //noinspection unchecked
                        Struct middle = ((Converter<Object, Struct>) converterInfo.getConverter()).convert(from);
                        if (middle == null)
                            return null;
                        return castingConverter.convert(middle);
                    }, converterInfo.getFlags());
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        disableRegistrations();
        return customClassInfo;
    }

    // Remove from all three collections
    public static void removeClassInfo(ClassInfo<?> classInfo) {
        try {
            Class<?> customClass = classInfo.getC();
            List<ConverterInfo<?,?>> toRemove = new ArrayList<>();
            var converters = getConverters();
            for (var converterInfo : converters) {
                if (converterInfo.getTo().equals(customClass))
                    toRemove.add(converterInfo);
            }
            converters.removeAll(toRemove);

            getExactClassInfos().remove(classInfo.getC());
            getClassInfosByCodeName().remove(classInfo.getCodeName());
            resortClassInfos(classInfo);
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove classinfo.", e);
        }
    }

}
