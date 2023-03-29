package fr.dossierfacile.process.file.util;

import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import javax.print.Doc;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static fr.dossierfacile.common.enums.DocumentCategory.PROFESSIONAL;
import static fr.dossierfacile.common.enums.DocumentCategory.TAX;
import static org.assertj.core.api.Assertions.anyOf;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentsTest {

    @Test
    void should_filter_documents_by_category() {
        Documents documents = new Documents(List.of(
                document(1L, TAX, false),
                document(2L, PROFESSIONAL, false),
                document(3L, TAX, true),
                document(4L, PROFESSIONAL, true)
        ));

        assertThat(documents.byCategory(TAX))
                .hasSize(1)
                .are(documentsWithId(1L));

        assertThat(documents.byCategory(PROFESSIONAL))
                .hasSize(1)
                .are(documentsWithId(2L));

        assertThat(documents.byCategories(List.of(TAX, PROFESSIONAL)))
                .hasSize(2)
                .are(documentsWithId(1L, 2L));
    }

    @Test
    void should_filter_out_elements_with_no_documents() {
        Documents documents = new Documents(List.of(
                document(1L, TAX, false),
                document(2L, TAX, null),
                document(3L, TAX, true)
        ));

        assertThat(documents.byCategory(TAX))
                .hasSize(2)
                .are(documentsWithId(1L, 2L));
    }

    @Test
    void should_select_documents_of_tenant_and_guarantors() {
        Tenant tenant = Tenant.builder()
                .documents(List.of(document(1L, TAX, false)))
                .guarantors(List.of(
                        guarantor(document(2L, TAX, false)),
                        guarantor(document(2L, TAX, false))))
                .build();

        Documents allDocuments = Documents.ofTenantAndGuarantors(tenant);

        assertThat(allDocuments.byCategory(TAX))
                .hasSize(3)
                .are(documentsWithId(1L, 2L, 3L));
    }

    private static Document document(long id, DocumentCategory category, Boolean noDocument) {
        return Document.builder()
                .id(id)
                .documentCategory(category)
                .noDocument(noDocument)
                .build();
    }

    private static Guarantor guarantor(Document document) {
        return Guarantor.builder()
                .documents(List.of(document))
                .build();
    }

    private static Condition<Document> documentsWithId(Long... ids) {
        var conditions = Arrays.stream(ids)
                .map(id -> new Condition<Document>(doc -> id.equals(doc.getId()), "doc " + id))
                .collect(Collectors.toList());
        return anyOf(conditions);
    }

}