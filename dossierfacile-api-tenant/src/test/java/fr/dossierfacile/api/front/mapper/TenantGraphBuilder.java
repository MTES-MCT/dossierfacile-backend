package fr.dossierfacile.api.front.mapper;

import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.Document;
import fr.dossierfacile.common.entity.DocumentAnalysisReport;
import fr.dossierfacile.common.entity.DocumentAnalysisRule;
import fr.dossierfacile.common.entity.DocumentDeniedReasons;
import fr.dossierfacile.common.entity.File;
import fr.dossierfacile.common.entity.Guarantor;
import fr.dossierfacile.common.entity.DocumentRuleLevel;
import fr.dossierfacile.common.entity.StorageFile;
import fr.dossierfacile.common.entity.Tenant;
import fr.dossierfacile.common.enums.ApplicationType;
import fr.dossierfacile.common.enums.DocumentSubCategory;
import fr.dossierfacile.common.enums.TenantFileStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Fluent builder for constructing Tenant entity graphs with automatic
 * bidirectional relationship wiring at build time.
 */
public class TenantGraphBuilder {

    private Long id = 1L;
    private TenantFileStatus status = TenantFileStatus.VALIDATED;
    private Boolean franceConnect = false;
    private String userFirstName;
    private String userLastName;
    private String userPreferredName;
    private final List<Consumer<Tenant>> documentConfigurators = new ArrayList<>();
    private final List<Consumer<Tenant>> guarantorConfigurators = new ArrayList<>();
    private Consumer<Tenant> apartmentSharingConfigurator;

    public static TenantGraphBuilder aTenant() {
        return new TenantGraphBuilder();
    }

    public TenantGraphBuilder withId(Long id) {
        this.id = id;
        return this;
    }

    public TenantGraphBuilder withStatus(TenantFileStatus status) {
        this.status = status;
        return this;
    }

    public TenantGraphBuilder withFranceConnect(String firstName, String lastName, String preferredName) {
        this.franceConnect = true;
        this.userFirstName = firstName;
        this.userLastName = lastName;
        this.userPreferredName = preferredName;
        return this;
    }

    public TenantGraphBuilder withDocument(Consumer<DocumentBuilder> configurator) {
        documentConfigurators.add(tenant -> {
            DocumentBuilder db = new DocumentBuilder();
            configurator.accept(db);
            Document doc = db.build();
            doc.setTenant(tenant);
            tenant.getDocuments().add(doc);
        });
        return this;
    }

    public TenantGraphBuilder withGuarantor(Consumer<GuarantorBuilder> configurator) {
        guarantorConfigurators.add(tenant -> {
            GuarantorBuilder gb = new GuarantorBuilder();
            configurator.accept(gb);
            Guarantor guarantor = gb.build();
            guarantor.setTenant(tenant);
            tenant.getGuarantors().add(guarantor);
        });
        return this;
    }

    public TenantGraphBuilder inApartmentSharing(Consumer<ApartmentSharingBuilder> configurator) {
        this.apartmentSharingConfigurator = tenant -> {
            ApartmentSharingBuilder asb = new ApartmentSharingBuilder();
            configurator.accept(asb);
            asb.build(tenant);
        };
        return this;
    }

    public TenantGraphBuilder inDefaultApartmentSharing() {
        return inApartmentSharing(as -> as.withType(ApplicationType.ALONE));
    }

    public Tenant build() {
        Tenant tenant = Tenant.builder()
                .id(id)
                .status(status)
                .franceConnect(franceConnect)
                .documents(new ArrayList<>())
                .guarantors(new ArrayList<>())
                .build();

        if (franceConnect) {
            tenant.setUserFirstName(userFirstName);
            tenant.setUserLastName(userLastName);
            tenant.setUserPreferredName(userPreferredName);
        }

        for (Consumer<Tenant> dc : documentConfigurators) {
            dc.accept(tenant);
        }

        for (Consumer<Tenant> gc : guarantorConfigurators) {
            gc.accept(tenant);
        }

        if (apartmentSharingConfigurator != null) {
            apartmentSharingConfigurator.accept(tenant);
        } else {
            ApartmentSharing as = new ApartmentSharing();
            as.setApplicationType(ApplicationType.ALONE);
            as.setTenants(new ArrayList<>(List.of(tenant)));
            as.setApartmentSharingLinks(new ArrayList<>());
            tenant.setApartmentSharing(as);
        }

        return tenant;
    }

