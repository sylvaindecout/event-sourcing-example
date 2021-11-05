package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Event;
import fr.sdecout.eventsourcing.StreamRevision;
import net.jqwik.api.*;
import net.jqwik.api.constraints.NotEmpty;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Stream;

import static com.google.common.base.Predicates.or;
import static fr.sdecout.eventsourcing.StreamRevision.defaultStreamRevision;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderType.CALL_CUSTOMER;
import static org.assertj.core.api.Assertions.assertThat;

class ReminderEventStreamTest {

    @Property
    void should_expose_empty_event_stream() {
        assertThat(ReminderEventStream.emptyEventStream().isEmpty()).isTrue();
    }

    @Property
    void should_initialize_from_list_of_events(@ForAll @NotEmpty List<ReminderEvent> events) {
        assertThat(new ReminderEventStream(events).isEmpty()).isFalse();
    }

    @Property
    void should_initialize_from_events(@ForAll ReminderEvent event) {
        assertThat(ReminderEventStream.of(event).isEmpty()).isFalse();
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_version_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        final var versionOfLastEvent = reverseEvents(eventStream).findFirst().map(Event::getVersion);
        assertThat(state.getVersion()).isEqualTo(versionOfLastEvent.orElse(defaultStreamRevision()));
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_reminder_ID_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        final var lastReminderScheduledEvent = getLastReminderSchedulingEvent(eventStream);
        assertThat(state.getId()).isEqualTo(lastReminderScheduledEvent.map(ReminderEvent::getReminderId).orElse(null));
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_intervention_ID_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        final var lastReminderScheduledEvent = getLastReminderSchedulingEvent(eventStream);
        assertThat(state.getInterventionId()).isEqualTo(lastReminderScheduledEvent.map(ReminderEvent.ReminderScheduled::getInterventionId).orElse(null));
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_reminder_type_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        final var lastReminderScheduledEvent = getLastReminderSchedulingEvent(eventStream);
        assertThat(state.getType()).isEqualTo(lastReminderScheduledEvent.map(ReminderEvent.ReminderScheduled::getReminderType).orElse(null));
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_country_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        assertThat(state.getCountry()).isEqualTo(getLastCountryUpdate(eventStream).orElse(null));
    }

    private static Optional<Country> getLastCountryUpdate(@ForAll ReminderEventStream eventStream) {
        return reverseEvents(eventStream)
                .filter(or(ReminderEvent.ReminderScheduled.class::isInstance, ReminderEvent.ReminderTransferred.class::isInstance))
                .findFirst()
                .map(event -> event instanceof ReminderEvent.ReminderScheduled
                        ? ((ReminderEvent.ReminderScheduled) event).getCountry()
                        : ((ReminderEvent.ReminderTransferred) event).getCountry());
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_assignee_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        assertThat(state.getAssignee()).isEqualTo(getLastAssigneeUpdate(eventStream).orElse(null));
    }

    private static Optional<String> getLastAssigneeUpdate(@ForAll ReminderEventStream eventStream) {
        return reverseEvents(eventStream)
                .filter(or(ReminderEvent.ReminderAssigned.class::isInstance, ReminderEvent.ReminderTransferred.class::isInstance))
                .findFirst()
                .filter(ReminderEvent.ReminderAssigned.class::isInstance)
                .map(ReminderEvent.ReminderAssigned.class::cast)
                .map(ReminderEvent.ReminderAssigned::getAssignee);
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_scheduled_time_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        assertThat(state.getScheduledTime()).isEqualTo(getLastScheduledTimeUpdate(eventStream).orElse(null));
    }

    private static Optional<ZonedDateTime> getLastScheduledTimeUpdate(@ForAll ReminderEventStream eventStream) {
        return reverseEvents(eventStream)
                .filter(or(ReminderEvent.ReminderScheduled.class::isInstance, ReminderEvent.ReminderRescheduled.class::isInstance))
                .findFirst()
                .map(event -> event instanceof ReminderEvent.ReminderScheduled
                        ? ((ReminderEvent.ReminderScheduled) event).getScheduledTime()
                        : ((ReminderEvent.ReminderRescheduled) event).getScheduledTime());
    }

    @Property
    @FromData("reminderEventsStreams")
    void should_set_status_on_replay(@ForAll ReminderEventStream eventStream) {
        final ReminderState state = eventStream.replay();

        assertThat(state.getStatus()).isEqualTo(getLastStatusUpdate(eventStream).orElse(null));
    }

    private Optional<ReminderState.ReminderStatus> getLastStatusUpdate(@ForAll ReminderEventStream eventStream) {
        return reverseEvents(eventStream)
                .filter(or(ReminderEvent.ReminderScheduled.class::isInstance, ReminderEvent.ReminderMarkedAsDone.class::isInstance,
                        ReminderEvent.ReminderReopened.class::isInstance, ReminderEvent.ReminderCancelled.class::isInstance))
                .findFirst()
                .map(event -> event instanceof ReminderEvent.ReminderCancelled
                        ? ReminderState.ReminderStatus.CANCELLED
                        : event instanceof ReminderEvent.ReminderMarkedAsDone
                        ? ReminderState.ReminderStatus.DONE
                        : ReminderState.ReminderStatus.PENDING);
    }

    private static Optional<ReminderEvent.ReminderScheduled> getLastReminderSchedulingEvent(final ReminderEventStream eventStream) {
        return reverseEvents(eventStream)
                .filter((ReminderEvent.ReminderScheduled.class)::isInstance)
                .map((ReminderEvent.ReminderScheduled.class)::cast)
                .findFirst();
    }

    private static Stream<? extends ReminderEvent> reverseEvents(final ReminderEventStream eventStream) {
        return reverse(eventStream.getEvents().stream());
    }

    private static <T> Stream<T> reverse(final Stream<T> input) {
        final Deque<T> output = input.collect(Collector.of(ArrayDeque::new, ArrayDeque::addFirst, (d1, d2) -> {
            d2.addAll(d1);
            return d2;
        }));
        return output.stream();
    }

    @Data
    private static Iterable<Tuple.Tuple1<ReminderEventStream>> reminderEventsStreams() {
        return Table.of(
                ReminderEventStream.of(
                        new ReminderEvent.ReminderCancelled("REMINDER1", new StreamRevision(1), Instant.now()),
                        new ReminderEvent.ReminderScheduled("REMINDER2", new StreamRevision(2), Instant.now(), "INTERVENTION1", CALL_CUSTOMER, new Country("IT"), ZonedDateTime.now()),
                        new ReminderEvent.ReminderCancelled("REMINDER3", new StreamRevision(3), Instant.now())
                ),
                ReminderEventStream.of(
                        new ReminderEvent.ReminderCancelled("REMINDER1", new StreamRevision(1), Instant.now())
                ),
                ReminderEventStream.emptyEventStream()
        );
    }

}
