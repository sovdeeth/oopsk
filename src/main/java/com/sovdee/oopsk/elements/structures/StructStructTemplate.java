package com.sovdee.oopsk.elements.structures;

import ch.njol.skript.ScriptLoader;
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
import ch.njol.skript.lang.ParseContext;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.Functions;
import ch.njol.skript.registrations.Classes;
import ch.njol.skript.util.LiteralUtils;
import ch.njol.skript.util.Utils;
import ch.njol.util.Pair;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Field;
import com.sovdee.oopsk.core.Field.Modifier;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.core.StructTemplate;
import com.sovdee.oopsk.events.DynamicFieldEvalEvent;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converter;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sovdee.oopsk.core.generation.ReflectionUtils.addClassInfo;
import static com.sovdee.oopsk.core.generation.ReflectionUtils.disableRegistrations;
import static com.sovdee.oopsk.core.generation.ReflectionUtils.enableRegistrations;
import static com.sovdee.oopsk.core.generation.ReflectionUtils.getQuickAccessConverters;
import static com.sovdee.oopsk.core.generation.ReflectionUtils.removeClassInfo;
import static com.sovdee.oopsk.core.generation.ReflectionUtils.removeLanguageNode;

@Name("Struct Template")
@Description({
        "Creates a struct template. The template name is case insensitive and has the same restrictions as function names.",
        "Fields are defined in the format '[const[ant]] <fieldname>: <fieldtype> [= %object%]'. Their names are case insensitive and consist of letters, underscores, and spaces. " +
        "No two fields in the same template can have the same name.",
        "The field type can be a single type or a plural type. The default value can be set by adding an optional '= value' at the end of the line." +
        "The default value will be evaluated when the struct is created.",
        "Fields can be marked as constant by adding 'const' or 'constant' at the beginning of the line. Constant fields cannot be changed after the struct is created.",
        "Dynamic fields can be made by adding 'dynamic' to the beginning of the line. Dynamic fields require a default value and will always re-evaluate their value each time they are called. " +
        "This means they cannot be changed directly, but can rely on the values of other fields or even functions.",
        "Converters can be defined in a 'converts to:' section. Each converter is defined in the format '<target type> via %expression%'. " +
        "Note that oopsk cannot generate chained converters reliably, so you should expressly define converters for all target types you wish to convert to.",
        "Be careful when using converters, as they can cause unexpected behavior in all of your scripts if not used properly." +
        "Best practice is to ensure you reload all scripts after defining or modifying struct templates to ensure all converters are registered correctly."
})
@Example("""
    struct message:
        sender: player
        message: string
        const timestamp: date = now
        attachments: objects
        converts to:
            string via this->message
    """)
@Example("""
    struct Vector2:
        x: number
        y: number
        dynamic length: number = sqrt(this->x^2 + this->y^2)
    """)
@Example("""
    struct CustomPlayer
        const player: player
        rank: string = "Member"
        dynamic isAdmin: boolean = whether this->rank is "Admin"
        converts to:
            player via this->player
            location via this->player's location
    """)
@Since("1.0")
public class StructStructTemplate extends Structure {

    static {
        Skript.registerStructure(StructStructTemplate.class, "struct <(" + Functions.functionNamePattern + ")>");
    }

