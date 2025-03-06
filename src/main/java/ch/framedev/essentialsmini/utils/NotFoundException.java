package ch.framedev.essentialsmini.utils;


/*
 * de.framedev.essentialsmini.utils
 * ===================================================
 * This File was Created by FrameDev
 * Please do not change anything without my consent!
 * ===================================================
 * This Class was created at 20.09.2020 19:35
 */

public class NotFoundException extends RuntimeException {

    public NotFoundException(Object object) {
        super(object + " cannot be found!");
    }

    public NotFoundException() {
        super("Object cannot be found!");
    }

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
