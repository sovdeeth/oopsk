package com.sovdee.oopsk.elements.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.ClassInfo;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Field;
import com.sovdee.oopsk.core.StructTemplate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        List<Field<?>> fields = getFields(node);

        if (fields == null)
            return false;

        var templateManager = Oopsk.getTemplateManager();

        if (templateManager.getTemplate(name) != null) {
            Skript.error("Struct by the name of " + name + " already exists.");
            return false;
        }
        template = new StructTemplate(name, fields);

        return templateManager.addTemplate(template);
    }


    private static final Pattern fieldPattern = Pattern.compile("([\\w ]+): ([\\w ]+?)(?: ?= ?(.+))?");

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
                // parse the field name
                String fieldName = matcher.group(1).trim().toLowerCase(Locale.ENGLISH);
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
                var pair = Utils.getEnglishPlural(matcher.group(2).trim());
                boolean isPlural = pair.getValue();
                ClassInfo<?> fieldType = Classes.getClassInfoFromUserInput(pair.getKey());
                if (fieldType == null) {
                    Skript.error("invalid field type: " + matcher.group(2).trim());
                    return null;
                }
                // parse the default value
                Expression<?> defaultValue = null;
                if (matcher.group(3) != null) {
                    //noinspection unchecked
                    defaultValue = new SkriptParser(matcher.group(3), SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(fieldType.getC());
                    if (defaultValue == null || LiteralUtils.hasUnparsedLiteral(defaultValue)) {
                        Skript.error("Invalid default value for the given type: '" + matcher.group(3) + "'");
                        return null;
                    }
                }
                //noinspection rawtypes,unchecked
                fields.add(new Field<>(fieldName, (ClassInfo) fieldType, !isPlural, defaultValue));
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
