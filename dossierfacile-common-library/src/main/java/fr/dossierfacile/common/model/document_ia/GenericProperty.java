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
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final String TYPE_LIST = "list";
    public static final String TYPE_OBJECT = "object";

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

    @JsonIgnore
    public String[] getStringListValue() {
        if (!TYPE_LIST.equals(type)) {
            throw new IllegalStateException("Property type is not list");
        }

        if (value == null) {
            return new String[0];
        }

        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Unsupported value type for list property '" + name + "': " + value.getClass());
        }

        return values.stream()
                .map(item -> item == null ? null : item.toString())
                .toArray(String[]::new);
    }

    @JsonIgnore
    public List<GenericProperty> getObjectValue() {
        if (!TYPE_OBJECT.equals(type)) {
            throw new IllegalStateException("Property type is not object");
        }

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Unsupported value type for object property '" + name + "': " + value.getClass());
        }

        return values.stream()
                .map(item -> toGenericProperty(item, name, TYPE_OBJECT))
                .toList();
    }

    @JsonIgnore
    public List<List<GenericProperty>> getObjectListValue() {
        if (!TYPE_LIST.equals(type)) {
            throw new IllegalStateException("Property type is not list");
        }

        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw new IllegalArgumentException("Unsupported value type for list property '" + name + "': " + value.getClass());
        }

        return values.stream()
                .map(item -> {
                    GenericProperty listItem = toGenericProperty(item, name, TYPE_LIST);
                    if (!TYPE_OBJECT.equals(listItem.getType())) {
                        throw new IllegalArgumentException("Unsupported list item property type for '" + name + "': " + listItem.getType());
                    }
                    List<GenericProperty> objectValue = listItem.getObjectValue();
                    return objectValue == null ? List.<GenericProperty>of() : objectValue;
                })
                .toList();
    }

    private GenericProperty toGenericProperty(Object item, String propertyName, String context) {
        if (item instanceof GenericProperty gp) {
            return gp;
        }

        if (item instanceof Map<?, ?> mapItem) {
            validateStrictGenericPropertyMap(mapItem, propertyName);
            return GenericProperty.builder()
                    .name((String) mapItem.get("name"))
                    .value(mapItem.get("value"))
                    .type((String) mapItem.get("type"))
                    .build();
        }

        throw new IllegalArgumentException("Unsupported " + context + " item type for property '" + propertyName + "': "
                + (item == null ? "null" : item.getClass()));
    }

    private void validateStrictGenericPropertyMap(Map<?, ?> mapItem, String propertyName) {
        Set<?> keys = mapItem.keySet();
        if (!keys.equals(Set.of("name", "value", "type"))) {
            throw new IllegalArgumentException("Unsupported object schema for property '" + propertyName
                    + "'. Expected keys: [name, value, type], found: " + keys);
        }
        if (!(mapItem.get("name") instanceof String) || !(mapItem.get("type") instanceof String)) {
            throw new IllegalArgumentException("Unsupported object schema for property '" + propertyName
                    + "'. 'name' and 'type' must be strings");
        }
    }
}