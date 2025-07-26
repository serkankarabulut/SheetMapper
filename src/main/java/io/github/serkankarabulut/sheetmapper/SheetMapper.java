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

public final class SheetMapper {

    private static final Logger logger = LoggerFactory.getLogger(SheetMapper.class);
    private final ConverterRegistry converterRegistry;


    private SheetMapper(ConverterRegistry converterRegistry) throws SheetMappingException {
        if (converterRegistry == null) {
            logger.error("ConverterRegistry cannot be null");
            throw new SheetMappingException("ConverterRegistry cannot be null");
        }
        this.converterRegistry = converterRegistry;
    }

    public static SheetMapper forCsv() {
        return new SheetMapper(new ConverterRegistry());
    }

    public static SheetMapper forCsv(ConverterRegistry converterRegistry) throws SheetMappingException {
        return new SheetMapper(converterRegistry);
    }

    public <T> List<T> map(File sheetData, Class<T> clazz) throws SheetMappingException {
        if (clazz == null) {
            logger.error("Class cannot be null");
            throw new SheetMappingException("Class cannot be null");
        }
        if (sheetData == null || !sheetData.exists()) {
            logger.error("Sheet data file cannot be null and must exist: {}", sheetData);
            throw new SheetMappingException("Sheet data file cannot be null and must be exist");
        }
        if (!sheetData.getName().endsWith(".csv")) {
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

            String[] rowData = csvProcessor.readNext();
            while (rowData != null) {
                T instance = mapValue(rowData, headerIndexMap, columnFieldMap, constructor);
                mappedClasses.add(instance);
                rowData = csvProcessor.readNext();
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

    private <T> Set<Field> findFields(Class<T> clazz) {
        Set<Field> fields = Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(Column.class))
                .collect(Collectors.toSet());
        if (fields.isEmpty()) {
            logger.error("No fields found in class: {}", clazz.getName());
            throw new SheetMappingException("No fields found in class: " + clazz.getName());
        }
        return fields;
    }

    private String getColumnName(Field field) {
        return field.getAnnotation(Column.class).name() == null || field.getAnnotation(Column.class).name().isEmpty() ?
                field.getName() : field.getAnnotation(Column.class).name();
    }

    private Map<String, Integer> getHeaderIndexMap(CsvProcessor csvProcessor) throws CsvValidationException, IOException {
        String[] headersArray = csvProcessor.readNext();
        if (headersArray == null) {
            logger.error("CSV file is empty or does not contain headers");
            throw new SheetMappingException("CSV file is empty or does not contain headers: ");
        }
        List<String> headers = List.of(headersArray);
        Map<String, Integer> headerIndexMap = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            headerIndexMap.put(headers.get(i), i);
        }

        return headerIndexMap;
    }

    private <T> T mapValue(String[] rowData, Map<String, Integer> headerIndexMap, Map<String, Field> columnFieldMap, Constructor<T> constructor) throws SheetMappingException, InvocationTargetException, InstantiationException, IllegalAccessException {
        T instance = constructor.newInstance();
        for (Map.Entry<String, Field> entry : columnFieldMap.entrySet()) {

            String columnName = entry.getKey();
            Field field = entry.getValue();
            Integer index = headerIndexMap.get(columnName);

            if (index == null) {
                logger.error("Column not found in CSV headers: {}", columnName);
                throw new SheetMappingException("Column not found in CSV headers: " + columnName);
            }
            String value = rowData[index];
            Object convertedValue = converterRegistry.convert(value, field.getType());

            if (convertedValue == null && field.getType().isPrimitive()) {
                logger.error("Cannot map null value to primitive type: {}", field.getType().getName());
                throw new SheetMappingException("Cannot map null value to primitive type: " + field.getType().getName());
            }

            field.setAccessible(true);
            field.set(instance, convertedValue);
        }

        return instance;
    }
}
