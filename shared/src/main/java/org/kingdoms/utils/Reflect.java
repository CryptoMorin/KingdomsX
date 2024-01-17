package org.kingdoms.utils;

public final class Reflect {
    public static boolean classExists(String clazz) {
        try {
            // Prevent static initialization
            Class.forName(clazz, false, Reflect.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return false;
        } catch (Throwable e) {
            // Not sure if this'd happen, but some server software like silencing errors for some reasons.
            e.printStackTrace();
            return true;
        }
    }
}
