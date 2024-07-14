package org.kingdoms.asm;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.events.EventException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class StaticMethodHandleEventExecutor implements EventExecutor {
    private final Class<? extends Event> eventClass;
    private final MethodHandle handle;

    public StaticMethodHandleEventExecutor(@NotNull Class<? extends Event> eventClass, @NotNull Method m) {
        Preconditions.checkArgument(Modifier.isStatic(m.getModifiers()), "Not a static method: %s", m);
        Preconditions.checkArgument(eventClass != null, "eventClass is null");
        this.eventClass = eventClass;
        try {
            m.setAccessible(true);
            this.handle = MethodHandles.lookup().unreflect(m);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Unable to set accessible", e);
        }
    }

    @Override
    public void execute(@NotNull Listener listener, @NotNull Event event) throws EventException {
        if (!eventClass.isInstance(event)) return;
        try {
            handle.invoke(event);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
