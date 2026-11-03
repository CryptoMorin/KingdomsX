package org.kingdoms.editor;

public class EditorSocketException extends Exception {
    private final int errorCode;
    private final String socketLink;

    public EditorSocketException(String message, Throwable cause, int errorCode, String socketLink) {
        super(message, cause);
        this.errorCode = errorCode;
        this.socketLink = socketLink;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getSocketLink() {
        return socketLink;
    }
}
