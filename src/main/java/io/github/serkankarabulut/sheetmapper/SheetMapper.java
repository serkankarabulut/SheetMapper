package io.github.serkankarabulut.sheetmapper;

import com.opencsv.exceptions.CsvValidationException;
import io.github.serkankarabulut.sheetmapper.annotation.Column;
import io.github.serkankarabulut.sheetmapper.converter.ConverterRegistry;
import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;
import io.github.serkankarabulut.sheetmapper.internal.CsvProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main class for mapping sheet data (e.g., CSV) to a list of Plain Old Java Objects (POJOs).
 * <p>
 * This class provides a simple, annotation-based mechanism to parse rows from a sheet
 * and map them to instances of a specified target class. It handles header detection,
 * type conversion, and object instantiation automatically.
 *
 * @author Serkan Karabulut
 */
public final class SheetMapper {

    private static final Logger logger = LoggerFactory.getLogger(SheetMapper.class);
    private final ConverterRegistry converterRegistry;

    /**
     * Private constructor to initialize the SheetMapper with a specific {@link ConverterRegistry}.
     *
     * @param converterRegistry The registry containing type converters for mapping. Cannot be null.
     * @throws SheetMappingException if the converterRegistry is null.
     */
    private SheetMapper(ConverterRegistry converterRegistry) {
        if (converterRegistry == null) {
            logger.error("ConverterRegistry cannot be null");
            throw new SheetMappingException("ConverterRegistry cannot be null");
        }
        this.converterRegistry = converterRegistry;
    }

    /**
     * Creates a new {@link SheetMapper} instance configured for CSV processing with default type converters.
     *
     * @return A new instance of {@link SheetMapper}.
     */
    public static SheetMapper forCsv() {
        return new SheetMapper(new ConverterRegistry());
    }

    /**
     * Creates a new {@link SheetMapper} instance for CSV processing with a custom {@link ConverterRegistry}.
     * This allows for the registration of custom type converters.
     *
     * @param converterRegistry The custom converter registry to use for type conversions.
     * @return A new instance of {@link SheetMapper}.
     * @throws SheetMappingException if the provided converterRegistry is null.
     */
    public static SheetMapper forCsv(ConverterRegistry converterRegistry) {
        return new SheetMapper(converterRegistry);
    }

    /**
     * Maps the data from a given sheet file to a list of objects of the specified class.
     *
     * @param sheetData The file containing the sheet data (e.g., a .csv file). Must exist and be readable.
     * @param clazz     The target class to which the data should be mapped. Must have a no-argument constructor
     *                  and fields annotated with {@link Column}.
     * @param <T>       The type of the target class.
     * @return A list of populated objects of type {@code T}.
     * @throws SheetMappingException if any error occurs during the mapping process, such as file not found,
     *                               I/O errors, or issues with class instantiation and field mapping.
     */
    public <T> List<T> map(File sheetData, Class<T> clazz) throws SheetMappingException {
        if (clazz == null) {
            logger.error("Class cannot be null");
            throw new SheetMappingException("Class cannot be null");
        }
        if (sheetData == null || !sheetData.exists()) {
            logger.error("Sheet data file cannot be null and must exist: {}", sheetData);
            throw new SheetMappingException("Sheet data file cannot be null and must exist");
        }
        if (!sheetData.getName().toLowerCase().endsWith(".csv")) {
            logger.error("Sheet data file must be a CSV file: {}", sheetData.getAbsolutePath());
            throw new SheetMappingException("Sheet data file must be a CSV file: " + sheetData.getAbsolutePath());
        }

        List<T> mappedClasses = new ArrayList<>();
        try (Reader reader = new FileReader(sheetData);
             CsvProcessor csvProcessor = new CsvProcessor(reader)
        ) {
            Constructor<T> constructor = clazz.getDeclaredConstructor();
            Set<Field> fields = findFields(clazz);
            Map<String, Field> columnFieldMap = fields.stream()
                    .collect(Collectors.toMap(this::getColumnName, field -> field));
            Map<String, Integer> headerIndexMap = getHeaderIndexMap(csvProcessor);

            String[] rowData;
            while ((rowData = csvProcessor.readNext()) != null) {
                T instance = mapRowToInstance(rowData, headerIndexMap, columnFieldMap, constructor);
                mappedClasses.add(instance);
            }

        } catch (FileNotFoundException e) {
            logger.error("File not found: {}", sheetData.getAbsolutePath(), e);
            throw new SheetMappingException("File not found: " + sheetData.getAbsolutePath(), e);
        } catch (NoSuchMethodException e) {
            logger.error("Class must have a no-arg constructor: {}", clazz.getName(), e);
            throw new SheetMappingException("Class must have a no-arg constructor: " + clazz.getName(), e);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            logger.error("Error creating instance of class: {}", clazz.getName(), e);
            throw new SheetMappingException("Error creating instance of class: " + clazz.getName(), e);
        } catch (IOException | CsvValidationException e) {
            logger.error("Error reading file: {}", sheetData.getAbsolutePath(), e);
            throw new SheetMappingException("Error reading file: " + sheetData.getAbsolutePath(), e);
        }
        return mappedClasses;
    }

