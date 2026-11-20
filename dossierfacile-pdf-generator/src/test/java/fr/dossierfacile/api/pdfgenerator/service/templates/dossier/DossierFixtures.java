package fr.dossierfacile.api.pdfgenerator.service.templates.dossier;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.ObjectStorageProvider;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentCategory;
import fr.dossierfacile.common.enums.DocumentStatus;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.enums.TenantType;
import fr.dossierfacile.common.enums.TypeGuarantor;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Builds validated dossiers with very long names, to check the dossier PDF on the worst cases.
 */
final class DossierFixtures {

    private static final AtomicLong IDS = new AtomicLong(1);
    private static final List<String> FIRST_NAMES = List.of("Léa", "Camille", "Jean", "Anne-Sophie", "Marie-Clémence", "Louise-Éléonore Marie Josèphe");
    private static final List<String> LAST_NAMES = List.of("Ngo", "Orio", "Petit", "Lefèvre", "Razafindrakotoarimananatsoavololona",
            "Van Der Straeten-Oudinot De Rosnay", "Castelbajac-Montesquiou D'Artagnan", "Vandenberghe-Quéméneur De La Villemarqué");

    private DossierFixtures() {}

    static ApartmentSharing alone() {
        Tenant tenant = tenant(TenantType.CREATE, "Anne-Laure", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 2100, 650, 400);
        naturalGuarantor(tenant, "Jean-Christophe", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 4200, 800);
        naturalGuarantor(tenant, "Marie-Thérèse Josiane", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 3100);
        tenant.setClarification("""
                Bonjour, je suis infirmière au CHU de Rennes depuis trois ans, en CDI.
                Je cherche un T2 proche de la ligne B du métro, disponible à partir de janvier.
                Mes parents se portent garants. Je reste disponible pour une visite en soirée 🙂""");
        return apartmentSharing(ApplicationType.ALONE, tenant);
    }

    static ApartmentSharing couple() {
        Tenant first = tenant(TenantType.CREATE, "Marie-Clémence", "Vandenberghe-Quéméneur De La Villemarqué", 2600);
        naturalGuarantor(first, "Jean-Christophe", "Vandenberghe-Quéméneur De La Villemarqué", 5200);
        naturalGuarantor(first, "Marie-Antoinette", "Vandenberghe-Quéméneur De La Villemarqué", 2900);
        Tenant second = tenant(TenantType.JOIN, "Jean-Baptiste", "Van Der Straeten-Oudinot De Rosnay", 3100, 450);
        organismGuarantor(second);
        first.setClarification("Nous sommes en couple depuis cinq ans et cherchons un T3 lumineux à Nantes.");
        return apartmentSharing(ApplicationType.COUPLE, first, second);
    }

    /** Builds the example of the Figma. The names are short, so Camille and her two guarantors fit in one column of the cover. */
    static ApartmentSharing shortNames() {
        Tenant camille = tenant(TenantType.CREATE, "Camille", "Lefèvre", 3000);
        naturalGuarantor(camille, "Frédéric", "Lefèvre", 5000);
        naturalGuarantor(camille, "Odile", "Lefèvre", 2800);
        Tenant nadia = tenant(TenantType.JOIN, "Nadia", "Colin", 4300);
        organismGuarantor(nadia);
        Tenant thomas = tenant(TenantType.JOIN, "Thomas", "Orio", 2300);
        naturalGuarantor(thomas, "Hélène", "Orio", 2900);
        camille.setClarification("Nous travaillons tous les trois à Lyon et cherchons un T4 près du métro.");
        return apartmentSharing(ApplicationType.GROUP, camille, nadia, thomas);
    }

