package com.sovdee.oopsk.core.generation;

import com.sovdee.oopsk.core.Struct;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

import java.net.URL;
import java.net.URLClassLoader;

public class TemporaryClassManager {
    private ClassLoader disposableClassLoader;

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
        return new ByteBuddy()
                .subclass(Struct.class)
                .name(name)
                .make()
                .load(disposableClassLoader, ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();
    }

    public void unloadAll() {
        // Drop the ClassLoader and all its classes
        disposableClassLoader = null;
        System.gc(); // Suggest garbage collection

        // Create a fresh ClassLoader for new classes
        resetClassLoader();
    }
}
