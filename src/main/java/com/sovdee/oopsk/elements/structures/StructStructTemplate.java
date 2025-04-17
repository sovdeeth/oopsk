package com.sovdee.oopsk.elements.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.Utils;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Field;
import com.sovdee.oopsk.core.Field.Modifier;
import com.sovdee.oopsk.core.StructTemplate;
import com.sovdee.oopsk.events.DynamicFieldEvalEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Name("Struct Template")
@Description({
        "Creates a struct template. The template name is case insensitive and has the same restrictions as function names.",
        "Fields are defined in the format '[const[ant]] <fieldname>: <fieldtype> [= %object%]'. Their names are case insensitive and consist of letters, underscores, and spaces. " +
        "No two fields in the same template can have the same name.",
        "The field type can be a single type or a plural type. The default value can be set by adding an optional '= value' at the end of the line." +
        "The default value will be evaluated when the struct is created.",
        "Fields can be marked as constant by adding 'const' or 'constant' at the beginning of the line. Constant fields cannot be changed after the struct is created.",
        "Dynamic fields can be made by adding 'dynamic' to the beginning of the line. Dynamic fields require a default value and will always re-evaluate their value each time they are called. " +
        "This means they cannot be changed directly, but can rely on the values of other fields or even functions."
})
@Example("""
    struct message:
        sender: player
        message: string
        const timestamp: date = now
        attachments: objects
    """)
@Example("""
    struct Vector2:
        x: number
        y: number
        dynamic length: number = sqrt(this->x^2 + this->y^2)
    """)
@Since("1.0")
public class StructStructTemplate extends Structure {

    static {
        Skript.registerStructure(StructStructTemplate.class, "struct <(" + Functions.functionNamePattern + ")>");
    }

    private StructTemplate template;
    private EntryContainer entryContainer;
    private String name;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        MatchResult regex = parseResult.regexes.get(0);
        name = regex.group(1).trim().toLowerCase(Locale.ENGLISH);
        this.entryContainer = entryContainer;

        return entryContainer != null;
    }

    @Override
    public boolean preLoad() {
        SectionNode node = entryContainer.getSource();

        if (node.isEmpty()) {
            Skript.error("Struct templates must have at least one field.");
            return false;
        }

        List<Field<?>> fields = getFields(node);

        if (fields == null)
            return false;

        var templateManager = Oopsk.getTemplateManager();

        if (templateManager.getTemplate(name) != null) {
            Skript.error("Struct by the name of " + name + " already exists.");
            return false;
        }
        template = new StructTemplate(name, fields);
        if (!templateManager.addTemplate(template))
            return false;

        // delayed parse so all fields are present
        getParser().setCurrentEvent("parse template", DynamicFieldEvalEvent.class);
        if (!template.parseFields()) {
            templateManager.removeTemplate(template);
            return false;
        }
        getParser().deleteCurrentEvent();

        return true;
    }


    private static final Pattern fieldPattern = Pattern.compile("(?<const>const(?:ant)? )?(?<dynamic>dynamic)?(?<name>[\\w ]+): (?<type>[\\w ]+?)(?: ?= ?(?<value>.+))?");

    private List<Field<?>> getFields(@NotNull SectionNode node) {
        List<Field<?>> fields = new ArrayList<>();
        for (Node child : node) {
            if (child instanceof SimpleNode simpleNode) {
                // match the field pattern
                String key = simpleNode.getKey();
                Matcher matcher;
                if (key == null || !(matcher = fieldPattern.matcher(key)).matches()) {
                    Skript.error("invalid field: " + key);
                    return null;
                }

                // get modifiers (TODO: better parsing than regex?)
                Set<Modifier> modifiers = EnumSet.noneOf(Modifier.class);

                if (matcher.group("const") != null)
                    modifiers.add(Modifier.CONSTANT);

                if (matcher.group("dynamic") != null) {
                    if (modifiers.contains(Modifier.CONSTANT))
                        Skript.warning("All dynamic fields are already constant, so declaring them as both is unnecessary.");
                    modifiers.add(Modifier.DYNAMIC);
                    modifiers.add(Modifier.CONSTANT);
                }

                // parse the field name
                String fieldName = matcher.group("name").trim().toLowerCase(Locale.ENGLISH);
                if (fieldName.isEmpty()) {
                    Skript.error("Field name cannot be empty.");
                    return null;
                }
                // check if the field name is already taken
                if (fields.stream().anyMatch(field -> field.name().equalsIgnoreCase(fieldName))) {
                    Skript.error("Field name '" + fieldName + "' is already taken.");
                    return null;
                }

                // parse the field type
                var pair = Utils.getEnglishPlural(matcher.group("type").trim());
                boolean isPlural = pair.getValue();
                ClassInfo<?> fieldType = Classes.getClassInfoFromUserInput(pair.getKey());
                if (fieldType == null) {
                    Skript.error("invalid field type: " + matcher.group("type").trim());
                    return null;
                }

                String defaultValueString = matcher.group("value");
                if (defaultValueString == null && modifiers.contains(Modifier.DYNAMIC)) {
                    Skript.error("Dynamic fields require a default value to be given.");
                    return null;
                }

                //noinspection rawtypes,unchecked
                fields.add(new Field<>(fieldName, (ClassInfo) fieldType, !isPlural, defaultValueString, modifiers));
            }
        }
        return fields;
    }
    @Override
    public boolean load() {

        return true;
    }

    @Override
    public void unload() {
        Oopsk.getTemplateManager().removeTemplate(template);
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "struct " + name;
    }
}
