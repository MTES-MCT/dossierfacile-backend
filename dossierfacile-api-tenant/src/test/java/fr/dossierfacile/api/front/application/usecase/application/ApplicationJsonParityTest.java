package fr.dossierfacile.api.front.application.usecase.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fr.dossierfacile.api.front.application.projection.FullApplicationResponseProjection;
import fr.dossierfacile.api.front.application.projection.LightApplicationResponseProjection;
import fr.dossierfacile.api.front.application.projection.ApplicationProjectionLoader;
import fr.dossierfacile.api.front.application.usecase.application.GetFullApplicationUseCase.GetFullApplicationCommand;
import fr.dossierfacile.api.front.application.usecase.application.GetLightApplicationUseCase.GetLightApplicationCommand;
import fr.dossierfacile.api.front.domain.policy.TrigramAccessPolicy;
import fr.dossierfacile.api.front.fixtures.ApplicationSeed;
import fr.dossierfacile.api.front.fixtures.ApplicationSeed.Seed;
import fr.dossierfacile.api.front.repository.ApiTenantLogRepository;
import fr.dossierfacile.api.front.repository.JpaTestApplication;
import fr.dossierfacile.common.domain.service.ApartmentSharingStatusDomainService;
import fr.dossierfacile.common.entity.ApartmentSharing;
import fr.dossierfacile.common.entity.TenantLog;
import fr.dossierfacile.common.enums.TenantFileStatus;
import fr.dossierfacile.common.infrastructure.repository.JpaApartmentSharingRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaDocumentRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaGuarantorRepository;
import fr.dossierfacile.common.infrastructure.repository.JpaTenantRepository;
import fr.dossierfacile.common.mapper.ApplicationFullMapperImpl;
import fr.dossierfacile.common.mapper.ApplicationLightMapperImpl;
import fr.dossierfacile.common.model.apartment_sharing.ApplicationModel;
import fr.dossierfacile.common.service.interfaces.LinkLogService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Characterization and parity harness for the GET /api/application/full/{token} and light/{token} JSON.
 * The legacy MapStruct mappers act as the source of truth: new read path (use case + projection) must produce the exact same JSON on the same data.
 * In classic differential testing vocabulary, we compare the oracle (or source of truth) to a candidate
 */
