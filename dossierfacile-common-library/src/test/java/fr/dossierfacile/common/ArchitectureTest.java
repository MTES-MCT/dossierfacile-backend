package fr.dossierfacile.common;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;

public class ArchitectureTest {

    @Test
    void only_jpa_repositories_should_access_jpa_entities() {
        JavaClasses importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("fr.dossierfacile.common");

        ArchRule rule = noClasses()
                .that().areNotAssignableTo(fr.dossierfacile.common.infrastructure.repository.JpaRepository.class)
                .and().areNotAssignableTo(fr.dossierfacile.common.domain.model.DomainAggregate.class)
                .should().callMethodWhere(new DescribedPredicate<>("calls getEntityOnlyForRepository on DomainAggregate") {
                    @Override
                    public boolean test(JavaMethodCall target) {
                        return target.getTarget().getName().equals("getEntityOnlyForRepository")
                                && target.getTarget().getOwner().isAssignableTo(fr.dossierfacile.common.domain.model.DomainAggregate.class);
                    }
                });

        rule.check(importedClasses);
    }
}
