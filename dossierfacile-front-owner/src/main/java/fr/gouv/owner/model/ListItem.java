package fr.gouv.owner.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListItem implements Comparable<ListItem> {
    private int id;
    private String value;
    private String label;

    public ListItem(int id, String value) {
        this.id = id;
        this.value = value;
        this.label = value;
    }

    public ListItem(String value) {
        this.value = value;
        this.label = value;
    }

    @Override
    public int compareTo(ListItem o) {
        if (getValue() == null || o.getValue() == null) {
            return 0;
        }
        return getValue().compareTo(o.getValue());
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
