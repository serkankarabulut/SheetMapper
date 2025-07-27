package io.github.serkankarabulut.sheetmapper.converter;

/**
 * A functional interface for converting a string value from a sheet cell into a specific target type.
 * <p>
 * Implementations of this interface define the logic for transforming a single {@link String}
 * from a CSV or another sheet format into a Java object of type {@code T}. This is essential for
 * handling custom data types, such as {@link java.time.LocalDate}, {@link java.math.BigDecimal},
 * or any user-defined class.
 * <p>
 * Since this is a {@link FunctionalInterface}, it can be conveniently implemented using a lambda expression.
 *
 * <pre>{@code
 * // Example implementation for converting a string to a LocalDate
 * TypeConverter<LocalDate> localDateConverter = value -> LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
 * }</pre>
 *
 * @param <T> The target type to which the string value will be converted.
 * @author Serkan Karabulut
 */
@FunctionalInterface
public interface TypeConverter<T> {

    /**
     * Converts the given string value into an object of type {@code T}.
     *
     * @param value The string value from the sheet cell to be converted. This value is never null or blank,
     *              as such cases are handled by the calling registry before this method is invoked.
     * @return The converted object of type {@code T}.
     * @throws Exception if any error occurs during the conversion process (e.g., a parsing error like
     *                   {@link NumberFormatException}). The exception will be caught by the mapping
     *                   engine and wrapped in a {@link io.github.serkankarabulut.sheetmapper.exception.SheetMappingException}.
     */
    T convert(String value) throws Exception;
}