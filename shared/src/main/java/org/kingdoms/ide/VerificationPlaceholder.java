package org.kingdoms.ide;

import java.lang.annotation.*;

@Target({
        ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE,
        ElementType.PACKAGE, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.RUNTIME)
public @interface VerificationPlaceholder {
    String value();
}
