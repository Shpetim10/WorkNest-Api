package com.worknest.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packages = "com.worknest", importOptions = ImportOption.DoNotIncludeTests.class)
public class PackageStructureTest {

    @ArchTest
    static final ArchRule layered_architecture_is_respected = layeredArchitecture()
            .consideringOnlyDependenciesInAnyPackage("com.worknest..")
            .layer("Web").definedBy("com.worknest.features..web..")
            .layer("Application").definedBy("com.worknest.features..application..", "com.worknest.features.notification.email.service..")
            .layer("Repository").definedBy("com.worknest.features..repository..")
            .layer("Domain").definedBy("com.worknest.domain..")
            .layer("Common").definedBy("com.worknest.common..")
            .layer("Security").definedBy("com.worknest.security..")
            .layer("Tenant").definedBy("com.worknest.tenant..")
            .layer("Audit").definedBy("com.worknest.audit..")

            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Security", "Audit")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Repository", "Web", "Security", "Tenant", "Audit", "Common");

    @ArchTest
    static final ArchRule controllers_should_be_in_web_package = classes()
            .that().haveSimpleNameEndingWith("Controller")
            .should().resideInAPackage("..web..");

    @ArchTest
    static final ArchRule services_should_be_in_application_package = classes()
            .that().haveSimpleNameEndingWith("ServiceImpl")
            .should().resideInAnyPackage("..application..", "..service..");

    @ArchTest
    static final ArchRule repositories_should_be_in_repository_package = classes()
            .that().haveSimpleNameEndingWith("Repository")
            .should().resideInAPackage("..repository..");

    @ArchTest
    static final ArchRule features_should_not_depend_on_other_features_internals = com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices()
            .matching("com.worknest.features.(*)..")
            .should().notDependOnEachOther()
            .because("Features should be isolated from each other's internal implementations");
}
