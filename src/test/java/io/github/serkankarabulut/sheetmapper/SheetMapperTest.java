package io.github.serkankarabulut.sheetmapper;

import io.github.serkankarabulut.sheetmapper.annotation.Column;
import io.github.serkankarabulut.sheetmapper.converter.ConverterRegistry;
import io.github.serkankarabulut.sheetmapper.converter.TypeConverter;
import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SheetMapperTest {

    @TempDir
    File tempDir;

    private SheetMapper sheetMapper;

    @BeforeEach
    void setUp() {
        sheetMapper = SheetMapper.forCsv();
    }

    public static class User {
        @Column(name = "ID") private int id;
        @Column(name = "Username") private String name;
        @Column(name = "Active") private boolean isActive;
        public User() {}
        public int getId() { return id; }
        public String getName() { return name; }
        public boolean isActive() { return isActive; }
    }

    public static class Event {
        @Column(name = "Event Name") private String eventName;
        @Column(name = "Date") private LocalDate eventDate;
        public Event() {}
        public String getEventName() { return eventName; }
        public LocalDate getEventDate() { return eventDate; }
    }

    public static class UserWithDefaultColumnName {
        @Column(name = "ID") private int id;
        @Column private String username;
        public UserWithDefaultColumnName() {}
        public String getUsername() { return username; }
    }

    public static class UserWithNoDefaultConstructor {
        public UserWithNoDefaultConstructor(String name) { /* No-arg constructor is missing */ }
    }

    public static class UserWithNoAnnotations {
        private String name; // No @Column annotation
    }

    @Nested
    @DisplayName("Successful Mapping Scenarios")
    class SuccessScenarios {
        @Test
        @DisplayName("Given a valid CSV file, it should correctly map to a List of User objects")
        void map_whenCsvIsValid_shouldReturnListOfUsers() throws IOException, SheetMappingException {
            String csvContent = "ID,Username,Active\n1,Jane Smith,true\n2,John Doe,false";
            File csvFile = createTempCsvFile("users.csv", csvContent);
            List<User> users = sheetMapper.map(csvFile, User.class);

            assertThat(users).isNotNull().hasSize(2);
            User firstUser = users.getFirst();
            assertThat(firstUser.getId()).isEqualTo(1);
            assertThat(firstUser.getName()).isEqualTo("Jane Smith");
            assertThat(firstUser.isActive()).isTrue();
        }

        @Test
        @DisplayName("With a custom ConverterRegistry, it should correctly map custom types like LocalDate")
        void map_withCustomConverterRegistry_shouldMapCustomTypes() throws IOException, SheetMappingException {
            ConverterRegistry customRegistry = new ConverterRegistry();
            TypeConverter<LocalDate> dateConverter = str -> LocalDate.parse(str, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            customRegistry.register(LocalDate.class, dateConverter);
            SheetMapper customMapper = SheetMapper.forCsv(customRegistry);

            String csvContent = "Event Name,Date\nProject Launch,25/10/2025";
            File csvFile = createTempCsvFile("events.csv", csvContent);

            List<Event> events = customMapper.map(csvFile, Event.class);
            assertThat(events).isNotNull().hasSize(1);
            assertThat(events.getFirst().getEventDate()).isEqualTo(LocalDate.of(2025, 10, 25));
        }

        @Test
        @DisplayName("When the CSV file has only a header row, it should return an empty list")
        void map_whenCsvHasOnlyHeader_shouldReturnEmptyList() throws IOException, SheetMappingException {
            String csvContent = "ID,Username,Active";
            File csvFile = createTempCsvFile("header_only.csv", csvContent);
            List<User> users = sheetMapper.map(csvFile, User.class);
            assertThat(users).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("When the @Column annotation name is empty, it should use the field name as the column name")
        void map_whenColumnNameIsEmpty_shouldUseFieldName() throws IOException, SheetMappingException {
            String csvContent = "ID,username\n1,testuser";
            File csvFile = createTempCsvFile("default_name.csv", csvContent);
            List<UserWithDefaultColumnName> result = sheetMapper.map(csvFile, UserWithDefaultColumnName.class);
            assertThat(result).hasSize(1);
            assertThat(result.getFirst().getUsername()).isEqualTo("testuser");
        }
    }

    @Nested
    @DisplayName("Standard Exception Scenarios")
    class StandardExceptionScenarios {
        @Test
        @DisplayName("When the given file does not exist, it should throw SheetMappingException")
        void map_whenFileDoesNotExist_shouldThrowException() {
            File nonExistentFile = new File(tempDir, "nonexistent.csv");
            assertThatThrownBy(() -> sheetMapper.map(nonExistentFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Sheet data file cannot be null and must exist");
        }

        @Test
        @DisplayName("When the given file is not a .csv file, it should throw SheetMappingException")
        void map_whenFileIsNotCsv_shouldThrowException() throws IOException {
            File txtFile = createTempCsvFile("test.txt", "content");
            assertThatThrownBy(() -> sheetMapper.map(txtFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageStartingWith("Sheet data file must be a CSV file:");
        }

        @Test
        @DisplayName("When the given class is null, it should throw SheetMappingException")
        void map_whenClassIsNull_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "ID\n1");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, null))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Class cannot be null");
        }

        @Test
        @DisplayName("When the given file is null, it should throw SheetMappingException")
        void map_whenFileIsNull_shouldThrowException() {
            assertThatThrownBy(() -> sheetMapper.map(null, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Sheet data file cannot be null and must exist");
        }

        @Test
        @DisplayName("When the CSV file is empty, it should throw SheetMappingException")
        void map_whenCsvIsEmpty_shouldThrowException() throws IOException {
            File emptyFile = createTempCsvFile("empty.csv", "");
            assertThatThrownBy(() -> sheetMapper.map(emptyFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("CSV file is empty or does not contain a header row.");
        }

        @Test
        @DisplayName("When a required column is missing in the CSV, it should throw SheetMappingException")
        void map_whenColumnIsMissing_shouldThrowException() throws IOException {
            String csvContent = "ID,Active\n1,true";
            File csvFile = createTempCsvFile("missing_column.csv", csvContent);
            assertThatThrownBy(() -> sheetMapper.map(csvFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Column not found in CSV headers: Username");
        }

        @Test
        @DisplayName("When trying to map a null value to a primitive field, it should throw SheetMappingException")
        void map_whenNullIsMappedToPrimitive_shouldThrowException() throws IOException {
            String csvContent = "ID,Username,Active\n,Jane Smith,true";
            File csvFile = createTempCsvFile("null_for_primitive.csv", csvContent);
            assertThatThrownBy(() -> sheetMapper.map(csvFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("Cannot map null value to primitive type: int");
        }

        @Test
        @DisplayName("When the target class has no no-arg constructor, it should throw SheetMappingException")
        void map_whenNoDefaultConstructor_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "name\nJohn");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, UserWithNoDefaultConstructor.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Class must have a no-arg constructor");
        }

        @Test
        @DisplayName("When the target class has no fields with the @Column annotation, it should throw SheetMappingException")
        void map_whenNoAnnotations_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "name\nJohn");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, UserWithNoAnnotations.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("No fields annotated with @Column found in class");
        }

        // --- NEW TEST ---
        @Test
        @DisplayName("When a data row is shorter than the header, it should throw SheetMappingException")
        void map_whenRowIsShorterThanHeader_shouldThrowException() throws IOException {
            // The second data row is missing the 'Active' column
            String csvContent = "ID,Username,Active\n1,Jane Smith,true\n2,John Doe";
            File csvFile = createTempCsvFile("jagged_row.csv", csvContent);

            // This test covers the 'if (index >= rowData.length)' check in mapRowToInstance
            assertThatThrownBy(() -> sheetMapper.map(csvFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Missing value for column");
        }
    }

    @Nested
    @DisplayName("Advanced Exception Scenarios")
    class AdvancedExceptionScenarios {

        public abstract static class AbstractUser { @Column(name = "ID") private int id; }
        public static class UserWithPrivateConstructor { @Column(name = "ID") private int id; private UserWithPrivateConstructor() {} }
        public static class UserWithFailingConstructor { @Column(name = "ID") private int id; public UserWithFailingConstructor() { throw new UnsupportedOperationException("Constructor failed"); } }

        @Test
        @DisplayName("When forCsv is called with a null registry, it should throw SheetMappingException")
        void constructor_whenRegistryIsNull_shouldThrowException() {
            assertThatThrownBy(() -> SheetMapper.forCsv(null))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("ConverterRegistry cannot be null");
        }

        @Test
        @DisplayName("When the given path is a directory, it should throw SheetMappingException")
        void map_whenFileIsDirectory_shouldThrowExceptionForFileNotFound() {
            File directoryAsCsv = new File(tempDir, "a-directory.csv");
            directoryAsCsv.mkdir();
            assertThatThrownBy(() -> sheetMapper.map(directoryAsCsv, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessage("File not found: " + directoryAsCsv.getAbsolutePath());
        }

        @Test
        @DisplayName("When mapping to an abstract class, it should throw SheetMappingException")
        void map_whenClassIsAbstract_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "ID\n1");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, AbstractUser.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Error creating instance of class");
        }

        @Test
        @DisplayName("When the target class has a private constructor, it should throw SheetMappingException")
        void map_whenConstructorIsPrivate_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "ID\n1");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, UserWithPrivateConstructor.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Error creating instance of class");
        }

        @Test
        @DisplayName("When the target class constructor throws an exception, it should throw SheetMappingException")
        void map_whenConstructorFails_shouldThrowException() throws IOException {
            File csvFile = createTempCsvFile("test.csv", "ID\n1");
            assertThatThrownBy(() -> sheetMapper.map(csvFile, UserWithFailingConstructor.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Error creating instance of class");
        }

        @Test
        @DisplayName("When the CSV file is malformed, it should throw SheetMappingException")
        void map_whenCsvIsMalformed_shouldThrowException() throws IOException {
            String malformedCsv = "ID,Username,Active\n1,UserOne,true\n2,\"UserTwo has an unclosed quote,false";
            File csvFile = createTempCsvFile("malformed.csv", malformedCsv);
            assertThatThrownBy(() -> sheetMapper.map(csvFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Error reading file");
        }

        @Test
        @DisplayName("When a converter returns a wrong type, it should throw SheetMappingException")
        void map_whenConverterReturnsWrongType_shouldThrowException() throws IOException {

            File csvFile = createTempCsvFile("test.csv", "ID,Username,Active\n1,test,true");

            ConverterRegistry faultyRegistry = new ConverterRegistry();

            faultyRegistry.register(
                    (Class) int.class,
                    (TypeConverter) (value -> "this is not an integer")
            );
            SheetMapper faultyMapper = SheetMapper.forCsv(faultyRegistry);

            assertThatThrownBy(() -> faultyMapper.map(csvFile, User.class))
                    .isInstanceOf(SheetMappingException.class)
                    .hasMessageContaining("Type mismatch for field id");
        }
    }

    private File createTempCsvFile(String fileName, String content) throws IOException {
        File file = new File(tempDir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}