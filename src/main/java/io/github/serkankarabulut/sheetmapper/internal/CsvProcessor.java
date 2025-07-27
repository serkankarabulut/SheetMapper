package io.github.serkankarabulut.sheetmapper.internal;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.Reader;

/**
 * A wrapper class for {@link CSVReader} to handle the process of reading a CSV file.
 * <p>
 * This class is intended for internal use within the SheetMapper library only. It abstracts the
 * underlying CSV parsing logic and implements {@link AutoCloseable} to be used safely within
 * a try-with-resources statement, ensuring that the reader is always closed.
 *
 * @author Serkan Karabulut
 */
public class CsvProcessor implements AutoCloseable {
    private final CSVReader csvReader;

    /**
     * Constructs a new CsvProcessor.
     *
     * @param reader The reader providing the CSV data.
     */
    public CsvProcessor(Reader reader) {
        this.csvReader = new CSVReader(reader);
    }

    /**
     * Reads the next line from the CSV input stream and converts it into a string array.
     *
     * @return A string array containing the entries of the next line. Returns {@code null} if the end of the stream has been reached.
     * @throws CsvValidationException if a validation error occurs while parsing the CSV.
     * @throws IOException            if an I/O error occurs during reading.
     */
    public String[] readNext() throws CsvValidationException, IOException {
        return csvReader.readNext();
    }

    /**
     * Closes the underlying {@link CSVReader}. This method is automatically called
     * when the object is used in a try-with-resources statement.
     *
     * @throws IOException if an I/O error occurs when closing the reader.
     */
    @Override
    public void close() throws IOException {
        csvReader.close();
    }
}