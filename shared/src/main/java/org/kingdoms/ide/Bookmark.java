package org.kingdoms.ide;

import java.lang.annotation.*;

/**
 * This is merely used for easier navigation when searching the source code.
 * It has no effect on the program itself.
 * <p>
 * Should be used for things that can't be commented with a simple "TODO"
 * or used when categorizing them.
 * <p>
 * The intended way to use this annotation is to search for usages of {@link BookmarkType}
 * for the category of bookmark you're looking for using a proper Java IDE.
 */
@Target({
        ElementType.TYPE, ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER,
        ElementType.CONSTRUCTOR, ElementType.LOCAL_VARIABLE, ElementType.ANNOTATION_TYPE,
        ElementType.PACKAGE, ElementType.TYPE_PARAMETER, ElementType.TYPE_USE
})
@Retention(RetentionPolicy.SOURCE)
@Repeatable(Bookmarks.class)
@Documented
public @interface Bookmark {
    BookmarkType[] value();
    String comment() default "";
}
