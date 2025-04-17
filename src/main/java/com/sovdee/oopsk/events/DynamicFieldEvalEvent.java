package com.sovdee.oopsk.events;

import com.sovdee.oopsk.core.Struct;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Used for context when evaluating a dynamic field
 */
public class DynamicFieldEvalEvent extends Event {

    private final Struct struct;

    public DynamicFieldEvalEvent(Struct struct) {
        this.struct = struct;
    }

    /**
     * @return The struct that initiated this eval event.
     */
    public Struct getStruct() {
        return struct;
    }

    // bukkit stuff

    private static final HandlerList HANDLER_LIST = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return getHandlerList();
    }
}
