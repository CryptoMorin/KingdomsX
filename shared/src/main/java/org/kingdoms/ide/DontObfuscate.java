package org.kingdoms.ide;

import java.lang.annotation.*;

/**
 * Prevents ProGuard from obfuscating these members.
 */
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS)
@Documented
public @interface DontObfuscate {}