    static ApartmentSharing largeFlatsharing() {
        Tenant t1 = tenant(TenantType.CREATE, "Marie-Clémence", "Vandenberghe-Quéméneur De La Villemarqué", 1800);
        naturalGuarantor(t1, "Jean-Christophe", "Vandenberghe-Quéméneur De La Villemarqué", 5200);
        naturalGuarantor(t1, "Marie-Antoinette", "Vandenberghe-Quéméneur De La Villemarqué", 2900, 300);
        Tenant t2 = tenant(TenantType.JOIN, "Jean-Baptiste", "Van Der Straeten-Oudinot De Rosnay", 1500, 300);
        organismGuarantor(t2);
        Tenant t3 = tenant(TenantType.JOIN, "Anne-Laure", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 1200, 500, 250);
        naturalGuarantor(t3, "Pierre-Emmanuel", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 3800);
        naturalGuarantor(t3, "Marie-Thérèse Josiane", "Bourdonnaye-Lanvaux De Kerangal-Le Bihan", 2700);
        Tenant t4 = tenant(TenantType.JOIN, "Pierre-Alexandre", "Montmorency-Laval De Saint-Exupéry", 2200);
        legalPersonGuarantor(t4, "Société Civile Immobilière Montmorency-Laval Et Associés");
        Tenant t5 = tenant(TenantType.JOIN, "Louise-Éléonore Marie Josèphe", "Castelbajac-Montesquiou D'Artagnan", 950);
        naturalGuarantor(t5, "Henri-Guillaume", "Castelbajac-Montesquiou D'Artagnan", 6100);
        naturalGuarantor(t5, "Anne-Sophie", "Castelbajac-Montesquiou D'Artagnan", 2400);
        Tenant t6 = tenant(TenantType.JOIN, "Charles-Henri", "Dupont-Aignan De La Rochefoucauld", 2050);
        Tenant t7 = tenant(TenantType.JOIN, "Marguerite-Hélène", "Lefebvre-Desnouettes De Kerguelen", 1350);
        organismGuarantor(t7);
        t1.setClarification("""
                Nous sommes sept amis, étudiants et jeunes actifs, et cherchons une grande maison en colocation à Lyon.
                Chacun a son garant ou une garantie Visale. Nous pouvons visiter le week-end.""");
        return apartmentSharing(ApplicationType.GROUP, t1, t2, t3, t4, t5, t6, t7);
    }

    /** Builds a candidate with four guarantors. The cover then shows "Quatre personnes (18 000 € cumulés)", which takes two lines. */
    static ApartmentSharing fourGuarantors() {
        Tenant camille = tenant(TenantType.CREATE, "Camille", "Lefèvre", 3000);
        naturalGuarantor(camille, "Frédéric", "Lefèvre", 5000);
        naturalGuarantor(camille, "Odile", "Lefèvre", 2800);
        naturalGuarantor(camille, "Hélène", "Lefèvre", 4200);
        naturalGuarantor(camille, "Paul", "Lefèvre", 6000);
        return apartmentSharing(ApplicationType.ALONE, camille);
    }

    /** Builds a dossier with random names, emails, guarantors and pieces. The same seed gives the same dossier. */
    static ApartmentSharing random(int seed) {
        Random random = new Random(seed);
        Tenant[] tenants = new Tenant[1 + random.nextInt(7)];
        for (int i = 0; i < tenants.length; i++) {
            Tenant tenant = tenant(i == 0 ? TenantType.CREATE : TenantType.JOIN, pick(FIRST_NAMES, random), pick(LAST_NAMES, random), randomIncomes(random));
            if (random.nextInt(6) == 0) {
                tenant.setEmail("a".repeat(20 + random.nextInt(100)) + "@exemple.fr");
            }
            int guarantors = random.nextInt(5);
            for (int g = 0; g < guarantors; g++) {
                naturalGuarantor(tenant, pick(FIRST_NAMES, random), pick(LAST_NAMES, random), randomIncomes(random));
            }
            if (random.nextInt(5) == 0) {
                organismGuarantor(tenant);
            }
            if (random.nextInt(8) == 0) {
                legalPersonGuarantor(tenant, "Société Civile Immobilière " + pick(LAST_NAMES, random));
            }
            tenants[i] = tenant;
        }
        return apartmentSharing(ApplicationType.GROUP, tenants);
    }

