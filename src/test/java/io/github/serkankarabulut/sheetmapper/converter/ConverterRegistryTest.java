package io.github.serkankarabulut.sheetmapper.converter;

import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConverterRegistryTest {

    private ConverterRegistry converterRegistry;

    @BeforeEach
    void setUp() {
        converterRegistry = new ConverterRegistry();
    }

    @Nested
    @DisplayName("Default Converter Tests")
    class DefaultConverters {

        @Test
        @DisplayName("should convert to String")
        void shouldConvertTo_String() {
            assertThat(converterRegistry.convert("hello", String.class)).isEqualTo("hello");
        }

        @Test
        @DisplayName("should convert to Integer and int")
        void shouldConvertTo_Integer() {
            assertThat(converterRegistry.convert("123", Integer.class)).isEqualTo(123);
            assertThat(converterRegistry.convert("-45", int.class)).isEqualTo(-45);
        }

        @Test
        @DisplayName("should convert to Long and long")
        void shouldConvertTo_Long() {
            assertThat(converterRegistry.convert("1234567890", Long.class)).isEqualTo(1234567890L);
            assertThat(converterRegistry.convert("-98765", long.class)).isEqualTo(-98765L);
        }

        @Test
        @DisplayName("should convert to Double and double")
        void shouldConvertTo_Double() {
            assertThat(converterRegistry.convert("123.45", Double.class)).isEqualTo(123.45);
            assertThat(converterRegistry.convert("-0.5", double.class)).isEqualTo(-0.5);
        }

        @Test
        @DisplayName("should convert to Float and float")
        void shouldConvertTo_Float() {
            assertThat(converterRegistry.convert("12.3", Float.class)).isEqualTo(12.3f);
            assertThat(converterRegistry.convert("-0.5", float.class)).isEqualTo(-0.5f);
        }

        @Test
        @DisplayName("should convert to Boolean and boolean")
        void shouldConvertTo_Boolean() {
            assertThat(converterRegistry.convert("true", Boolean.class)).isEqualTo(true);
            assertThat(converterRegistry.convert("false", boolean.class)).isEqualTo(false);
            assertThat(converterRegistry.convert("TRUE", boolean.class)).isEqualTo(true);
            assertThat(converterRegistry.convert("anyOtherString", boolean.class)).isEqualTo(false);
        }
    }

    @Nested
    @DisplayName("Custom Converter Registration")
    class CustomConverters {

        @Test
        @DisplayName("should allow registering a new custom converter")
        void shouldRegisterAndUseCustomConverter() {
            TypeConverter<LocalDate> dateConverter = value -> LocalDate.parse(value, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            converterRegistry.register(LocalDate.class, dateConverter);

            Object result = converterRegistry.convert("25/12/2025", LocalDate.class);

            assertThat(result)
                    .isInstanceOf(LocalDate.class)
                    .isEqualTo(LocalDate.of(2025, 12, 25));
        }

        @Test
        @DisplayName("should allow overriding a default converter")
        void shouldOverrideDefaultConverter() {
            converterRegistry.register(String.class, String::toUpperCase);

            Object result = converterRegistry.convert("hello world", String.class);

            assertThat(result).isEqualTo("HELLO WORLD");
        }

        @Test
        @DisplayName("should allow registering a map of converters")
        void shouldRegisterAllFromMap() {
            TypeConverter<LocalDate> dateConverter = value -> LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            Map<Class<?>, TypeConverter<?>> customMap = Collections.singletonMap(LocalDate.class, dateConverter);

            converterRegistry.registerAll(customMap);
            Object result = converterRegistry.convert("2025-10-21", LocalDate.class);

            assertThat(result).isEqualTo(LocalDate.of(2025, 10, 21));
        }

        @Test
        @DisplayName("registerAll should not fail with a null map")
        void registerAll_shouldNotFailWithNullMap() {
            converterRegistry.registerAll(null);
        }
    }


    @Nested
    @DisplayName("Input and Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("should return null for empty string input")
        void shouldReturnNullForEmptyInput() {
            assertThat(converterRegistry.convert("", String.class)).isNull();
        }

        @Test
        @DisplayName("should return null for blank string input")
        void shouldReturnNullForBlankInput() {
            assertThat(converterRegistry.convert("   ", String.class)).isNull();
        }

        @Test
        @DisplayName("should return null for null string input")
        void shouldReturnNullForNullInput() {
            assertThat(converterRegistry.convert(null, String.class)).isNull();
        }

        @Test
        @DisplayName("should throw exception for unsupported conversion type")
        void shouldThrowExceptionForUnsupportedType() {
            Class<?> unsupportedType = java.awt.Point.class;

            assertThatThrownBy(() -> converterRegistry.convert("some-value", unsupportedType))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Unsupported type conversion for: Point");
        }

        @Test
        @DisplayName("should throw exception when conversion fails due to format error")
        void shouldThrowExceptionOnConversionFailure() {
            String invalidValue = "abc";

            assertThatThrownBy(() -> converterRegistry.convert(invalidValue, Integer.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Error converting value 'abc' to type Integer");
        }
    }
}