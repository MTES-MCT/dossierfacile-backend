package fr.dossierfacile.common.model.document_ia;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericProperty implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static final String TYPE_STRING = "string";
    public static final String TYPE_DATE = "date";

    private String name;

    private Object value;

    private String type;

    @JsonIgnore
    public String getStringValue() {
        if (!TYPE_STRING.equals(type)) {
            throw new IllegalStateException("Property type is not string");
        }
        return (String) value;
    }

    @JsonIgnore
    public LocalDate getDateValue() {
        if (!TYPE_DATE.equals(type)) {
            throw new IllegalStateException("Property type is not date");
        }

        if (value == null) {
            return null;
        }

        if (value instanceof String || value instanceof CharSequence) {
            String text = value.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(text, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format for property '" + name + "': '" + text + "'. Expected format: YYYY-MM-DD", e);
            }
        } else if (value instanceof Date date) {
            return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
        } else {
            throw new IllegalArgumentException("Unsupported value type for date property '" + name + "': " + value.getClass());
        }
    }
}