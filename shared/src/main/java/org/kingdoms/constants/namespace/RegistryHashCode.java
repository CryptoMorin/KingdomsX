package org.kingdoms.constants.namespace;

/**
 * Any object that can have it's hashcode optimized by being registered into a {@link NamespacedRegistry}.
 */
public interface RegistryHashCode {
    void setHashCode(int hashCode);
}
