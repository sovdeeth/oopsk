package com.sovdee.oopsk.core;

import com.sovdee.oopsk.Oopsk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class TemplateManager {

    // TEMPLATES
    private final Map<String, StructTemplate> templates = new HashMap<>();

    public boolean addTemplate(@NotNull StructTemplate template) {
        if (templates.containsKey(template.getName()))
            return false; // Template with the same name already exists
        templates.put(template.getName(), template);
        // reparent all orphaned structs of this template
        Oopsk.getStructManager().reparentStructs(template);
        return true; // Template added successfully
    }

    public StructTemplate getTemplate(String name) {
        return templates.get(name);
    }

    public boolean removeTemplate(@NotNull StructTemplate template) {
        String name = template.getName();
        if (!templates.containsKey(name))
            return false; // Template with the given name does not exist
        templates.remove(name);
        // mark all structs of this template as orphaned
        Oopsk.getStructManager().orphanStructs(template);
        return true; // Template removed successfully
    }

    public @Unmodifiable Collection<StructTemplate> getTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    public Map<StructTemplate, Field<?>> getFieldsMatching(Predicate<Field<?>> predicate) {
        Map<StructTemplate, Field<?>> matchingFields = new HashMap<>();
        for (StructTemplate template : templates.values()) {
            for (Field<?> field : template.getFields().values()) {
                if (predicate.test(field)) {
                    matchingFields.put(template, field);
                }
            }
        }
        return matchingFields;
    }

}
