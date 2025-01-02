package org.kingdoms.server.thread;


import java.lang.annotation.*;

/**
 * Marks that a method should run in the given thread context, otherwise it'll cause errors.
 * We could use compile-time annotation processing in order to wrap these methods in simple
 * checks and run them in their required thread if the threads don't match. That's a lot of work tho.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface RequiredThread {
    ServerThread value();
}
