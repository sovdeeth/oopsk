package com.sovdee.oopsk.elements.expressions;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser.ParseResult;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;
import com.sovdee.oopsk.objects.CustomClass;
import com.sovdee.oopsk.objects.CustomClassInstance;
import com.sovdee.oopsk.objects.CustomClassManager;
import org.bukkit.event.Event;
import org.jetbrains.annotations.Nullable;

public class ExprNewObject extends SimpleExpression<CustomClassInstance> {

    static {
        Skript.registerExpression(ExprNewObject.class, CustomClassInstance.class, ExpressionType.SIMPLE, "[a] new instance of [struct [named]] <\\w+>");
    }

    private CustomClass clazz;

    @Override
    public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, ParseResult parseResult) {
        String name = parseResult.regexes.get(0).group(0);
        @Nullable CustomClass candidate = CustomClassManager.getClassByName(name);
        if (candidate == null) {
            Skript.error("Cannot create new instance of struct named " + name + " because the struct cannot be found.");
            return false;
        }
        clazz = candidate;
        return true;
    }

    @Override
    protected CustomClassInstance[] get(Event event) {
        return new CustomClassInstance[] {clazz.createInstance()};
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends CustomClassInstance> getReturnType() {
        return CustomClassInstance.class;
    }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "new instance of " + clazz;
    }
}
