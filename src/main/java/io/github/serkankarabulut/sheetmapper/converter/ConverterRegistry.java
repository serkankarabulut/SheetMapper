package io.github.serkankarabulut.sheetmapper.converter;

import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages a collection of {@link TypeConverter} instances for mapping string data to specific Java types.
 * <p>
 * This registry is responsible for holding all available type converters and dispatching
 * conversion tasks to the appropriate one based on the target class. It comes pre-configured
 * with default converters for common Java types but can be extended with custom converters.
 *
 * @author Serkan Karabulut
 */
public class ConverterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConverterRegistry.class);
    private final Map<Class<?>, TypeConverter<?>> typeConverters = new HashMap<>();

    /**
     * Constructs a new ConverterRegistry and initializes it with a set of default converters.
     * The default converters handle standard Java types such as String, Integer, Long, Double,
     * Boolean, and their corresponding primitive types.
     */
    public ConverterRegistry() {
        registerDefaultConverters();
    }

    /**
     * Registers the default {@link TypeConverter} instances for common Java types.
     */
    private void registerDefaultConverters() {
        // String
        register(String.class, value -> value);

        // Integer and int
        register(Integer.class, Integer::parseInt);
        register(int.class, Integer::parseInt);

        // Long and long
        register(Long.class, Long::parseLong);
        register(long.class, Long::parseLong);

        // Double and double
        register(Double.class, Double::parseDouble);
        register(double.class, Double::parseDouble);

        // Float and float
        register(Float.class, Float::parseFloat);
        register(float.class, Float::parseFloat);

        // Boolean and boolean
        register(Boolean.class, Boolean::parseBoolean);
        register(boolean.class, Boolean::parseBoolean);
    }

    /**
     * Registers a custom {@link TypeConverter} for a specific target type.
     * If a converter for the given type already exists, it will be replaced.
     *
     * @param type      The class of the target type (e.g., {@code LocalDate.class}).
     * @param converter The converter implementation that handles the conversion.
     * @param <T>       The target type.
     */
    public <T> void register(Class<T> type, TypeConverter<T> converter) {
        typeConverters.put(type, converter);
    }

    /**
     * Registers all converters from the given map.
     * This is a convenience method for adding multiple converters at once.
     *
     * @param converters A map where keys are the target type classes and values are the corresponding converters.
     */
    public void registerAll(Map<Class<?>, TypeConverter<?>> converters) {
        if (converters != null) {
            typeConverters.putAll(converters);
        }
    }

    /**
     * Converts a string value to an instance of the specified target type using a registered converter.
     *
     * @param csvValue   The raw string value from the sheet cell. If this value is {@code null} or blank,
     *                   the method will return {@code null}.
     * @param targetType The target {@link Class} to convert the string into.
     * @return The converted object, or {@code null} if the input string was null or blank.
     * @throws SheetMappingException if no suitable {@link TypeConverter} is found for the target type,
     *                               or if the selected converter fails to process the value.
     */
    public Object convert(String csvValue, Class<?> targetType) {
        if (csvValue == null || csvValue.trim().isEmpty()) {
            return null;
        }

        TypeConverter<?> converter = typeConverters.get(targetType);

        if (converter == null) {
            logger.error("Unsupported type conversion for: {}", targetType.getSimpleName());
            throw new SheetMappingException("Unsupported type conversion for: " + targetType.getSimpleName());
        }

        try {
            return converter.convert(csvValue);
        } catch (Exception e) {
            logger.error("Error converting value '{}' to type {}: {}", csvValue, targetType.getSimpleName(), e.getMessage(), e);
            throw new SheetMappingException("Error converting value '" + csvValue + "' to type " + targetType.getSimpleName(), e);
        }
    }
}