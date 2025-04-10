package com.sovdee.oopsk.core;

import com.sovdee.oopsk.Oopsk;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class StructManager {


    private final Map<StructTemplate, Set<Struct>> activeStructs = new HashMap<>();
    private final Map<String, Set<Struct>> orphanedStructs = new HashMap<>();

    public Struct createStruct(StructTemplate template, @Nullable Event event) {
        Struct struct = new Struct(template, event);
        activeStructs.computeIfAbsent(template, k -> Collections.newSetFromMap(new WeakHashMap<>())).add(struct);
        return struct;
    }

    public void deleteStruct(Struct struct) {
        Set<Struct> structs = activeStructs.get(struct.getTemplate());
        if (structs != null) {
            structs.remove(struct);
            if (structs.isEmpty()) {
                activeStructs.remove(struct.getTemplate());
            }
        }
    }

    public void orphanStructs(StructTemplate template) {
        Set<Struct> structs = activeStructs.remove(template);
        if (structs != null) {
            for (Struct struct : structs) {
                orphanedStructs.computeIfAbsent(template.getName(), k -> Collections.newSetFromMap(new WeakHashMap<>())).add(struct);
            }
        }
    }

    public void reparentStructs(StructTemplate template) {
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
