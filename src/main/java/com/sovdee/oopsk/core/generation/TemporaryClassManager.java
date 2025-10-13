package com.sovdee.oopsk.core.generation;

import com.sovdee.oopsk.core.Struct;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

public class TemporaryClassManager {
    private ClassLoader disposableClassLoader;

    private Map<String, Class<?>> createdClasses = new HashMap<>();

    public TemporaryClassManager() {
        resetClassLoader();
    }

    private void resetClassLoader() {
        // Create a new child ClassLoader
        this.disposableClassLoader = new URLClassLoader(
                new URL[0],
                Struct.class.getClassLoader()
        );
    }

    public Class<?> createTemporarySubclass(String name) {
        if (createdClasses.containsKey(name)) {
            return createdClasses.get(name);
        }
        var c = new ByteBuddy()
                .subclass(Struct.class)
                .name(name)
                .make()
                .load(disposableClassLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
        createdClasses.put(name, c);
        return c;
    }

}
