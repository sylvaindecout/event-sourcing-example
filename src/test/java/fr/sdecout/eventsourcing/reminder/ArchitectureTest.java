package fr.sdecout.eventsourcing.reminder;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "fr.sdecout.eventsourcing")
class ArchitectureTest {

    @ArchTest
    public static final ArchRule domain_is_isolated = classes()
            .that()
            .resideInAPackage("fr.sdecout.reminder.domain..")
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage(
                    "", // Enum inherited methods and Arrays are in the default package :'(
                    "java..",
                    "org.mockito..",
                    "org.assertj..",
                    "fr.sdecout.eventsourcing.reminder.domain..",
                    "fr.sdecout.eventsourcing"
            )
            .as("The domain of the hexagon should be independent of infrastructure and technology")
            .because("business rules evolve at a different rhythm than technology." +
                    " See https://blog.xebia.fr/2016/03/16/perennisez-votre-metier-avec-larchitecture-hexagonale/" +
                    " for more information.");

    @ArchTest
    public static final ArchRule adapters_do_not_depend_on_one_another = slices()
            .matching("..infra.(*)..")
            .namingSlices("adapter '$1'")
            .should()
            .notDependOnEachOther()
            .as("Adapters around the domain of the hexagon should be independent of each other")
            .because("every business process should be under the control of the domain.");

}
