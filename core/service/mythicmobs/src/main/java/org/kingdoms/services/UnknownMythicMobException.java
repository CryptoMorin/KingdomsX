package org.kingdoms.services;

public class UnknownMythicMobException extends RuntimeException {
    private final String name;

    public UnknownMythicMobException(String name) {
        super(name);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public UnknownMythicMobException(String name, Throwable cause) {
        super(name, cause);
        this.name = name;
    }
}
