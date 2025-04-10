package com.sovdee.oopsk.core;

import ch.njol.skript.lang.Expression;
import com.sovdee.oopsk.Oopsk;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * StructManager is responsible for managing the lifecycle of structs.
 * All struct creation and deletion should be done through this class, though it's rare for any structs to be manually deleted.
 */
public class StructManager {

    private final Map<StructTemplate, Set<Struct>> activeStructs = new HashMap<>();
    private final Map<String, Set<Struct>> orphanedStructs = new HashMap<>();

    /**
     * Creates a new struct with the given template and event.
     *
     * @param template The template to create the struct from.
     * @param event    The event to evaluate the default values in.
     * @return The created struct.
     */
    public Struct createStruct(StructTemplate template, @Nullable Event event) {
        return createStruct(template, event, null);
    }

    /**
     * Creates a new struct with the given template and event. Allows a map of initial values to be set in the struct.
     *
     * @param template The template to create the struct from.
     * @param event    The event to evaluate the default values in.
     * @param initialValues The initial values to set in the struct. This is a map of field names to expressions.
     * @return The created struct.
     */
    public Struct createStruct(StructTemplate template, @Nullable Event event, @Nullable Map<String, Expression<?>> initialValues) {
        Struct struct = new Struct(template, event, initialValues);
        activeStructs.computeIfAbsent(template, k -> Collections.newSetFromMap(new WeakHashMap<>())).add(struct);
        return struct;
    }

    /**
     * Deletes a struct from the active structs list.
     *
     * @param struct The struct to delete.
     */
    public void deleteStruct(@NotNull Struct struct) {
        Set<Struct> structs = activeStructs.get(struct.getTemplate());
        if (structs != null) {
            structs.remove(struct);
            if (structs.isEmpty()) {
                activeStructs.remove(struct.getTemplate());
            }
        }
    }

    /**
     * Orphans the structs of the given template. This should be called when a template is removed.
     *
     * @param template The template to orphan the structs of.
     */
    public void orphanStructs(StructTemplate template) {
        Set<Struct> structs = activeStructs.remove(template);
        if (structs != null) {
            orphanedStructs.computeIfAbsent(template.getName(), k -> Collections.newSetFromMap(new WeakHashMap<>())).addAll(structs);
        }
    }

    /**
     * Reparents the structs of the given template. This should be called when a template is added.
     * Matches are made by template name, so if a template with the same name is added, the structs will be reparented to the new template.
     *
     * @param template The template to reparent the structs of.
     */
    public void reparentStructs(@NotNull StructTemplate template) {
        Set<Struct> structs = orphanedStructs.remove(template.getName());
        if (structs == null)
            return;
        boolean modified = false;
        for (Struct struct : structs) {
            modified |= struct.updateFromTemplate(template);
        }
        if (modified)
            Oopsk.warning("Existing structs of template '" + template.getName() + "' have had their fields modified to match the new template. This may have caused data loss.");
        activeStructs.computeIfAbsent(template, k -> Collections.newSetFromMap(new WeakHashMap<>())).addAll(structs);

    }

}
