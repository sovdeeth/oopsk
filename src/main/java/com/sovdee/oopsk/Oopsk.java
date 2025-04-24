package com.sovdee.oopsk;

import ch.njol.skript.Skript;
import ch.njol.skript.SkriptAddon;
import ch.njol.skript.bstats.bukkit.Metrics;
import com.sovdee.oopsk.core.StructManager;
import com.sovdee.oopsk.core.TemplateManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.logging.Logger;

public final class Oopsk extends JavaPlugin {

    private static Oopsk instance;
    private static SkriptAddon addon;
    private static StructManager structManager;
    private static TemplateManager templateManager;
    private static Logger logger;

    public static Oopsk getInstance() {
        return instance;
    }

    public static SkriptAddon getAddonInstance() {
        return addon;
    }

    public static StructManager getStructManager() {
        return structManager;
    }

    public static TemplateManager getTemplateManager() {
        return templateManager;
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warning(String message) {
        logger.warning(message);
    }

    public static void severe(String message) {
        logger.severe(message);
    }

    public static void debug(String message) {
        if (Skript.debug()) {
            logger.info(message);
        }
    }

    @Override
    public void onEnable() {
        instance = this;
        addon = Skript.registerAddon(this);
        addon.setLanguageFileDirectory("lang");
        logger = this.getLogger();
        structManager = new StructManager();
        templateManager = new TemplateManager();
        try {
            addon.loadClasses("com.sovdee.oopsk");
        } catch (IOException e) {
            e.printStackTrace();
        }
        int pluginId = 18916;
        Metrics metrics = new Metrics(this, pluginId);
        Oopsk.info("oopsk has been enabled.");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        Oopsk.info("oopsk has been disabled.");
        structManager = null;
        templateManager = null;
        instance = null;
        addon = null;
        logger = null;
    }
}