    // === DocumentBuilder ===

    public static class DocumentBuilder {
        private String name;
        private boolean hasWatermarkFile = false;
        private final DocumentSubCategory subCategory = DocumentSubCategory.MY_NAME;
        private final List<Consumer<Document>> fileConfigurators = new ArrayList<>();
        private Consumer<Document> deniedReasonsConfigurator;
        private Consumer<Document> analysisReportConfigurator;

        public DocumentBuilder withName(String name) {
            this.name = name;
            return this;
        }


        public DocumentBuilder withWatermarkFile() {
            this.hasWatermarkFile = true;
            return this;
        }

        public void withFile(Consumer<FileBuilder> configurator) {
            fileConfigurators.add(doc -> {
                FileBuilder fb = new FileBuilder();
                configurator.accept(fb);
                File file = fb.build();
                file.setDocument(doc);
                doc.getFiles().add(file);
            });
        }

        public void withDeniedReasons(Consumer<DeniedReasonsBuilder> configurator) {
            this.deniedReasonsConfigurator = doc -> {
                DeniedReasonsBuilder drb = new DeniedReasonsBuilder();
                configurator.accept(drb);
                doc.setDocumentDeniedReasons(drb.build());
            };
        }

        public void withAnalysisReport(Consumer<AnalysisReportBuilder> configurator) {
            this.analysisReportConfigurator = doc -> {
                AnalysisReportBuilder arb = new AnalysisReportBuilder();
                configurator.accept(arb);
                DocumentAnalysisReport report = arb.build();
                report.setDocument(doc);
                doc.setDocumentAnalysisReport(report);
            };
        }

        Document build() {
            StorageFile watermarkFile = null;
            if (hasWatermarkFile) {
                watermarkFile = new StorageFile();
                watermarkFile.setName("watermark.pdf");
            }

            Document doc = Document.builder()
                    .name(name)
                    .watermarkFile(watermarkFile)
                    .documentSubCategory(subCategory)
                    .files(new ArrayList<>())
                    .build();

            for (Consumer<Document> fc : fileConfigurators) {
                fc.accept(doc);
            }
            if (deniedReasonsConfigurator != null) {
                deniedReasonsConfigurator.accept(doc);
            }
            if (analysisReportConfigurator != null) {
                analysisReportConfigurator.accept(doc);
            }

            return doc;
        }
    }

    // === FileBuilder ===

    public static class FileBuilder {
        private Long id;
        private boolean hasPreview = false;
        private String sfName;
        private String sfContentType;
        private Long sfSize;
        private String sfMd5;

        public void withId(Long id) {
            this.id = id;
        }

        public FileBuilder withPreview() {
            this.hasPreview = true;
            return this;
        }

        public FileBuilder withStorageFile(String name, String contentType, Long size, String md5) {
            this.sfName = name;
            this.sfContentType = contentType;
            this.sfSize = size;
            this.sfMd5 = md5;
            return this;
        }

        File build() {
            StorageFile storageFile = null;
            if (sfName != null) {
                storageFile = new StorageFile();
                storageFile.setName(sfName);
                storageFile.setContentType(sfContentType);
                storageFile.setSize(sfSize);
                storageFile.setMd5(sfMd5);
            }

            StorageFile preview = hasPreview ? new StorageFile() : null;

            return File.builder()
                    .id(id)
                    .storageFile(storageFile)
                    .preview(preview)
                    .build();
        }
    }

    // === DeniedReasonsBuilder ===

    public static class DeniedReasonsBuilder {
        private boolean messageData = true;
        private List<String> checkedOptions = new ArrayList<>();
        private List<Integer> checkedOptionsId;

        public DeniedReasonsBuilder withMessageData(boolean messageData) {
            this.messageData = messageData;
            return this;
        }

        public void withOptions(List<String> options, List<Integer> ids) {
            this.checkedOptions = new ArrayList<>(options);
            this.checkedOptionsId = ids != null ? new ArrayList<>(ids) : null;
        }

        public void withOptions(List<String> options) {
            withOptions(options, null);
        }

        DocumentDeniedReasons build() {
            return DocumentDeniedReasons.builder()
                    .messageData(messageData)
                    .checkedOptions(new ArrayList<>(checkedOptions))
                    .checkedOptionsId(checkedOptionsId != null ? new ArrayList<>(checkedOptionsId) : null)
                    .build();
        }
    }

    // === AnalysisReportBuilder ===

