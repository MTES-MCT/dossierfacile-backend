package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.Bookmark;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.GuarantorSummary;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.PiecePage;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryLine;
import fr.dossierfacile.api.pdfgenerator.service.templates.dossier.DossierPdfModel.SummaryPerson;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Builds the {@link DossierPdfModel} of a dossier. It lists the persons and their pieces in display order, and
 * formats every text.
 * <p>
 * The pieces follow this order: each candidate, then its guarantors. For each person, the categories follow the
 * order of {@link DocumentCategory}, and the documents of a category follow their creation date.
 */
class DossierPdfModelBuilder {

    private final DossierPdfResources resources;
    private final Function<Document, List<PieceContent>> pieceContents;
    private final List<PiecePage> piecePages = new ArrayList<>();

    DossierPdfModelBuilder(DossierPdfResources resources, Function<Document, List<PieceContent>> pieceContents) {
        this.resources = resources;
        this.pieceContents = pieceContents;
    }

    DossierPdfModel build(ApartmentSharing apartmentSharing, DossierPdfStatus status, LocalDate date, LocalDate requestDate) {
        List<Tenant> tenants = apartmentSharing.getTenants().stream()
                .sorted(Comparator.comparing(Tenant::getTenantType))
                .toList();
        String names = DossierPdfFormat.enumeration(tenants.stream().map(DossierPdfFormat::fullName).filter(StringUtils::isNotBlank).toList());

        List<List<SummaryPerson>> candidates = new ArrayList<>();
        List<Bookmark> personBookmarks = new ArrayList<>();
        for (Tenant tenant : tenants) {
            List<SummaryPerson> persons = new ArrayList<>();
            persons.add(addPerson(true, "Candidat", DossierPdfFormat.fullName(tenant), tenant.getEmail(), tenant.getDocuments(),
                    category -> tenantPieceTitle(category, DossierPdfFormat.fullName(tenant)), personBookmarks));
            for (Guarantor guarantor : sortedGuarantors(tenant)) {
                persons.add(addPerson(false, ("Garant " + DossierPdfFormat.of(tenant.getFirstName())).strip(), guarantorName(guarantor), null,
                        guarantor.getDocuments(),
                        category -> guarantorPieceTitle(guarantor, category, DossierPdfFormat.fullName(tenant)), personBookmarks));
            }
            candidates.add(persons);
        }

        Tenant mainTenant = tenants.stream().filter(t -> t.getTenantType() == TenantType.CREATE).findFirst().orElse(tenants.get(0));
        String clarification = StringUtils.trimToNull(mainTenant.getClarification());
        String clarificationTitle = tenants.size() > 1 ? "Le mot des candidats" : "Le mot du candidat";

        List<Bookmark> bookmarks = new ArrayList<>();
        bookmarks.add(bookmark(status == DossierPdfStatus.VALIDATED ? "Sommaire" : "Page de garde", "cover", List.of()));
        if (clarification != null) {
            bookmarks.add(bookmark(clarificationTitle, "clarification", List.of()));
        }
        bookmarks.addAll(personBookmarks);
        bookmarks.add(bookmark("Comprendre les statuts", "statuses", List.of()));

        return DossierPdfModel.builder()
                .status(status)
                .title(("Dossier de location " + DossierPdfFormat.of(names)).strip())
                .names(names)
                .date(DossierPdfFormat.date(date))
                .requestDate(requestDate == null ? null : DossierPdfFormat.date(requestDate))
                .candidateCount(tenants.size() + (tenants.size() > 1 ? " candidats" : " candidat"))
                .totalIncome(DossierPdfFormat.euros(apartmentSharing.totalSalary()))
                .guarantorSummaries(guarantorSummaries(tenants))
                .candidates(candidates)
                .clarificationTitle(clarificationTitle)
                .clarification(clarification)
                .piecePages(piecePages)
                .bookmarks(bookmarks)
                .build();
    }