    /**
     * Scans the target class for fields annotated with {@link Column}.
     *
     * @param clazz The class to scan.
     * @param <T>   The type of the class.
     * @return A set of fields that are annotated and should be mapped.
     * @throws SheetMappingException if no annotated fields are found.
     */
    private <T> Set<Field> findFields(Class<T> clazz) {
        Set<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toSet());
        if (fields.isEmpty()) {
            logger.error("No fields annotated with @Column found in class: {}", clazz.getName());
            throw new SheetMappingException("No fields annotated with @Column found in class: " + clazz.getName());
        }
        return fields;
    }

    /**
     * Determines the column name for a given field based on its {@link Column} annotation.
     * If the annotation's {@code name} attribute is set, it is used; otherwise, the field's name is used.
     *
     * @param field The field to inspect.
     * @return The name of the column to map to this field.
     */
    private String getColumnName(Field field) {
        Column columnAnnotation = field.getAnnotation(Column.class);
        String name = columnAnnotation.name();
        return (name == null || name.trim().isEmpty()) ? field.getName() : name;
    }

    /**
     * Reads the first line of the CSV file to build a map of header names to their column indices.
     *
     * @param csvProcessor The CSV processor to read from.
     * @return A map where keys are header names and values are their 0-based indices.
     * @throws CsvValidationException if there is an error during CSV validation.
     * @throws IOException            if an I/O error occurs.
     * @throws SheetMappingException  if the CSV file is empty or contains no header row.
     */
    private Map<String, Integer> getHeaderIndexMap(CsvProcessor csvProcessor) throws CsvValidationException, IOException {
        String[] headersArray = csvProcessor.readNext();
        if (headersArray == null) {
            logger.error("CSV file is empty or does not contain a header row.");
            throw new SheetMappingException("CSV file is empty or does not contain a header row.");
        }
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (int i = 0; i < headersArray.length; i++) {
            headerIndexMap.put(headersArray[i], i);
        }
        return headerIndexMap;
    }

    /**
     * Creates a new instance of the target class and populates its fields with data from a single CSV row.
     *
     * @param rowData        An array of strings representing the data in one row.
     * @param headerIndexMap A map from column names to their indices.
     * @param columnFieldMap A map from column names to their corresponding {@link Field} objects.
     * @param constructor    The no-argument constructor of the target class.
     * @param <T>            The type of the target object.
     * @return A new, populated instance of the target class.
     * @throws SheetMappingException         if a required column is not found in the CSV or a null value is mapped to a primitive type.
     * @throws InvocationTargetException     if the constructor throws an exception.
     * @throws InstantiationException        if the class cannot be instantiated.
     * @throws IllegalAccessException        if the constructor or a field is not accessible.
     */
    private <T> T mapRowToInstance(String[] rowData, Map<String, Integer> headerIndexMap, Map<String, Field> columnFieldMap, Constructor<T> constructor) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        T instance = constructor.newInstance();
        for (Map.Entry<String, Field> entry : columnFieldMap.entrySet()) {
            String columnName = entry.getKey();
            Field field = entry.getValue();
            Integer index = headerIndexMap.get(columnName);

            if (index == null) {
                logger.error("Column '{}' not found in CSV headers.", columnName);
                throw new SheetMappingException("Column not found in CSV headers: " + columnName);
            }

            if (index >= rowData.length) {
                logger.warn("Missing value for column '{}' (index {}) in a row. Skipping field set.", columnName, index);
                throw new SheetMappingException("Missing value for column '" + columnName + "' (index " + index + ") in a row. Skipping field set.");
            }

            String value = rowData[index];
            Object convertedValue = converterRegistry.convert(value, field.getType());

            if (convertedValue == null && field.getType().isPrimitive()) {
                logger.error("Cannot map null value to primitive type '{}' for field '{}'", field.getType().getName(), field.getName());
                throw new SheetMappingException("Cannot map null value to primitive type: " + field.getType().getName());
            }

            try {
                field.setAccessible(true);
                field.set(instance, convertedValue);
            } catch (IllegalArgumentException e) {
                logger.error("Type mismatch for field '{}'. Expected {} but got {}.", field.getName(), field.getType().getName(), convertedValue != null ? convertedValue.getClass().getName() : "null");
                throw new SheetMappingException("Type mismatch for field " + field.getName(), e);
            }
        }
        return instance;
    }
}