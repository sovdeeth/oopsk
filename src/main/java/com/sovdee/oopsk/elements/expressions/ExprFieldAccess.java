package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.classes.Changer.ChangeMode;
import ch.njol.skript.config.Node;
import ch.njol.skript.doc.Description;
import ch.njol.skript.doc.Example;
import ch.njol.skript.doc.Name;
import ch.njol.skript.doc.Since;
import ch.njol.skript.expressions.base.PropertyExpression;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.registrations.Classes;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.Oopsk;
import com.sovdee.oopsk.core.Field;
import com.sovdee.oopsk.core.Struct;
import com.sovdee.oopsk.core.StructTemplate;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.converter.Converters;
import org.skriptlang.skript.log.runtime.SyntaxRuntimeErrorProducer;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

@Name("Struct Field Access")
@Description("Access a field of a struct. The field name is case insensitive. Non-constant fields can be set, reset, or deleted.")
@Example("set the field name of {_struct} to \"test\"")
@Example("set {_struct}'s name field to \"test\"")
@Example("reset {_struct}->name")
@Since("1.0")
public class ExprFieldAccess extends PropertyExpression<Struct, Object> implements SyntaxRuntimeErrorProducer {

    static {
        Skript.registerExpression(ExprFieldAccess.class, Object.class, ExpressionType.PROPERTY,
                "[the] field <[\\w ]+> [of] %struct%",
                "%struct%'[s] <[\\w ]+> field",
                "%struct%[ ]->[ ]<[\\w ]+>");
    }

    String fieldName;
    Node node;

    Map<StructTemplate, Field<?>> possibleFields;
    Class<?>[] returnTypes;
    Class<?> returnType;
    boolean isAnyFieldSingle;
    boolean isAnyFieldPlural;
    boolean areAllFieldsConstant;

    @Override
    public boolean init(Expression<?>[] expressions, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        //noinspection unchecked
        setExpr((Expression<Struct>) expressions[0]);
        node = getParser().getNode();
        fieldName = parseResult.regexes.get(0).group(0);
        if (fieldName == null) {
            Skript.error("Field name cannot be null.");
            return false;
        }
        fieldName = fieldName.trim().toLowerCase(Locale.ENGLISH);
        if (!updateFieldGuesses()) {
            Skript.error("No field with name '" + fieldName + "' found.");
            return false;
        }
        return true;
    }

    /**
     * Update the return type / isSingle guesses based on existing fields.
     * @return True if a field was found, false otherwise.
     */
    private boolean updateFieldGuesses() {
        // get all possible fields that this could be accessing
        var fieldSetMap = Oopsk.getTemplateManager()
                .getFieldsMatching((field -> field.name().equalsIgnoreCase(fieldName)));
        if (fieldSetMap.isEmpty()) {
            return false;
        }
        // collapse setMap to a 1:1 map of template -> field
        // since our predicate is based on name, there should never be a template with multiple fields that match.
        possibleFields = new WeakHashMap<>();
        for (Map.Entry<StructTemplate, Set<Field<?>>> entry : fieldSetMap.entrySet()) {
            StructTemplate template = entry.getKey();
            Set<Field<?>> fields = entry.getValue();
            // if there are multiple fields, pick the first one
            possibleFields.put(template, fields.stream().findFirst().orElse(null));
        }
        // use super type of all possible fields
        returnTypes = possibleFields.values().stream()
                .map((field -> field.type().getC()))
                .distinct()
                .toArray(Class<?>[]::new);
        returnType = Classes.getSuperClassInfo(returnTypes).getC();
        // plurality/constant checks
        areAllFieldsConstant = false;
        isAnyFieldSingle = false;
        isAnyFieldPlural = false;
        for (Field<?> field : possibleFields.values()) {
            if (field.single()) {
                isAnyFieldSingle = true;
            } else {
                isAnyFieldPlural = true;
            }
            areAllFieldsConstant |= field.constant();
        }
        return true;
    }

