package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.StreamRevision;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.*;
import static net.jqwik.api.Combinators.combine;

public class ReminderEventArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(ReminderEvent.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final ArbitraryProvider.SubtypeProvider subtypeProvider) {
        return singleton(oneOf(
                reminderScheduled(),
                reminderCancelled(),
                reminderMarkedAsDone(),
                reminderAssigned(),
                reminderTransferred(),
                reminderReopened(),
                reminderRescheduled()
        ));
    }

    private static Arbitrary<ReminderEvent> reminderScheduled() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class),
                strings(),
                Arbitraries.of(ReminderType.class),
                defaultFor(Country.class),
                defaultFor(ZonedDateTime.class)
        ).as(ReminderEvent.ReminderScheduled::new);
    }

    private static Arbitrary<ReminderEvent> reminderCancelled() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class)
        ).as(ReminderEvent.ReminderCancelled::new);
    }

    private static Arbitrary<ReminderEvent> reminderMarkedAsDone() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class)
        ).as(ReminderEvent.ReminderMarkedAsDone::new);
    }

    private static Arbitrary<ReminderEvent> reminderReopened() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class)
        ).as(ReminderEvent.ReminderReopened::new);
    }

    private static Arbitrary<ReminderEvent> reminderRescheduled() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class),
                defaultFor(ZonedDateTime.class)
        ).as(ReminderEvent.ReminderRescheduled::new);
    }

    private static Arbitrary<ReminderEvent> reminderAssigned() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class),
                strings()
        ).as(ReminderEvent.ReminderAssigned::new);
    }

    private static Arbitrary<ReminderEvent> reminderTransferred() {
        return combine(
                strings(),
                defaultFor(StreamRevision.class),
                defaultFor(Instant.class),
                defaultFor(Country.class)
        ).as(ReminderEvent.ReminderTransferred::new);
    }

}
