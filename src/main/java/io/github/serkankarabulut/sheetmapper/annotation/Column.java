package io.github.serkankarabulut.sheetmapper.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field within a POJO to be mapped from a column in a sheet (e.g., a CSV file).
 * <p>
 * This annotation is essential for the mapping process. The {@link io.github.serkankarabulut.sheetmapper.SheetMapper}
 * scans for this annotation on the fields of the target class to determine which fields should be populated
 * with data from the sheet.
 * <p>
 * The mapping is based on the column name, which can be specified explicitly using the {@link #name()}
 * attribute or inferred from the field's name if the attribute is omitted.
 *
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * public class User {
 *
 *     // Maps the field 'id' to the column named "user_id" in the CSV header.
 *     @Column(name = "user_id")
 *     private Long id;
 *
 *     // Maps the field 'userName' to the column named "userName" in the CSV header.
 *     @Column
 *     private String userName;
 * }
 * }</pre>
 *
 * @author Serkan Karabulut
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column {

    /**
     * Specifies the exact name of the column in the sheet's header that should be mapped to this field.
     * <p>
     * If this attribute is not set, or is an empty string, the mapping mechanism will use the
     * name of the Java field itself as the column name.
     *
     * @return The name of the column in the sheet. Defaults to an empty string.
     */
    String name() default "";
}