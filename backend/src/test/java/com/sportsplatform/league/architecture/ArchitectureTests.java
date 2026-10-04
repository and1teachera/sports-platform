package com.sportsplatform.league.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests for the League context's boundaries, implementing the rules of
 * ADR-0006, Boundary enforcement.
 *
 * <p>Each rule is an {@link ArchRule} whose {@code because(...)} cites the rule by its name
 * in ADR-0006 §3. Rule 9 (Isolation) is covered by rule 1 (Domain independence) while League
 * is the only context, as ADR-0006 states; no separate test is written for it today.</p>
 */
@AnalyzeClasses(
        packages = "com.sportsplatform",
        importOptions = ImportOption.DoNotIncludeTests.class
)
public class ArchitectureTests {

    private static final String LEAGUE_DOMAIN = "com.sportsplatform.league.domain..";
    private static final String LEAGUE_APPLICATION = "com.sportsplatform.league.application..";
    private static final String LEAGUE_PERSISTENCE = "com.sportsplatform.league.infrastructure.persistence..";
    private static final String ACQUISITION = "com.sportsplatform.acquisition..";

    // Rule 1 — Domain independence.
    @ArchTest
    static final ArchRule domain_depends_only_on_itself_and_the_jdk = classes()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().onlyDependOnClassesThat().resideInAnyPackage(
                    LEAGUE_DOMAIN,
                    "java..",
                    "jdk.."
            )
            .because("ADR-0006 rule Domain independence");

    // Rule 2 — No framework in the domain.
    @ArchTest
    static final ArchRule domain_has_no_framework_dependency = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "org.flywaydb..",
                    "lombok.."
            )
            .because("ADR-0006 rule No framework in the domain");

    // Rule 3 — No hidden clock.
    @ArchTest
    static final ArchRule domain_does_not_depend_on_the_clock_type = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().dependOnClassesThat().areAssignableTo(Clock.class)
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_instant_now = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(Instant.class, "now")
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_local_date_now = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(LocalDate.class, "now")
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_local_date_time_now = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(LocalDateTime.class, "now")
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_zoned_date_time_now = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(ZonedDateTime.class, "now")
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_system_current_time_millis = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(System.class, "currentTimeMillis")
            .because("ADR-0006 rule No hidden clock");

    @ArchTest
    static final ArchRule domain_does_not_call_system_nano_time = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethod(System.class, "nanoTime")
            .because("ADR-0006 rule No hidden clock");

    // Rule 4 — No input or output.
    @ArchTest
    static final ArchRule domain_does_no_io = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "java.io..",
                    "java.nio.file..",
                    "java.net..",
                    "java.sql.."
            )
            .because("ADR-0006 rule No input or output");

    // Rule 5 — No provider reference.
    @ArchTest
    static final ArchRule domain_declares_no_provider_reference_type = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().haveSimpleNameContaining("ProviderReference")
            .because("ADR-0006 rule No provider reference");

    @ArchTest
    static final ArchRule domain_does_not_depend_on_provider_reference_types = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().dependOnClassesThat().haveSimpleNameContaining("ProviderReference")
            .because("ADR-0006 rule No provider reference");

    // Rule 6 — Application layer. Subject (..league.application..) is empty at CP6 by design.
    @ArchTest
    static final ArchRule application_layer_depends_on_neither_web_nor_infrastructure = noClasses()
            .that().resideInAPackage(LEAGUE_APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.sportsplatform.league.infrastructure..",
                    "com.sportsplatform.league.web..",
                    "org.springframework.web..",
                    "org.springframework.jdbc..",
                    "org.springframework.transaction.."
            )
            .because("ADR-0006 rule Application layer")
            .allowEmptyShould(true);

    // Rule 7 — Persistence adapters.
    @ArchTest
    static final ArchRule persistence_adapters_do_not_depend_on_the_web_edge = noClasses()
            .that().resideInAPackage(LEAGUE_PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.sportsplatform.league.web..",
                    "org.springframework.web.."
            )
            .because("ADR-0006 rule Persistence adapters");

    // Rule 8 — HTTP clients. Subject: every class outside the future acquisition package.
    @ArchTest
    static final ArchRule only_acquisition_may_reach_an_http_client = noClasses()
            .that().resideOutsideOfPackage(ACQUISITION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.apache.hc..",
                    "org.apache.http..",
                    "okhttp3..",
                    "java.net.http..",
                    "org.springframework.web.client..",
                    "org.springframework.web.reactive.function.client.."
            )
            .because("ADR-0006 rule HTTP clients");
}