@SpringBootTest
@ContextConfiguration(classes = JpaTestApplication.class)
@Import({
        ApplicationFullMapperImpl.class,
        ApplicationLightMapperImpl.class,
        JpaApartmentSharingRepository.class,
        JpaTenantRepository.class,
        JpaGuarantorRepository.class,
        JpaDocumentRepository.class,
        ApartmentSharingStatusDomainService.class,
        ApplicationProjectionLoader.class,
        FullApplicationResponseProjection.class,
        LightApplicationResponseProjection.class,
        TrigramAccessPolicy.class,
        fr.dossierfacile.api.front.domain.policy.LinkBruteForcePolicy.class,
        GetLightApplicationUseCase.class,
        GetFullApplicationUseCase.class
})
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:paritydb;MODE=PostgreSQL",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "application.base.url=http://api.test",
        "tenant.base.url=http://front.test"
})
class ApplicationJsonParityTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private ApplicationFullMapperImpl applicationFullMapper;
    @Autowired
    private ApplicationLightMapperImpl applicationLightMapper;
    @Autowired
    private ApiTenantLogRepository tenantLogRepository;
    @Autowired
    private GetLightApplicationUseCase getLightApplicationUseCase;
    @Autowired
    private GetFullApplicationUseCase getFullApplicationUseCase;
    @MockitoBean
    private LinkLogService linkLogService;
    @MockitoBean
    private fr.dossierfacile.api.front.service.interfaces.BruteForceProtectionService bruteForceProtectionService;

    private TransactionTemplate tx;
    private Seed seed;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Autowired
    void initTransactionTemplate(PlatformTransactionManager transactionManager) {
        this.tx = new TransactionTemplate(transactionManager);
    }

    @BeforeEach
    void setUp() {
        seed = tx.execute(status -> ApplicationSeed.seed(em));
    }

    // --- Oracle: legacy mappers + the lastUpdateDate rule of ApartmentSharingServiceImpl ---

    ApplicationModel oracleFull(Long sharingId, UUID token) {
        return tx.execute(status -> {
            ApartmentSharing sharing = em.find(ApartmentSharing.class, sharingId);
            ApplicationModel model = applicationFullMapper.toApplicationModelWithToken(sharing, token);
            model.setLastUpdateDate(legacyLastUpdateDate(sharing));
            return model;
        });
    }

    ApplicationModel oracleLight(Long sharingId) {
        return tx.execute(status -> {
            ApartmentSharing sharing = em.find(ApartmentSharing.class, sharingId);
            ApplicationModel model = applicationLightMapper.toApplicationModel(sharing);
            model.setLastUpdateDate(legacyLastUpdateDate(sharing));
            return model;
        });
    }

    private LocalDateTime legacyLastUpdateDate(ApartmentSharing sharing) {
        LocalDateTime lastUpdateDate = sharing.getLastUpdateDate();
        if (sharing.getStatus() == TenantFileStatus.VALIDATED) {
            return tenantLogRepository.findLastValidationLogByApartmentSharing(sharing.getId())
                    .map(TenantLog::getCreationDateTime)
                    .orElse(lastUpdateDate);
        }
        return lastUpdateDate;
    }

    // --- Normalization: sort every array of objects by "id" (legacy order was unspecified) ---

    JsonNode normalize(ApplicationModel model) {
        try {
            JsonNode root = objectMapper.readTree(objectMapper.writeValueAsString(model));
            sortArraysById(root);
            return root;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void sortArraysById(JsonNode node) {
        if (node instanceof ObjectNode objectNode) {
            objectNode.forEach(this::sortArraysById);
        } else if (node instanceof ArrayNode arrayNode) {
            List<JsonNode> children = new ArrayList<>();
            arrayNode.forEach(children::add);
            children.forEach(this::sortArraysById);
            if (children.stream().allMatch(c -> c.hasNonNull("id"))) {
                children.sort(Comparator.comparingLong(c -> c.get("id").asLong()));
                arrayNode.removeAll();
                children.forEach(arrayNode::add);
            }
        }
    }

    // --- Differential: new read path vs legacy oracle ---

    @Test
    void light_use_case_produces_the_exact_legacy_json() {
        ApplicationModel candidate = getLightApplicationUseCase.execute(
                new GetLightApplicationCommand(seed.lightToken(), "127.0.0.1"));

        JsonNode oracle = normalize(oracleLight(seed.sharing1Id()));
        assertThat(normalize(candidate)).isEqualTo(oracle);
    }

    @Test
    void full_use_case_produces_the_exact_legacy_json() {
        ApplicationModel candidate = getFullApplicationUseCase.execute(
                new GetFullApplicationCommand(seed.validToken(), ApplicationSeed.MAIN_TENANT_TRIGRAM, null, "127.0.0.1"));

        JsonNode oracle = normalize(oracleFull(seed.sharing1Id(), seed.validToken()));
        assertThat(normalize(candidate)).isEqualTo(oracle);
    }

    @Test
    void full_use_case_query_count_stays_bounded() {
        var statistics = em.getEntityManagerFactory()
                .unwrap(org.hibernate.SessionFactory.class)
                .getStatistics();

        statistics.clear();
        getFullApplicationUseCase.execute(
                new GetFullApplicationCommand(seed.validToken(), ApplicationSeed.MAIN_TENANT_TRIGRAM, null, "127.0.0.1"));
        long newPathQueries = statistics.getPrepareStatementCount();

        statistics.clear();
        oracleFull(seed.sharing1Id(), seed.validToken());
        long legacyPathQueries = statistics.getPrepareStatementCount();

        // N+1 guard: the ID-based loading must stay in the same order of magnitude as the legacy
        // lazy-loaded graph (5 planned queries + EAGER per-document fetches on the new entities)
        assertThat(newPathQueries)
                .as("new path=%d queries, legacy path=%d queries", newPathQueries, legacyPathQueries)
                .isLessThanOrEqualTo(30);
    }

    @Test
    void full_use_case_produces_the_exact_legacy_json_on_incomplete_sharing() {
        ApplicationModel candidate = getFullApplicationUseCase.execute(
                new GetFullApplicationCommand(seed.otherSharingToken(), "FAL", null, "127.0.0.1"));

        JsonNode oracle = normalize(oracleFull(seed.sharing2Id(), seed.otherSharingToken()));
        assertThat(normalize(candidate)).isEqualTo(oracle);
    }

    @Test
    void full_use_case_computes_null_tenant_status_like_legacy() {
        // Candidate MUST run before the oracle: the legacy getter heals the null status in memory,
        // and the oracle transaction could flush it — the candidate has to face the raw null column.
        ApplicationModel candidate = getFullApplicationUseCase.execute(
                new GetFullApplicationCommand(seed.nullStatusToken(), "NOS", null, "127.0.0.1"));

        JsonNode oracle = normalize(oracleFull(seed.nullStatusSharingId(), seed.nullStatusToken()));
        JsonNode normalizedCandidate = normalize(candidate);

        assertThat(normalizedCandidate).isEqualTo(oracle);
        // The status is computed, not null/absent. Legacy order: completeness is checked BEFORE
        // pending documents, so this file (no honor declaration, missing categories) stays
        // INCOMPLETE even though its only document is TO_PROCESS.
        assertThat(normalizedCandidate.get("tenants").get(0).get("status").asText()).isEqualTo("INCOMPLETE");
        assertThat(normalizedCandidate.get("status").asText()).isEqualTo("INCOMPLETE");
    }

    // --- Characterization of the current (legacy) behavior ---

    @Test
    void full_exposes_dossier_urls_and_watermarked_document_names() {
        JsonNode full = normalize(oracleFull(seed.sharing1Id(), seed.validToken()));

        assertThat(full.get("dossierUrl").asText())
                .isEqualTo("http://front.test/file/" + seed.validToken());
        assertThat(full.get("dossierPdfUrl").asText())
                .isEqualTo("http://api.test/api/application/fullPdf/" + seed.validToken());

        List<String> documentNames = full.findValues("name").stream().map(JsonNode::asText).toList();
        assertThat(documentNames).containsExactly(
                "http://api.test/api/application/links/" + seed.validToken() + "/documents/" + seed.watermarkedDocName());
    }

    @Test
    void light_hides_document_names_and_dossier_urls() {
        JsonNode light = normalize(oracleLight(seed.sharing1Id()));

        assertThat(light.has("dossierUrl")).isFalse();
        assertThat(light.has("dossierPdfUrl")).isFalse();
        assertThat(light.findValues("name")).isEmpty();
        assertThat(light.get("status").asText()).isEqualTo("VALIDATED");
    }

    @Test
    void light_and_full_differ_only_by_names_and_urls() {
        JsonNode full = normalize(oracleFull(seed.sharing1Id(), seed.validToken()));
        JsonNode light = normalize(oracleLight(seed.sharing1Id()));

        ((ObjectNode) full).remove("dossierUrl");
        ((ObjectNode) full).remove("dossierPdfUrl");
        full.findParents("name").forEach(parent -> ((ObjectNode) parent).remove("name"));

        assertThat(full).isEqualTo(light);
    }

    @Test
    void tenant_identity_follows_legacy_owner_type_rules() {
        JsonNode full = normalize(oracleFull(seed.sharing1Id(), seed.validToken()));
        JsonNode tenants = full.get("tenants");

        // SELF: user_account columns win
        JsonNode self = tenants.get(0);
        assertThat(self.get("firstName").asText()).isEqualTo("Jean");
        assertThat(self.get("lastName").asText()).isEqualTo("Dupont");
        assertThat(self.get("preferredName").asText()).isEqualTo("Martin");
        assertThat(self.get("franceConnect").asBoolean()).isTrue();
        assertThat(self.get("zipCode").asText()).isEqualTo("75011");
        assertThat(self.get("honorDeclaration").asBoolean()).isTrue();

        // THIRD_PARTY: tenant_* columns win, no fallback for preferredName
        JsonNode thirdParty = tenants.get(1);
        assertThat(thirdParty.get("firstName").asText()).isEqualTo("Marie");
        assertThat(thirdParty.get("lastName").asText()).isEqualTo("Durand");
        assertThat(thirdParty.get("preferredName").asText()).isEqualTo("Bernard");

        // Guarantors: NATURAL_PERSON with names, LEGAL_PERSON with legalPersonName
        List<JsonNode> guarantorTypes = full.findValues("typeGuarantor");
        assertThat(guarantorTypes).extracting(JsonNode::asText)
                .containsExactlyInAnyOrder("NATURAL_PERSON", "LEGAL_PERSON");
        assertThat(full.findValues("legalPersonName")).extracting(JsonNode::asText)
                .containsExactly("ACME SARL");
    }

    @Test
    void owner_type_null_falls_back_to_tenant_columns() {
        JsonNode full = normalize(oracleFull(seed.sharing2Id(), seed.otherSharingToken()));

        assertThat(full.get("status").asText()).isEqualTo("INCOMPLETE");
        JsonNode tenant = full.get("tenants").get(0);
        assertThat(tenant.get("firstName").asText()).isEqualTo("Paul");
        assertThat(tenant.get("lastName").asText()).isEqualTo("Fallback");
    }

    @Test
    void validated_sharing_takes_last_update_date_from_validation_log() {
        ApplicationModel full = oracleFull(seed.sharing1Id(), seed.validToken());

        LocalDateTime expected = tx.execute(status ->
                tenantLogRepository.findLastValidationLogByApartmentSharing(seed.sharing1Id())
                        .map(TenantLog::getCreationDateTime)
                        .orElseThrow());
        assertThat(full.getLastUpdateDate()).isEqualTo(expected);
    }
}