    private static String pick(List<String> values, Random random) {
        return values.get(random.nextInt(values.size()));
    }

    private static int[] randomIncomes(Random random) {
        return random.nextInt(3) == 0 ? new int[]{1000, 500} : new int[]{2000};
    }

    private static ApartmentSharing apartmentSharing(ApplicationType type, Tenant... tenants) {
        ApartmentSharing apartmentSharing = new ApartmentSharing();
        apartmentSharing.setId(IDS.getAndIncrement());
        apartmentSharing.setApplicationType(type);
        apartmentSharing.setTenants(new ArrayList<>(List.of(tenants)));
        return apartmentSharing;
    }

    private static Tenant tenant(TenantType type, String firstName, String lastName, int... incomes) {
        Tenant tenant = Tenant.builder().build();
        tenant.setId(IDS.getAndIncrement());
        tenant.setFirstName(firstName);
        tenant.setLastName(lastName);
        tenant.setEmail(emailOf(firstName) + "." + emailOf(lastName).replace("-", "") + "@exemple.fr");
        tenant.setTenantType(type);
        tenant.setStatus(TenantFileStatus.VALIDATED);
        tenant.setHonorDeclaration(true);
        tenant.setGuarantors(new ArrayList<>());
        tenant.setDocuments(personDocuments(incomes));
        return tenant;
    }

    private static String emailOf(String name) {
        return StringUtils.stripAccents(name).toLowerCase().replaceAll("[^a-z-]", "");
    }

    private static void naturalGuarantor(Tenant tenant, String firstName, String lastName, int... incomes) {
        Guarantor guarantor = guarantor(tenant, TypeGuarantor.NATURAL_PERSON);
        guarantor.setFirstName(firstName);
        guarantor.setLastName(lastName);
        guarantor.setDocuments(personDocuments(incomes));
    }

    private static void legalPersonGuarantor(Tenant tenant, String name) {
        Guarantor guarantor = guarantor(tenant, TypeGuarantor.LEGAL_PERSON);
        guarantor.setLegalPersonName(name);
        guarantor.setDocuments(new ArrayList<>(List.of(
                document(DocumentCategory.IDENTIFICATION, null),
                document(DocumentCategory.IDENTIFICATION_LEGAL_PERSON, null))));
    }

    private static void organismGuarantor(Tenant tenant) {
        Guarantor guarantor = guarantor(tenant, TypeGuarantor.ORGANISM);
        guarantor.setDocuments(new ArrayList<>(List.of(document(DocumentCategory.GUARANTEE_PROVIDER_CERTIFICATE, null))));
    }

    private static Guarantor guarantor(Tenant tenant, TypeGuarantor type) {
        Guarantor guarantor = Guarantor.builder().id(IDS.getAndIncrement()).typeGuarantor(type).tenant(tenant).build();
        tenant.getGuarantors().add(guarantor);
        return guarantor;
    }

    /** Builds the five categories of a person, with one income piece per given amount. */
    private static List<Document> personDocuments(int... incomes) {
        List<Document> documents = new ArrayList<>();
        documents.add(document(DocumentCategory.IDENTIFICATION, null));
        documents.add(document(DocumentCategory.RESIDENCY, null));
        documents.add(document(DocumentCategory.PROFESSIONAL, null));
        for (int income : incomes) {
            documents.add(document(DocumentCategory.FINANCIAL, income));
        }
        documents.add(document(DocumentCategory.TAX, null));
        return documents;
    }

    private static Document document(DocumentCategory category, Integer monthlySum) {
        Document document = new Document();
        document.setId(IDS.getAndIncrement());
        document.setDocumentCategory(category);
        document.setDocumentStatus(DocumentStatus.VALIDATED);
        document.setMonthlySum(monthlySum);
        document.setCreationDateTime(LocalDateTime.of(2026, 9, 1, 10, 0).plusMinutes(IDS.get()));
        document.setWatermarkFile(StorageFile.builder().path(category.name()).provider(ObjectStorageProvider.LOCAL).build());
        return document;
    }
}