    private StructTemplate template;
    private EntryContainer entryContainer;
    private String name;
    private final Map<Class<?>, Converter<Struct, Object>> converters = new HashMap<>();

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, SkriptParser.ParseResult parseResult, @Nullable EntryContainer entryContainer) {
        MatchResult regex = parseResult.regexes.get(0);
        name = regex.group(1).trim().toLowerCase(Locale.ENGLISH);
        this.entryContainer = entryContainer;

        if (entryContainer == null) {
            return false;
        }

        SectionNode node = entryContainer.getSource();

        if (node.isEmpty()) {
            Skript.error("Struct templates must have at least one field.");
            return false;
        }

        var templateManager = Oopsk.getTemplateManager();

        if (templateManager.getTemplate(name) != null) {
            Skript.error("Struct by the name of " + name + " already exists.");
            return false;
        }

        registerCustomType(); // TODO: safety against name clashes

        return true;
    }

    @Override
    public boolean preLoad() {
        SectionNode node = entryContainer.getSource();
        var templateManager = Oopsk.getTemplateManager();

        List<Field<?>> fields = getFields(node);

        if (fields == null) {
            unregisterCustomType();
            return false;
        }

        template = new StructTemplate(name, fields, customClass);
        return templateManager.addTemplate(template);
    }

    private void registerConverters(SectionNode node) {
        // find
        boolean found = false;
        for (Node child : node) {
            if (child instanceof SectionNode convertersNode) {
                String key = ScriptLoader.replaceOptions(convertersNode.getKey());
                if (key != null && key.trim().equalsIgnoreCase("converts to")) {
                    if (found) {
                        Skript.error("Multiple 'converts to' sections found in struct " + name + ".");
                        return;
                    }
                    parseConverters(convertersNode);
                    found = true;
                    if (this.converters.isEmpty()) {
                        Skript.error("No valid converters found in struct " + name + "'s 'converts to' section.");
                        return;
                    }
                } else {
                    Skript.error("Unexpected section '" + key + "' found in struct " + name + ".");
                    return;
                }
            }
        }

        // register
        if (found) {
            enableRegistrations();
            for (var entry : this.converters.entrySet()) {
                Class<?> targetClass = entry.getKey();
                Converter<Struct, Object> converter = entry.getValue();
                //noinspection unchecked
                Converters.registerConverter((Class<Struct>) customClass, (Class<Object>) targetClass, converter);
            }
            disableRegistrations();
        }

    }

    private static final Pattern CONVERTER_PATTERN = Pattern.compile("([\\w ]+) via (.+)");

    private void parseConverters(@NotNull SectionNode node) {
        for (Node child : node) {
            if (child instanceof SimpleNode simpleNode) {
                String entry = simpleNode.getKey();
                if (entry == null)
                    throw new IllegalStateException("Null node found.");
                // split into type and converter
                Matcher matcher = CONVERTER_PATTERN.matcher(entry);
                if (!matcher.matches()) {
                    Skript.error("Invalid converter entry: " + entry);
                    continue;
                }
                String typeString = matcher.group(1).trim();
                String converterString = matcher.group(2).trim();

                var pair = Utils.getEnglishPlural(typeString);
                ClassInfo<?> targetType = Classes.getClassInfoFromUserInput(pair.getKey());
                if (targetType == null) {
                    Skript.error("Invalid converter target type: " + typeString);
                    continue;
                }

                // parse the converter expression
                var converter = new SkriptParser(converterString, SkriptParser.ALL_FLAGS, ParseContext.DEFAULT).parseExpression(targetType.getC());
                if (converter == null || LiteralUtils.hasUnparsedLiteral(converter)) {
                    Skript.error("Converter expression does not return the declared type of " +  Classes.toString(targetType) + ": '" + converterString + "'");
                    continue;
                }

                // clear quick access converter to ensure there isn't a null entry
                try {
                    //noinspection removal,SuspiciousMethodCalls
                    getQuickAccessConverters().remove(new Pair<>(customClass, targetType.getC()));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                // register the converter
                converters.put(targetType.getC(), new Converter<>() {
                    @Override
                    public @Nullable Object convert(Struct from) {
                        return converter.getSingle(new DynamicFieldEvalEvent(from));
                    }
                });
            }
        }
    }

    public Class<? extends Struct> customClass;
    public ClassInfo<?> customClassInfo;

    // this is a crime against humanity
    private void registerCustomType() {
        var classManager = Oopsk.getClassManager();

        // Create a dynamic subclass
        //noinspection unchecked
        customClass = (Class<? extends Struct>) classManager.createTemporarySubclass("Struct_"+ name.replaceAll("[^a-zA-Z0-9_]", "_"));
        assert customClass != null;

        // hack open the Classes class to allow re-registration
        customClassInfo = addClassInfo(customClass, name);
    }

    private void unregisterCustomType() {
        if (customClassInfo != null) {
            enableRegistrations();
            removeLanguageNode("types." + customClassInfo.getCodeName());
            removeClassInfo(customClassInfo);
            disableRegistrations();
            customClassInfo = null;
            customClass = null;
        }
    }

    @Override
    public boolean load() {
        var templateManager = Oopsk.getTemplateManager();

        // delayed parse so all fields are present
        getParser().setCurrentEvent("parse template", DynamicFieldEvalEvent.class);
        if (!template.parseFields()) {
            templateManager.removeTemplate(template);
            unregisterCustomType();
            return false;
        }
        SectionNode node = entryContainer.getSource();
        registerConverters(node);
        getParser().deleteCurrentEvent();

        return true;
    }

    private static final Pattern fieldPattern = Pattern.compile("(?<const>const(?:ant)? )?(?<dynamic>dynamic)?(?<name>[\\w ]+): (?<type>[\\w ]+?)(?: ?= ?(?<value>.+))?");

    private List<Field<?>> getFields(@NotNull SectionNode node) {
        List<Field<?>> fields = new ArrayList<>();
        for (Node child : node) {
            if (child instanceof SimpleNode simpleNode) {
                // match the field pattern
                String key = ScriptLoader.replaceOptions(simpleNode.getKey());
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
    public void unload() {
        Oopsk.getTemplateManager().removeTemplate(template);
        unregisterCustomType();
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "struct " + name;
    }
}
