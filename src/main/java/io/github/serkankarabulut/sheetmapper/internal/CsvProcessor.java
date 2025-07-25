package io.github.serkankarabulut.sheetmapper.internal;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;

public class CsvProcessor implements AutoCloseable {
    private final CSVReader csvReader;

    public CsvProcessor(Reader reader) {
        csvReader = new CSVReader(reader);
    }

    public String[] readNext() throws CsvValidationException, IOException {
        return csvReader.readNext();
    }

    @Override
    public void close() throws IOException {
        csvReader.close();
    }
}
