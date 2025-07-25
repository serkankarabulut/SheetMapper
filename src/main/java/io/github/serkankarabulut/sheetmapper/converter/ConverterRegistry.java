package io.github.serkankarabulut.sheetmapper.converter;

import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ConverterRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConverterRegistry.class);
    private final Map<Class<?>, TypeConverter<?>> typeConverters = new HashMap<>();

    public ConverterRegistry() {
        registerDefaultConverters();
    }

    private void registerDefaultConverters() {

        // String
        register(String.class, value -> value);

        // Integer
        register(Integer.class, Integer::parseInt);
        register(int.class, Integer::parseInt);

        // Long
        register(Long.class, Long::parseLong);
        register(long.class, Long::parseLong);

        // Double
        register(Double.class, Double::parseDouble);
        register(double.class, Double::parseDouble);

        // Float
        register(Float.class, Float::parseFloat);
        register(float.class, Float::parseFloat);

        // Boolean
        register(Boolean.class, Boolean::parseBoolean);
        register(boolean.class, Boolean::parseBoolean);
    }

    public <T> void register(Class<T> type, TypeConverter<T> converter) {
        typeConverters.put(type, converter);
    }

    public <T> void registerAll(Map<Class<?>, TypeConverter<?>> converters) {
        if (converters != null) {
            typeConverters.putAll(converters);
        }
    }

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
            logger.error("Error converting value '{}' to type {}: {}", csvValue, targetType.getSimpleName(), e.getMessage());
            throw new SheetMappingException("Error converting value '" + csvValue + "' to type " + targetType.getSimpleName());
        }
    }
}