    /**
     * Adds the piece pages of one person and returns its summary entry.
     */
    private SummaryPerson addPerson(boolean candidate, String role, String name, String email, List<Document> documents,
            Function<DocumentCategory, PieceTitle> titles, List<Bookmark> bookmarks) {
        Map<DocumentCategory, List<Document>> byCategory = new LinkedHashMap<>();
        documents.stream()
                .sorted(Comparator.comparing(Document::getDocumentCategory)
                        .thenComparing(Document::getCreationDateTime, Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(document -> byCategory.computeIfAbsent(document.getDocumentCategory(), c -> new ArrayList<>()).add(document));

        List<SummaryLine> lines = new ArrayList<>();
        List<Bookmark> pieceBookmarks = new ArrayList<>();
        byCategory.forEach((category, categoryDocuments) -> {
            PieceTitle title = titles.apply(category);
            String firstAnchor = null;
            for (int index = 0; index < categoryDocuments.size(); index++) {
                String pieceTitle = categoryDocuments.size() > 1 ? title.numbered(index + 1) : title.single();
                String anchor = addPiecePages(categoryDocuments.get(index), pieceTitle);
                firstAnchor = firstAnchor == null ? anchor : firstAnchor;
            }
            lines.add(SummaryLine.builder()
                    .anchor(firstAnchor)
                    .label(title.summaryLabel())
                    .amount(category == DocumentCategory.FINANCIAL ? netAmount(categoryDocuments) : null)
                    .build());
            pieceBookmarks.add(bookmark(title.summaryLabel(), firstAnchor, List.of()));
        });
        if (!pieceBookmarks.isEmpty()) {
            bookmarks.add(bookmark(name + " (" + StringUtils.uncapitalize(role) + ")", pieceBookmarks.get(0).getAnchor(), pieceBookmarks));
        }
        String pieceCount = documents.size() + (documents.size() > 1 ? " pièces" : " pièce");
        return SummaryPerson.builder().candidate(candidate).role(role).name(name).email(email).pieceCount(pieceCount).lines(lines).build();
    }

    /**
     * Adds one page per page of the document and returns the anchor of the first one.
     */
    private String addPiecePages(Document document, String title) {
        List<PieceContent> contents = pieceContents.apply(document);
        if (contents.isEmpty()) {
            throw new IllegalStateException("Document " + document.getId() + " has no page");
        }
        // The pages of a PDF share the same file
        Map<byte[], String> uris = new IdentityHashMap<>();
        String firstAnchor = null;
        for (int index = 0; index < contents.size(); index++) {
            PieceContent content = contents.get(index);
            String anchor = "piece-" + (piecePages.size() + 1);
            String uri = uris.computeIfAbsent(content.file(), file -> resources.add(anchor + (content.isImage() ? ".jpg" : ".pdf"), file));
            piecePages.add(PiecePage.builder()
                    .anchor(anchor)
                    .title(title)
                    .uri(uri)
                    .pdfPage(content.isImage() ? null : content.pdfPage())
                    .alt(alt(title, content, index + 1, contents.size()))
                    .width(content.width())
                    .height(content.height())
                    .build());
            firstAnchor = firstAnchor == null ? anchor : firstAnchor;
        }
        return firstAnchor;
    }

    /**
     * Describes a piece page for screen readers, for example "La pièce d'identité de Camille Lefèvre, fichier
     * déposé". It then adds the text of the page, if the page has one.
     */
    private static String alt(String title, PieceContent content, int number, int count) {
        String alt = title + ", fichier déposé" + (count > 1 ? ", page " + number + " sur " + count : "");
        return content.text().isEmpty() ? alt : alt + ". Contenu : " + content.text();
    }

    private static List<Guarantor> sortedGuarantors(Tenant tenant) {
        return tenant.getGuarantors().stream()
                .sorted(Comparator.comparing(Guarantor::getTypeGuarantor)
                        .thenComparing(Guarantor::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    private static String guarantorName(Guarantor guarantor) {
        return switch (guarantor.getTypeGuarantor()) {
            case NATURAL_PERSON -> guarantor.getCompleteName();
            case LEGAL_PERSON -> StringUtils.defaultIfBlank(guarantor.getLegalPersonName(), "Personne morale");
            case ORGANISM -> "Organisme";
        };
    }

    private static String netAmount(List<Document> financialDocuments) {
        List<Integer> sums = financialDocuments.stream().map(Document::getMonthlySum).filter(Objects::nonNull).toList();
        return sums.isEmpty() ? null : DossierPdfFormat.euros(sums.stream().mapToInt(Integer::intValue).sum()) + " net";
    }

    private static List<GuarantorSummary> guarantorSummaries(List<Tenant> tenants) {
        List<Guarantor> guarantors = tenants.stream().flatMap(t -> t.getGuarantors().stream()).toList();
        List<Guarantor> naturalPersons = guarantors.stream().filter(g -> g.getTypeGuarantor() == TypeGuarantor.NATURAL_PERSON).toList();
        long legalPersons = guarantors.stream().filter(g -> g.getTypeGuarantor() == TypeGuarantor.LEGAL_PERSON).count();
        long organisms = guarantors.stream().filter(g -> g.getTypeGuarantor() == TypeGuarantor.ORGANISM).count();

        List<GuarantorSummary> summaries = new ArrayList<>();
        if (!naturalPersons.isEmpty()) {
            int income = naturalPersons.stream().mapToInt(Guarantor::getTotalSalary).sum();
            summaries.add(GuarantorSummary.builder()
                    .label(DossierPdfFormat.count(naturalPersons.size(), "personne", "personnes", true))
                    .detail(income > 0 ? "(" + DossierPdfFormat.euros(income) + " cumulés)" : null)
                    .build());
        }
        if (legalPersons > 0) {
            summaries.add(GuarantorSummary.builder().label(DossierPdfFormat.count((int) legalPersons, "personne morale", "personnes morales", true)).build());
        }
        if (organisms > 0) {
            summaries.add(GuarantorSummary.builder().label(DossierPdfFormat.count((int) organisms, "organisme", "organismes", false)).build());
        }
        if (summaries.isEmpty()) {
            summaries.add(GuarantorSummary.builder().label("Aucun garant").build());
        }
        return summaries;
    }

    private static Bookmark bookmark(String name, String anchor, List<Bookmark> children) {
        return Bookmark.builder().name(name).anchor(anchor).children(children).build();
    }

    private static PieceTitle tenantPieceTitle(DocumentCategory category, String name) {
        return switch (category) {
            case IDENTIFICATION -> new PieceTitle("Justificatif d'identité", "La pièce d'identité", DossierPdfFormat.of(name));
            case RESIDENCY -> new PieceTitle("Justificatif d'hébergement", "Le justificatif d'hébergement", DossierPdfFormat.of(name));
            case PROFESSIONAL ->
                new PieceTitle("Justificatif d'activité professionnelle", "Le justificatif d'activité professionnelle", DossierPdfFormat.of(name));
            case FINANCIAL -> new PieceTitle("Justificatif de ressources", "Le justificatif de ressources", DossierPdfFormat.of(name));
            case TAX -> new PieceTitle("Avis d'imposition", "L'avis d'imposition", DossierPdfFormat.of(name));
            default -> new PieceTitle("Autre pièce", "La pièce", DossierPdfFormat.of(name));
        };
    }

    private static PieceTitle guarantorPieceTitle(Guarantor guarantor, DocumentCategory category, String tenantName) {
        return switch (guarantor.getTypeGuarantor()) {
            case NATURAL_PERSON -> tenantPieceTitle(category, guarantor.getCompleteName());
            // IDENTIFICATION is the identity of the representative, and IDENTIFICATION_LEGAL_PERSON the one of the legal person.
            case LEGAL_PERSON -> category == DocumentCategory.IDENTIFICATION_LEGAL_PERSON
                    ? new PieceTitle("Identification de la personne morale", "L'identification de la personne morale garante", DossierPdfFormat.of(tenantName))
                    : new PieceTitle("Pièce d'identité du représentant", "La pièce d'identité du représentant de la personne morale garante",
                            DossierPdfFormat.of(tenantName));
            case ORGANISM -> new PieceTitle("Certificat de garantie", "Le certificat de l'organisme garant", DossierPdfFormat.of(tenantName));
        };
    }

    /**
     * Holds the labels of a piece. The summary shows a short one, such as "Justificatif de ressources". The page title
     * names the owner, as in "Le justificatif de ressources de Camille Lefèvre". It adds a number, as in "... n° 2 de
     * ...", when the person has several pieces of the category.
     */
    private record PieceTitle(String summaryLabel, String subject, String owner) {
        String single() {
            return (subject + " " + owner).strip();
        }

        String numbered(int number) {
            return (subject + " n°\u00A0" + number + " " + owner).strip();
        }
    }
}
