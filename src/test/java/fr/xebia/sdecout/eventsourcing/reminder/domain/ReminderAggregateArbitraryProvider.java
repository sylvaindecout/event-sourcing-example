package fr.xebia.sdecout.eventsourcing.reminder.domain;

import fr.xebia.sdecout.eventsourcing.StreamRevision;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.Set;

import static fr.xebia.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStatus.CANCELLED;
import static fr.xebia.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStatus.DONE;
import static fr.xebia.sdecout.eventsourcing.reminder.domain.ReminderType.CALL_CUSTOMER;
import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.of;
import static net.jqwik.api.Arbitraries.strings;
import static net.jqwik.api.Combinators.combine;

public final class ReminderAggregateArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(ReminderAggregate.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                combine(
                        strings(),
                        strings(),
                        of(ReminderState.ReminderStatus.class)
                ).as(ReminderAggregateArbitraryProvider::initReminder)
        );
    }

    private static ReminderAggregate initReminder(final String reminderId, final String interventionId, final ReminderState.ReminderStatus status) {
        final ReminderEvent firstEvent = new ReminderEvent.ReminderScheduled(reminderId, new StreamRevision(1), Instant.now(), interventionId, CALL_CUSTOMER, new Country("IT"), ZonedDateTime.parse("2019-07-17T10:15:30.00Z"));
        final Optional<? extends ReminderEvent> secondEvent = status == CANCELLED
                ? Optional.of(new ReminderEvent.ReminderCancelled(reminderId, new StreamRevision(2), Instant.now()))
                : status == DONE
                ? Optional.of(new ReminderEvent.ReminderMarkedAsDone(reminderId, new StreamRevision(2), Instant.now()))
                : Optional.empty();
        final ReminderEventStream eventStream = secondEvent
                .map(reminderEvent -> ReminderEventStream.of(firstEvent, reminderEvent))
                .orElseGet(() -> ReminderEventStream.of(firstEvent));
        return new ReminderAggregate(eventStream, Clock.systemUTC());
    }

}
