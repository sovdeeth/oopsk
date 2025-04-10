package com.sovdee.oopsk.core;

import com.sovdee.oopsk.Oopsk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * TemplateManager is responsible for managing struct templates.
 * It allows adding, removing, and retrieving templates, as well as finding fields matching a given predicate.
 */
public class TemplateManager {

    private final Map<String, StructTemplate> templates = new HashMap<>();

    /**
     * Adds a new template to the manager. Attempts to reparent all orphaned structs that match this template's name.
     *
     * @param template The template to add.
     * @return True if the template was added successfully, false if a template with the same name already exists.
     */
    public boolean addTemplate(@NotNull StructTemplate template) {
        if (templates.containsKey(template.getName()))
            return false; // Template with the same name already exists
        templates.put(template.getName(), template);
        // reparent all orphaned structs of this template
        Oopsk.getStructManager().reparentStructs(template);
        return true; // Template added successfully
    }

    /**
     * Retrieves a template by name.
     *
     * @param name The name of the template to retrieve.
     * @return The template, or null if it does not exist.
     */
    public StructTemplate getTemplate(String name) {
        return templates.get(name);
    }

    /**
     * Removes a template from the manager. This will orphan all structs of this template.
     *
     * @param template The template to remove.
     */
    public void removeTemplate(@NotNull StructTemplate template) {
        String name = template.getName();
        if (!templates.containsKey(name))
            return; // Template with the given name does not exist
        templates.remove(name);
        // mark all structs of this template as orphaned
        Oopsk.getStructManager().orphanStructs(template);
    }

    /**
     * Retrieves all templates managed by this manager.
     *
     * @return An unmodifiable collection of all templates.
     */
    public @Unmodifiable Collection<StructTemplate> getTemplates() {
        return Collections.unmodifiableCollection(templates.values());
    }

    /**
     * Finds all fields in all templates that match the given predicate.
     *
     * @param predicate The predicate to match fields against.
     * @return A map of templates to matching fields.
     */
    public Map<StructTemplate, Set<Field<?>>> getFieldsMatching(Predicate<Field<?>> predicate) {
        Map<StructTemplate, Set<Field<?>>> matchingFields = new HashMap<>();
        for (StructTemplate template : templates.values()) {
            for (Field<?> field : template.getFields()) {
                if (predicate.test(field)) {
                    matchingFields.computeIfAbsent(template, k -> Collections.newSetFromMap(new HashMap<>()))
                            .add(field);
                }
            }
        }
        return matchingFields;
    }

}
