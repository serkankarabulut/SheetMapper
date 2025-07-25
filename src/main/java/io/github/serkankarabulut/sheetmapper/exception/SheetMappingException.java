package io.github.serkankarabulut.sheetmapper.exception;

public class SheetMappingException extends RuntimeException {
    public SheetMappingException(String message) {
        super(message);
    }

    public SheetMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
