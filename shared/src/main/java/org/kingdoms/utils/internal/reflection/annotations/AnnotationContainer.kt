package org.kingdoms.utils.internal.reflection.annotations

interface AnnotationContainer {
    fun <T : Annotation> getAnnotation(annotationClass: java.lang.Class<T>): T?

    val javaObject: Any
    val declaringClass: java.lang.Class<*>

    companion object {
        @JvmStatic fun of(executable: java.lang.reflect.Executable) = Executable(executable)
        @JvmStatic fun <T> of(clazz: java.lang.Class<T>) = Class(clazz)
        @JvmStatic fun of(field: java.lang.reflect.Field) = Field(field)
    }

    fun findAll(vararg annotations: java.lang.Class<out Annotation?>): List<Annotation> {
        val found = mutableListOf<Annotation>()
        for (annotation in annotations) {
            val annotaiton: Annotation? = this.getAnnotation(annotation)
            if (annotaiton != null) found.add(annotaiton)
        }
        return found
    }

    fun any(vararg annotations: java.lang.Class<out Annotation?>): Annotation? {
        for (annotation in annotations) {
            val annotaiton: Annotation? = this.getAnnotation(annotation)
            if (annotaiton != null) return annotaiton
        }
        return null
    }

    /**
     * Constructors and normal methods.
     */
    class Executable(override val javaObject: java.lang.reflect.Executable) : AnnotationContainer {
        override val declaringClass: java.lang.Class<*> = javaObject.declaringClass
        override fun <T : Annotation> getAnnotation(annotationClass: java.lang.Class<T>): T? {
            return javaObject.getAnnotation(annotationClass)
        }
    }

    class Class<C>(override val javaObject: java.lang.Class<C>) : AnnotationContainer {
        override val declaringClass: java.lang.Class<*> = javaObject.declaringClass ?: Void::class.java
        override fun <T : Annotation> getAnnotation(annotationClass: java.lang.Class<T>): T? {
            return javaObject.getAnnotation(annotationClass)
        }
    }

    class Field(override val javaObject: java.lang.reflect.Field) : AnnotationContainer {
        override val declaringClass: java.lang.Class<*> = javaObject.declaringClass
        override fun <T : Annotation> getAnnotation(annotationClass: java.lang.Class<T>): T? {
            return javaObject.getAnnotation(annotationClass)
        }
    }
}
