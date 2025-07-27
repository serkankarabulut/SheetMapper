# SheetMapper

[![Build Status](https://img.shields.io/travis/com/your-username/sheet-mapper.svg?style=flat-square)](https://travis-ci.com/your-username/sheet-mapper)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.serkankarabulut/sheet-mapper.svg?style=flat-square)](https://search.maven.org/artifact/io.github.serkankarabulut/sheet-mapper)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)

**SheetMapper** is a simple and intuitive Java library for mapping spreadsheet data directly to Plain Old Java Objects (POJOs). It uses a straightforward, annotation-based approach to eliminate boilerplate code and make data parsing a breeze.

Currently, it provides robust support for CSV files, with plans to support Excel formats (`.xls`, `.xlsx`) in the future.

## Features

*   **Annotation-Driven Mapping:** Use the `@Column` annotation to link CSV columns to your Java object fields.
*   **Automatic Header Detection:** The library automatically reads the header row of your CSV to map columns by name, not by index.
*   **Extensible Type Conversion:** Comes with built-in converters for common Java types (`String`, `Integer`, `Long`, `Double`, `Boolean`, and their primitive counterparts).
*   **Custom Converters:** Easily register your own `TypeConverter` for custom data types like `LocalDate`, `BigDecimal`, or any other class.
*   **Fluent API:** A clean and modern API for easy integration.
*   **Lightweight:** Minimal dependencies to keep your project lean.

## Installation

SheetMapper is available on Maven Central. You can add it to your project using your favorite build tool.

### Maven

```xml
<dependency>
    <groupId>io.github.serkankarabulut</groupId>
    <artifactId>sheet-mapper</artifactId>
    <version>1.0.0</version> <!-- Replace it with the latest version -->
</dependency>
```

### Gradle

```groovy
implementation 'io.github.serkankarabulut:sheet-mapper:1.0.0' // Replace with the latest version
```

## Usage

Mapping a CSV file to a list of objects is a simple, three-step process.

### 1. Create Your POJO

First, create a Java class that will hold your data. This class must have a **no-argument constructor**. Use the `@Column` annotation on the fields you want to map from the CSV.

*   If the `@Column(name = "...")` attribute is set, the library will look for a column with that exact name in the CSV header.
*   If the `name` attribute is omitted, the library will use the field's name as the column name.

**Example `User.java`:**

```java
import io.github.serkankarabulut.sheetmapper.annotation.Column;

public class User {

    @Column(name = "user_id")
    private long id;

    @Column // Will map to a column named "username"
    private String username;

    @Column(name = "email_address")
    private String email;

    @Column(name = "is_active")
    private boolean isActive;

    // A public no-arg constructor is required
    public User() {
    }

    // Getters and Setters (optional, but good practice)
    // ...
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
```

### 2. Prepare Your CSV File

Create a CSV file with a header row that matches the names specified in your `@Column` annotations or field names.

**Example `users.csv`:**

```csv
user_id,username,email_address,is_active
101,johndoe,john.doe@example.com,true
102,janedoe,jane.doe@example.com,false
103,sammy.s,sam.smith@example.com,true
```

### 3. Map the Data

Use the `SheetMapper` class to perform the mapping.

```java
import io.github.serkankarabulut.sheetmapper.SheetMapper;
import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;

import java.io.File;
import java.util.List;

public class MainApplication {
    public static void main(String[] args) {
        try {
            // Get the file from the resources folder
            File csvFile = new File(MainApplication.class.getResource("/users.csv").getFile());
            
            // Create a SheetMapper instance and map the data
            List<User> users = SheetMapper.forCsv().map(csvFile, User.class);
            
            // Print the results
            users.forEach(System.out::println);
            
        } catch (SheetMappingException e) {
            System.err.println("Error mapping the sheet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

**Output:**

```
User{id=101, username='johndoe', email='john.doe@example.com', isActive=true}
User{id=102, username='janedoe', email='jane.doe@example.com', isActive=false}
User{id=103, username='sammy.s', email='sam.smith@example.com', isActive=true}
```

## Advanced Usage: Custom Type Converters

SheetMapper allows you to handle custom data types or special string formats by registering your own `TypeConverter`.

For example, let's say your CSV contains dates in `yyyy-MM-dd` format, and you want to map them to `java.time.LocalDate`.

**1. Update Your POJO:**

Add a `LocalDate` field to your `User.java` class.

```java
// ... inside User.java
import java.time.LocalDate;

public class User {
    // ... other fields
    
    @Column(name = "registration_date")
    private LocalDate registrationDate;
    
    // ... constructor, getters, setters, etc.
}
```

**2. Update Your CSV:**

Add the new column to `users.csv`.

```csv
user_id,username,email_address,is_active,registration_date
101,johndoe,john.doe@example.com,true,2024-01-15
102,janedoe,jane.doe@example.com,false,2024-03-22
103,sammy.s,sam.smith@example.com,true,2025-07-21
```

**3. Register the Custom Converter:**

Create a `ConverterRegistry`, register your custom converter for `LocalDate`, and pass it to the `SheetMapper`.

```java
import io.github.serkankarabulut.sheetmapper.SheetMapper;
import io.github.serkankarabulut.sheetmapper.converter.ConverterRegistry;
import io.github.serkankarabulut.sheetmapper.exception.SheetMappingException;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AdvancedMain {
    public static void main(String[] args) {
        try {
            // 1. Create a custom converter registry
            ConverterRegistry customRegistry = new ConverterRegistry();
            
            // 2. Define and register your converter for LocalDate
            // The TypeConverter interface is a @FunctionalInterface, so you can use a lambda
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            customRegistry.register(LocalDate.class, value -> LocalDate.parse(value, formatter));
            
            // 3. Create a SheetMapper instance with the custom registry
            SheetMapper sheetMapper = SheetMapper.forCsv(customRegistry);
            
            // 4. Map as usual
            File csvFile = new File(AdvancedMain.class.getResource("/users.csv").getFile());
            List<User> users = sheetMapper.map(csvFile, User.class);
            
            users.forEach(System.out::println);
            
        } catch (SheetMappingException e) {
            System.err.println("Error mapping the sheet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
```

## Contributing

Contributions are welcome! If you find a bug, have a feature request, or want to contribute to the code, please feel free to:

1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/my-new-feature`).
3.  Make your changes.
4.  Commit your changes (`git commit -a -m 'Add some feature'`).
5.  Push to the branch (`git push origin feature/my-new-feature`).
6.  Open a new Pull Request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE.txt) file for details.