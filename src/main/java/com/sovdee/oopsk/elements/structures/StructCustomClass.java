package com.sovdee.oopsk.elements.structures;

import ch.njol.skript.Skript;
import ch.njol.skript.config.Node;
import ch.njol.skript.config.SectionNode;
import ch.njol.skript.config.SimpleNode;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.function.Functions;
import com.sovdee.oopsk.objects.CustomClass;
import com.sovdee.oopsk.objects.CustomClassManager;
import com.sovdee.oopsk.objects.Field;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.skriptlang.skript.lang.entry.EntryContainer;
import org.skriptlang.skript.lang.structure.Structure;

import java.util.regex.MatchResult;

public class StructCustomClass extends Structure {

    static {
        Skript.registerStructure(StructCustomClass.class, "[global|:local] struct <(" + Functions.functionNamePattern + ")>");
    }

    private CustomClass object;

    @Override
    public boolean init(Literal<?>[] args, int matchedPattern, @NotNull SkriptParser.ParseResult parseResult, EntryContainer entryContainer) {
        MatchResult regex = parseResult.regexes.get(0);
        String name = regex.group(1);

        object = new CustomClass(name, getParser().getCurrentScript());

        if (CustomClassManager.getClassByName(name) != null) {
            Skript.error("Struct by the name of " + name + " already exists.");
            return false;
        }

        SectionNode node = entryContainer.getSource();
        loadFields(node);

        CustomClassManager.registerClass(object);

        return true;
    }

    private void loadFields(@NotNull SectionNode node) {
        for (Node child : node) {
            // plain
            if (child instanceof SimpleNode simpleNode) {
                String key = simpleNode.getKey();
                if (key == null || !key.matches("\\w+")) {
                    Skript.error("invalid field: " + key);
                    continue;
                }
                object.addFields(new Field(key));
            }
        }
    }

    public CustomClass getCustomClass() {
        return object;
    }

    @Override
    public boolean load() {
        return true;
    }

    @Override
    public void unload() {
        CustomClassManager.unregisterClass(object.getName());
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "obejct";
    }
}
