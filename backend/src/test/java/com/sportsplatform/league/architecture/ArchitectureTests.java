package com.sportsplatform.league.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.net.URLConnection;
import java.time.InstantSource;
import java.util.Calendar;
import java.util.Date;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

/**
 * Architecture tests for the League context's boundaries, implementing the rules of
 * ADR-0006, Boundary enforcement.
 *
 * <p>Each rule is an {@link ArchRule} whose {@code because(...)} cites the rule by its name
 * in ADR-0006 §3. Rule 9 (Isolation) is covered by rule 1 (Domain independence) while League
 * is the only context, as ADR-0006 states; no separate test is written for it today.</p>
 *
 * <p>Rule 1 plus review hold the no-provider-reference concept together: the field-name check
 * catches naming-only leaks (fields hinting at provider or external references even when typed
 * as a plain {@code String}), but it cannot by itself detect every semantic violation. Review
 * at code-change time is the complementary safeguard.</p>
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

    // Rule 3 — No hidden clock. Four checks replace the earlier seven.
    @ArchTest
    static final ArchRule domain_does_not_depend_on_an_instant_source = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().dependOnClassesThat().areAssignableTo(InstantSource.class)
            .because("ADR-0006 rule No hidden clock");

    private static final DescribedPredicate<JavaMethodCall> CALLS_NOW_ON_JAVA_TIME =
            new DescribedPredicate<>("calls now()/dateNow() on a java.time type") {
                @Override
                public boolean test(JavaMethodCall call) {
                    String ownerPackage = call.getTargetOwner().getPackageName();
                    String name = call.getName();
                    return ownerPackage.startsWith("java.time")
                            && (name.equals("now") || name.equals("dateNow"));
                }
            };

    @ArchTest
    static final ArchRule domain_does_not_call_a_now_method_on_a_java_time_type = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethodWhere(CALLS_NOW_ON_JAVA_TIME)
            .because("ADR-0006 rule No hidden clock");

    private static final DescribedPredicate<JavaMethodCall> IS_CALENDAR_GET_INSTANCE =
            new DescribedPredicate<>("calls Calendar.getInstance()") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return call.getTargetOwner().isAssignableTo(Calendar.class)
                            && call.getName().equals("getInstance");
                }
            };

    @ArchTest
    static final ArchRule domain_does_not_use_date_or_calendar_as_clocks = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callConstructor(Date.class)
            .orShould().callMethodWhere(IS_CALENDAR_GET_INSTANCE)
            .because("ADR-0006 rule No hidden clock");

    private static final DescribedPredicate<JavaMethodCall> IS_SYSTEM_TIME_API =
            new DescribedPredicate<>("calls System.currentTimeMillis()/nanoTime()") {
                @Override
                public boolean test(JavaMethodCall call) {
                    if (!call.getTargetOwner().getFullName().equals("java.lang.System")) return false;
                    String name = call.getName();
                    return name.equals("currentTimeMillis") || name.equals("nanoTime");
                }
            };

    @ArchTest
    static final ArchRule domain_does_not_call_system_time_apis = noClasses()
            .that().resideInAPackage(LEAGUE_DOMAIN)
            .should().callMethodWhere(IS_SYSTEM_TIME_API)
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
    static final ArchRule domain_fields_do_not_hint_at_provider_or_external_references = noFields()
            .that().areDeclaredInClassesThat().resideInAPackage(LEAGUE_DOMAIN)
            .should().haveNameMatching("(?i).*(provider|external).*")
            .because("ADR-0006 rule No provider reference");

    // Rule 6 — Application layer. Non-empty subject from the apply use case onward.
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
            .because("ADR-0006 rule Application layer");

    // Rule 7 — Persistence adapters.
    @ArchTest
    static final ArchRule persistence_adapters_do_not_depend_on_the_web_edge = noClasses()
            .that().resideInAPackage(LEAGUE_PERSISTENCE)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.sportsplatform.league.web..",
                    "org.springframework.web.."
            )
            .because("ADR-0006 rule Persistence adapters");

    // Rule 8 — HTTP clients. Two checks: package-based and the URLConnection bypass.
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

    @ArchTest
    static final ArchRule only_acquisition_may_reach_an_http_url_connection = noClasses()
            .that().resideOutsideOfPackage(ACQUISITION)
            .should().dependOnClassesThat().areAssignableTo(URLConnection.class)
            .because("ADR-0006 rule HTTP clients");
}
