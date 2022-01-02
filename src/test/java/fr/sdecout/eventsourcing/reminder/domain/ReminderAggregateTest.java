package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.StreamRevision;
import net.jqwik.api.*;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;

import static fr.sdecout.eventsourcing.StreamRevision.defaultStreamRevision;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStatus.*;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderType.CALL_CUSTOMER;
import static java.time.ZoneId.systemDefault;
import static net.jqwik.api.Arbitraries.defaultFor;
import static net.jqwik.api.Arbitraries.strings;
import static net.jqwik.api.Combinators.combine;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ReminderAggregateTest {

    private static final Instant NOW = Instant.parse("2017-12-31T23:59:59Z");
    private static final Instant TIMESTAMP_1 = Instant.parse("2019-07-17T10:15:30.00Z");
    private static final Instant TIMESTAMP_2 = Instant.parse("2019-07-18T10:15:30.00Z");
    private static final ZonedDateTime SCHEDULED_DATE_1 = ZonedDateTime.parse("2019-07-17T10:15:30.00Z");

    private final Clock clock = Clock.fixed(NOW, systemDefault());

    @Example
    void should_fail_to_initialize_state_from_null_event_stream() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ReminderAggregate(null, clock));
    }

    //FIXME: !!!
    @Example
    void should_initialize_state_from_input_event_stream() {
        final var eventStream = ReminderEventStream.of(
                new ReminderEvent.ReminderScheduled("REMINDER2", new StreamRevision(1), TIMESTAMP_1, "INTERVENTION1", CALL_CUSTOMER, new Country("IT"), SCHEDULED_DATE_1),
                new ReminderEvent.ReminderCancelled("REMINDER3", new StreamRevision(2), TIMESTAMP_2)
        );

        final ReminderAggregate aggregate = new ReminderAggregate(eventStream, clock);

        assertThat(aggregate.state()).isEqualTo(ReminderState.builder()
                .version(new StreamRevision(2))
                .id("REMINDER2")
                .interventionId("INTERVENTION1")
                .status(CANCELLED)
                .type(CALL_CUSTOMER)
                .country(new Country("IT"))
                .scheduledTime(SCHEDULED_DATE_1)
                .build());
    }

    @Property
    void should_schedule_new_reminder(@ForAll String reminderId, @ForAll String interventionId, @ForAll ReminderType reminderType,
                                      @ForAll Country country, @ForAll ZonedDateTime scheduledTime) {
        final ReminderAggregate aggregate = ReminderAggregate.scheduleNewReminder(reminderId, interventionId, reminderType, country, scheduledTime, clock);

        assertSoftly(softly -> {
            softly.assertThat(aggregate.clock()).isEqualTo(clock);
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderScheduled(reminderId, defaultStreamRevision().next(), NOW, interventionId, reminderType, country, scheduledTime)
            );
            softly.assertThat(aggregate.state()).isEqualTo(ReminderState.builder()
                    .version(defaultStreamRevision().next())
                    .id(reminderId)
                    .interventionId(interventionId)
                    .status(PENDING)
                    .type(reminderType)
                    .country(country)
                    .scheduledTime(scheduledTime)
                    .build());
        });
    }

    @Property
    void should_reschedule_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate,
                                                        @ForAll ZonedDateTime scheduledTime) {
        final var formerVersion = aggregate.state().version();

        aggregate.reschedule(scheduledTime);

        assertThat(aggregate.pendingEvents()).containsExactly(
                new ReminderEvent.ReminderRescheduled(aggregate.state().id(), formerVersion.next(), NOW, scheduledTime)
        );
    }

    @Property
    void should_fail_to_reschedule_reminder_with_status_other_than_pending(@ForAll("cancelledReminder") ReminderAggregate aggregate,
                                                                           @ForAll ZonedDateTime scheduledTime) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> aggregate.reschedule(scheduledTime))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): reschedule to %s", aggregate.state().id(), scheduledTime);
    }

    @Property
    void should_assign_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate,
                                                    @ForAll String assignee) {
        final var formerVersion = aggregate.state().version();

        aggregate.assignTo(assignee);

        assertSoftly(softly -> {
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderAssigned(aggregate.state().id(), formerVersion.next(), NOW, assignee)
            );
            softly.assertThat(aggregate.state().assignee()).isEqualTo(assignee);
        });
    }

    @Property
    void should_fail_to_assign_reminder_with_status_other_than_pending(@ForAll("cancelledReminder") ReminderAggregate aggregate,
                                                                       @ForAll String assignee) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> aggregate.assignTo(assignee))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): assign to %s", aggregate.state().id(), assignee);
    }

    @Property
    void should_unassign_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate) {
        final var formerVersion = aggregate.state().version();

        aggregate.unassign();

        assertThat(aggregate.pendingEvents()).containsExactly(
                new ReminderEvent.ReminderUnassigned(aggregate.state().id(), formerVersion.next(), NOW)
        );
    }

    @Property
    void should_fail_to_unassign_reminder_with_status_other_than_pending(@ForAll("cancelledReminder") ReminderAggregate aggregate) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> aggregate.unassign())
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): unassign", aggregate.state().id());
    }

    @Property
    void should_transfer_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate,
                                                      @ForAll Country country) {
        final var formerVersion = aggregate.state().version();

        aggregate.transferTo(country);

        assertSoftly(softly -> {
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderUnassigned(aggregate.state().id(), formerVersion.next(), NOW),
                    new ReminderEvent.ReminderTransferred(aggregate.state().id(), formerVersion.next().next(), NOW, country)
            );
            softly.assertThat(aggregate.state().country()).isEqualTo(country);
            softly.assertThat(aggregate.state().assignee()).isNull();
        });
    }

    @Property
    void should_fail_to_transfer_reminder_with_status_other_than_pending(@ForAll("cancelledReminder") ReminderAggregate aggregate,
                                                                         @ForAll Country country) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> aggregate.transferTo(country))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): transfer to %s", aggregate.state().id(), country);
    }

    @Property
    void should_mark_reminder_with_pending_status_as_done(@ForAll("pendingReminder") ReminderAggregate aggregate) {
        final var formerVersion = aggregate.state().version();

        aggregate.markAsDone();

        assertSoftly(softly -> {
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderMarkedAsDone(aggregate.state().id(), formerVersion.next(), NOW)
            );
            softly.assertThat(aggregate.state().status()).isEqualTo(DONE);
        });
    }

    @Property
    void should_fail_to_mark_reminder_with_cancelled_status_as_done(@ForAll("cancelledReminder") ReminderAggregate aggregate) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(aggregate::markAsDone)
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): mark as done", aggregate.state().id());
    }

    @Property
    void should_fail_to_mark_reminder_with_done_status_as_done(@ForAll("doneReminder") ReminderAggregate aggregate) {
        aggregate.markAsDone();

        assertThat(aggregate.pendingEvents()).isEmpty();
    }

    @Property
    void should_reopen_reminder_with_status_other_than_pending(@ForAll("cancelledReminder") ReminderAggregate aggregate) {
        final var formerVersion = aggregate.state().version();

        aggregate.reopen();

        assertSoftly(softly -> {
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderReopened(aggregate.state().id(), formerVersion.next(), NOW)
            );
            softly.assertThat(aggregate.state().status()).isEqualTo(PENDING);
        });
    }

    @Property
    void should_fail_to_reopen_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate) {
        aggregate.reopen();

        assertThat(aggregate.pendingEvents()).isEmpty();
    }

    @Property
    void should_cancel_reminder_with_pending_status(@ForAll("pendingReminder") ReminderAggregate aggregate) {
        final var formerVersion = aggregate.state().version();

        aggregate.cancel();

        assertSoftly(softly -> {
            softly.assertThat(aggregate.pendingEvents()).containsExactly(
                    new ReminderEvent.ReminderCancelled(aggregate.state().id(), formerVersion.next(), NOW)
            );
            softly.assertThat(aggregate.state().status()).isEqualTo(CANCELLED);
        });
    }

    @Property
    void should_fail_to_cancel_reminder_with_done_status(@ForAll("doneReminder") ReminderAggregate aggregate) {
        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(aggregate::cancel)
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): cancel", aggregate.state().id());
    }

    @Property
    void should_fail_to_cancel_reminder_with_cancelled_status(@ForAll("cancelledReminder") ReminderAggregate aggregate) {
        aggregate.cancel();

        assertThat(aggregate.pendingEvents()).isEmpty();
    }

    private static Arbitrary<ReminderEvent.ReminderScheduled> scheduledReminderEvent() {
        return combine(
                strings(),
                defaultFor(Instant.class),
                strings(),
                Arbitraries.of(ReminderType.class),
                defaultFor(Country.class),
                defaultFor(ZonedDateTime.class)
        ).as((reminderId, timestamp, interventionId, reminderType, country, scheduledTime) ->
                new ReminderEvent.ReminderScheduled(reminderId, defaultStreamRevision().next(), timestamp, interventionId, reminderType, country, scheduledTime));
    }

    @Provide
    private Arbitrary<ReminderAggregate> pendingReminder() {
        return scheduledReminderEvent()
                .map(ReminderEventStream::of)
                .map(eventStream -> new ReminderAggregate(eventStream, clock));
    }

    @Provide
    private Arbitrary<ReminderAggregate> cancelledReminder() {
        return scheduledReminderEvent().map(scheduledEvent ->
                ReminderEventStream.of(
                        scheduledEvent,
                        new ReminderEvent.ReminderCancelled(scheduledEvent.reminderId(), scheduledEvent.version().next(), scheduledEvent.timestamp().plusSeconds(1))
                )
        ).map(eventStream -> new ReminderAggregate(eventStream, clock));
    }

    @Provide
    private Arbitrary<ReminderAggregate> doneReminder() {
        return scheduledReminderEvent().map(scheduledEvent ->
                ReminderEventStream.of(
                        scheduledEvent,
                        new ReminderEvent.ReminderMarkedAsDone(scheduledEvent.reminderId(), scheduledEvent.version().next(), scheduledEvent.timestamp().plusSeconds(1))
                )
        ).map(eventStream -> new ReminderAggregate(eventStream, clock));
    }
}
