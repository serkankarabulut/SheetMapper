package io.github.serkankarabulut.sheetmapper.internal;

import com.opencsv.exceptions.CsvValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvProcessorTest {

    @Test
    @DisplayName("readNext should return CSV rows sequentially")
    void readNext_shouldReturnRowsSequentially() throws CsvValidationException, IOException {
        // Given
        String csvData = "header1,header2\nvalue1,value2";
        Reader reader = new StringReader(csvData);
        try (CsvProcessor csvProcessor = new CsvProcessor(reader)) {
            // When & Then
            String[] header = csvProcessor.readNext();
            assertThat(header).containsExactly("header1", "header2");

            String[] dataRow = csvProcessor.readNext();
            assertThat(dataRow).containsExactly("value1", "value2");
        }
    }

    @Test
    @DisplayName("readNext should return null at the end of the stream")
    void readNext_shouldReturnNullAtEndOfStream() throws CsvValidationException, IOException {
        // Given
        String csvData = "single,line";
        Reader reader = new StringReader(csvData);
        try (CsvProcessor csvProcessor = new CsvProcessor(reader)) {
            // When
            csvProcessor.readNext();
            String[] endOfFile = csvProcessor.readNext();

            // Then
            assertThat(endOfFile).isNull();
        }
    }

    @Test
    @DisplayName("close should close the underlying reader")
    void close_shouldCloseUnderlyingReader() throws IOException {
        // Given
        Reader readerSpy = spy(new StringReader("a,b,c"));
        CsvProcessor csvProcessor = new CsvProcessor(readerSpy);

        // When
        csvProcessor.close();

        // Then
        verify(readerSpy, times(1)).close();
    }

    @Test
    @DisplayName("Processor should be AutoCloseable and close the reader in a try-with-resources block")
    void processor_shouldBeAutoCloseable() throws IOException, CsvValidationException {
        // Given
        Reader readerSpy = spy(new StringReader("a,b"));

        // When
        try (CsvProcessor csvProcessor = new CsvProcessor(readerSpy)) {
            csvProcessor.readNext();
        }

        // Then
        verify(readerSpy, times(1)).close();
    }
}