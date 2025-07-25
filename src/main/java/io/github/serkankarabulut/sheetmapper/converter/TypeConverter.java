package io.github.serkankarabulut.sheetmapper.converter;

@FunctionalInterface
public interface TypeConverter<T> {
    T convert(String value) throws Exception;
}
