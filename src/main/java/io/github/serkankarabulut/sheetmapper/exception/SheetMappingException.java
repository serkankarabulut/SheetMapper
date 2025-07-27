package io.github.serkankarabulut.sheetmapper.exception;

/**
 * A custom runtime exception thrown for any errors that occur during the sheet mapping process.
 * <p>
 * This exception is the primary exception type used by the SheetMapper library to signal
 * issues such as file I/O errors, reflection problems, data conversion failures, or
 * invalid configuration. It wraps underlying checked exceptions (like {@link java.io.IOException})
 * to simplify error handling for the user.
 *
 * @author Serkan Karabulut
 */
public class SheetMappingException extends RuntimeException {

    /**
     * Constructs a new SheetMappingException with the specified detail message.
     *
     * @param message The detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public SheetMappingException(String message) {
        super(message);
    }

    /**
     * Constructs a new SheetMappingException with the specified detail message and cause.
     * <p>
     * Note that the detail message associated with {@code cause} is <i>not</i>
     * automatically incorporated in this exception's detail message.
     *
     * @param message The detail message (which is saved for later retrieval
     *                by the {@link #getMessage()} method).
     * @param cause   The cause (which is saved for later retrieval by the
     *                {@link #getCause()} method). (A {@code null} value is
     *                permitted, and indicates that the cause is nonexistent or unknown.)
     */
    public SheetMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}