    @Override
    protected Object[] get(Event event, Struct[] source) {
        if (source.length == 0)
            return null;
        // get actual struct and template
        Struct struct = source[0];
        StructTemplate template = struct.getTemplate();
        // get the field
        Field<?> field = template.getField(fieldName);
        if (field == null) {
            error("Field " + fieldName + " not found in struct " + template.getName());
            return null;
        }
        var value = struct.getFieldValue(field);
        // check type is accurate to what we claimed
        Class<?> type = value.getClass().getComponentType();
        if (Arrays.stream(returnTypes).noneMatch(returnType -> returnType.isAssignableFrom(type))) {
            //noinspection unchecked,rawtypes
            var converted = Converters.convert(value, (Class[]) returnTypes, returnType);
            if (converted != null) {
                // if the field is not valid, but the value is convertible, return the converted value
                return converted;
            }
            // if the field is not valid, and the value is not convertible, error
            error("The " + field + " of " + struct + " is not the same type it claimed to be at parse time. " +
                    "This likely was caused by template changes. Consider reloading this script.");
            return null;
        }
        // get the value
        return value;
    }

    @Override
    public Class<?> @Nullable [] acceptChange(ChangeMode mode) {
        // if the field is constant, we cannot change it
        if (areAllFieldsConstant) {
            Skript.error("Cannot change a constant field.");
            return null;
        }

        // reset and delete are always possible
        if (mode == ChangeMode.RESET || mode == ChangeMode.DELETE) {
            return new Class<?>[0];
        }

        if (mode == ChangeMode.SET) {
            // return all return types
            if (isAnyFieldPlural)
                // if any field is plural, return array types
                return Arrays.stream(returnTypes).map(Class::arrayType).toArray(Class<?>[]::new);
            return returnTypes;
        }
        return null;
    }

    @Override
    public void change(Event event, Object @Nullable [] delta, ChangeMode mode) {
        // get actual struct and template
        Struct struct = getExpr().getSingle(event);
        if (struct == null)
            return;
        StructTemplate template = struct.getTemplate();
        // get the field
        Field<?> field = template.getField(fieldName);
        if (field == null) {
            error("Field " + fieldName + " not found in struct " + template.getName());
            return;
        }
        if (field.constant()) {
            error("Field " + fieldName + " of " + struct + " is constant and cannot be changed.");
            return;
        }

        switch (mode) {
            case RESET -> // reset the field to its default value
                    struct.resetFieldValue(field, event);
            case DELETE -> // delete the field value
                    struct.setFieldValue(field, null);
            case SET -> { // set the field value
                if (delta == null || delta.length == 0) return;
                Class<?> fieldClass = field.type().getC();
                // check if the value is an array
                if (field.single()) {
                    // if delta is an array, error
                    if (delta.length > 1) {
                        error("Cannot set " + field + " of " + struct + " to multiple things. It is a single field.");
                        return;
                    }

                    // ensure the value is of the correct type
                    delta[0] = Converters.convert(delta[0], fieldClass);
                    if (delta[0] == null) {
                        // if it is not convertible, error
                        error("Cannot set " + field + " of " + struct + " to the given value. It is not " +
                                field.type().getName().withIndefiniteArticle() + ".");
                        return;
                    }
                    Object[] typedDelta = (Object[]) Array.newInstance(fieldClass, 1);
                    typedDelta[0] = delta[0];
                    struct.setFieldValue(field, typedDelta);
                } else {
                    // attempt to convert all values to the correct type
                    Object[] convertedDelta = Converters.convert(delta, fieldClass);
                    if (convertedDelta.length == 0) {
                        // if none of the values are convertible, error
                        var name = field.type().getName();
                        error("Cannot set " + field + " of " + struct + " to the given value[s]. They are not " + name.toString(true) + ".");
                        return;
                    } else if (convertedDelta.length != delta.length) {
                        // if not all values are of the correct type, warn
                        warning("Not all values are of the correct type for " + field + " of " + struct + ". " +
                                (delta.length - convertedDelta.length) + " value[s] were ignored.");
                    }
                    struct.setFieldValue(field, convertedDelta);
                }
            }
        }
    }

    @Override
    public Class<?> getReturnType() {
        return returnType;
    }

    @Override
    public Class<?>[] possibleReturnTypes() {
        return returnTypes;
    }

    @Override
    public boolean isSingle() {
        // err on the side of singles to avoid parser errors.
        return isAnyFieldSingle;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "field " + fieldName + " of " + getExpr().toString(event, debug);
    }

    @Override
    public Node getNode() {
        return node;
    }
}