    public static class AnalysisReportBuilder {
        private List<DocumentAnalysisRule> failedRules = new ArrayList<>();

        public void withFailedRules(DocumentAnalysisRule... rules) {
            this.failedRules = new ArrayList<>(List.of(rules));
        }

        DocumentAnalysisReport build() {
            return DocumentAnalysisReport.builder()
                    .failedRules(new ArrayList<>(failedRules))
                    .build();
        }
    }

    // === GuarantorBuilder ===

    public static class GuarantorBuilder {
        private Long id;
        private final List<Consumer<Guarantor>> documentConfigurators = new ArrayList<>();

        public GuarantorBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public void withDocument(Consumer<DocumentBuilder> configurator) {
            documentConfigurators.add(guarantor -> {
                DocumentBuilder db = new DocumentBuilder();
                configurator.accept(db);
                Document doc = db.build();
                doc.setGuarantor(guarantor);
                guarantor.getDocuments().add(doc);
            });
        }

        Guarantor build() {
            Guarantor guarantor = Guarantor.builder()
                    .id(id)
                    .documents(new ArrayList<>())
                    .build();

            for (Consumer<Guarantor> dc : documentConfigurators) {
                dc.accept(guarantor);
            }

            return guarantor;
        }
    }

    // === ApartmentSharingBuilder ===

    public static class ApartmentSharingBuilder {
        private ApplicationType type = ApplicationType.ALONE;
        private final List<Consumer<ApartmentSharing>> coTenantConfigurators = new ArrayList<>();

        public ApartmentSharingBuilder withType(ApplicationType type) {
            this.type = type;
            return this;
        }

        public void withCoTenant(Consumer<CoTenantBuilder> configurator) {
            coTenantConfigurators.add(as -> {
                CoTenantBuilder ctb = new CoTenantBuilder();
                configurator.accept(ctb);
                Tenant coTenant = ctb.build();
                coTenant.setApartmentSharing(as);
                as.getTenants().add(coTenant);
            });
        }

        void build(Tenant mainTenant) {
            ApartmentSharing as = new ApartmentSharing();
            as.setApplicationType(type);
            as.setTenants(new ArrayList<>());
            as.setApartmentSharingLinks(new ArrayList<>());

            as.getTenants().add(mainTenant);
            mainTenant.setApartmentSharing(as);

            for (Consumer<ApartmentSharing> cc : coTenantConfigurators) {
                cc.accept(as);
            }
        }
    }

    // === CoTenantBuilder ===

    public static class CoTenantBuilder {
        private Long id;
        private TenantFileStatus status = TenantFileStatus.VALIDATED;
        private final List<Consumer<Tenant>> documentConfigurators = new ArrayList<>();
        private final List<Consumer<Tenant>> guarantorConfigurators = new ArrayList<>();

        public CoTenantBuilder withId(Long id) {
            this.id = id;
            return this;
        }

        public CoTenantBuilder withStatus(TenantFileStatus status) {
            this.status = status;
            return this;
        }

        public CoTenantBuilder withDocument(Consumer<DocumentBuilder> configurator) {
            documentConfigurators.add(tenant -> {
                DocumentBuilder db = new DocumentBuilder();
                configurator.accept(db);
                Document doc = db.build();
                doc.setTenant(tenant);
                tenant.getDocuments().add(doc);
            });
            return this;
        }

        public void withGuarantor(Consumer<GuarantorBuilder> configurator) {
            guarantorConfigurators.add(tenant -> {
                GuarantorBuilder gb = new GuarantorBuilder();
                configurator.accept(gb);
                Guarantor guarantor = gb.build();
                guarantor.setTenant(tenant);
                tenant.getGuarantors().add(guarantor);
            });
        }

        Tenant build() {
            Tenant tenant = Tenant.builder()
                    .id(id)
                    .status(status)
                    .franceConnect(false)
                    .documents(new ArrayList<>())
                    .guarantors(new ArrayList<>())
                    .build();

            for (Consumer<Tenant> dc : documentConfigurators) {
                dc.accept(tenant);
            }
            for (Consumer<Tenant> gc : guarantorConfigurators) {
                gc.accept(tenant);
            }

            return tenant;
        }
    }

    // === Static helpers ===

    public static DocumentAnalysisRule rule(DocumentRuleLevel level, String message) {
        return DocumentAnalysisRule.builder()
                .level(level)
                .message(message)
                .build();
    }
}
