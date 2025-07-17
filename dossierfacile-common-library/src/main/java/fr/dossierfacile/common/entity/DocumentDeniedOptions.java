package fr.dossierfacile.common.entity;

import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "document_denied_options")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class DocumentDeniedOptions implements Serializable {

    private static final long serialVersionUID = -3348571231283734538L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(unique=true)
    private String code;

    private String messageValue;

    @Enumerated(EnumType.STRING)
    private DocumentCategory documentCategory;

    @Enumerated(EnumType.STRING)
    private DocumentSubCategory documentSubCategory;

    private String documentUserType;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        DocumentDeniedOptions that = (DocumentDeniedOptions) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public static int compareDocumentDeniedOptions(DocumentDeniedOptions o1, DocumentDeniedOptions o2) {

        boolean o1NullCat = o1.getDocumentCategory() == DocumentCategory.NULL;
        boolean o2NullCat = o2.getDocumentCategory() == DocumentCategory.NULL;
        if (o1NullCat && !o2NullCat) {
            return -1;
        }
        if (!o1NullCat && o2NullCat) {
            return 1;
        }
        if (o1NullCat) {
            int subCatCompare = String.valueOf(o1.getDocumentSubCategory()).compareTo(String.valueOf(o2.getDocumentSubCategory()));
            if (subCatCompare != 0) {
                return subCatCompare;
            }
            return o1.getCode().compareTo(o2.getCode());
        }

        // Les autres catégories
        int catCompare = String.valueOf(o1.getDocumentCategory()).compareTo(String.valueOf(o2.getDocumentCategory()));
        if (catCompare != 0) {
            return catCompare;
        }

        if (o1.getDocumentSubCategory() == DocumentSubCategory.UNDEFINED && o2.getDocumentSubCategory() != DocumentSubCategory.UNDEFINED) {
            return -1;
        }
        if (o1.getDocumentSubCategory() != DocumentSubCategory.UNDEFINED && o2.getDocumentSubCategory() == DocumentSubCategory.UNDEFINED) {
            return 1;
        }
        if (o1.getDocumentSubCategory() != DocumentSubCategory.UNDEFINED) {
            int subCatCompare = o1.getDocumentSubCategory().compareTo(o2.getDocumentSubCategory());
            if (subCatCompare != 0) {
                return subCatCompare;
            }
        }
        return o1.getCode().compareTo(o2.getCode());
    }
}
