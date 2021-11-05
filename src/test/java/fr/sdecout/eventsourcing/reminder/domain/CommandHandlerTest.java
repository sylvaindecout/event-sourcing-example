package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.StreamRevision;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Optional;

import static fr.sdecout.eventsourcing.reminder.domain.CommandHandler.DEFAULT_COUNTRY;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderEventStream.emptyEventStream;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderType.CALL_CUSTOMER;
import static java.time.ZoneId.systemDefault;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class CommandHandlerTest {

    private static final String NEXT_ID = "1";
    private static final Instant NOW = Instant.parse("2017-12-31T23:59:59Z");
    private static final ZonedDateTime A_DATE_IN_THE_FUTURE = ZonedDateTime.parse("2022-03-04T11:30:00Z");

    private final ReminderEventStore eventStore = mock(ReminderEventStore.class);
    private final ReminderIdGenerator idGenerator = () -> NEXT_ID;
    private final Clock clock = Clock.fixed(NOW, systemDefault());
    private final CommandHandler commandHandler = new CommandHandler(eventStore, idGenerator, clock);

    @Property
    void should_schedule_new_reminder(@ForAll ReminderType reminderType, @ForAll String interventionId, @ForAll ZonedDateTime scheduledTime) {
        Mockito.reset(eventStore);
        given(eventStore.find(NEXT_ID)).willReturn(Optional.empty());

        final ReminderState state = commandHandler.schedule(reminderType, interventionId, scheduledTime);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderScheduled(NEXT_ID, new StreamRevision(1), NOW, interventionId, reminderType, DEFAULT_COUNTRY, scheduledTime)
        ));
        assertThat(state.getId()).isEqualTo(NEXT_ID);
    }

    @Property
    void should_fail_to_schedule_reminder_with_duplicate_ID(@ForAll ReminderType reminderType, @ForAll String interventionId, @ForAll ZonedDateTime scheduledTime) {
        given(eventStore.find(NEXT_ID)).willReturn(Optional.of(new ReminderAggregate(emptyEventStream(), clock)));

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.schedule(reminderType, interventionId, scheduledTime))
                .withMessage("Unexpected command: reminder ID is already present");
    }

    @Property
    void should_reschedule_pending_reminder(@ForAll String reminderId, @ForAll ZonedDateTime scheduledTime) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.reschedule(reminderId, scheduledTime);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderRescheduled(reminderId, new StreamRevision(2), NOW, scheduledTime)
        ));
    }

    @Property
    void should_fail_to_reschedule_unknown_reminder(@ForAll String reminderId, @ForAll ZonedDateTime scheduledTime) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.reschedule(reminderId, scheduledTime))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_reschedule_cancelled_reminder(@ForAll String reminderId, @ForAll ZonedDateTime scheduledTime) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.reschedule(reminderId, scheduledTime))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): reschedule to %s", reminderId, scheduledTime);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_reschedule_done_reminder(@ForAll String reminderId, @ForAll ZonedDateTime scheduledTime) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.reschedule(reminderId, scheduledTime))
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): reschedule to %s", reminderId, scheduledTime);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_reopen_cancelled_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        commandHandler.reopen(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderReopened(reminderId, new StreamRevision(3), NOW)
        ));
    }

    @Property
    void should_reopen_done_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        commandHandler.reopen(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderReopened(reminderId, new StreamRevision(3), NOW)
        ));
    }

    @Property
    void should_fail_to_reopen_unknown_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.reopen(reminderId))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_not_reopen_pending_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.reopen(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents());
    }

    @Property
    void should_cancel_pending_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.cancel(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderCancelled(reminderId, new StreamRevision(2), NOW)
        ));
    }

    @Property
    void should_fail_to_cancel_unknown_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.cancel(reminderId))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_not_cancel_cancelled_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        commandHandler.cancel(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents());
    }

    @Property
    void should_fail_to_cancel_done_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.cancel(reminderId))
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): cancel", reminderId);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_mark_pending_reminder_as_done(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.markAsDone(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderMarkedAsDone(reminderId, new StreamRevision(2), NOW)
        ));
    }

    @Property
    void should_fail_to_mark_unknown_reminder_as_done(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.markAsDone(reminderId))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_not_mark_done_reminder_as_done(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        commandHandler.markAsDone(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents());
    }

    @Property
    void should_fail_to_mark_cancelled_reminder_as_done(@ForAll String reminderId) {
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.markAsDone(reminderId))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): mark as done", reminderId);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_assign_pending_reminder(@ForAll String reminderId, @ForAll String assignee) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.assign(reminderId, assignee);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderAssigned(reminderId, new StreamRevision(2), NOW, assignee)
        ));
    }

    @Property
    void should_fail_to_assign_unknown_reminder(@ForAll String reminderId, @ForAll String assignee) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.assign(reminderId, assignee))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_assign_cancelled_reminder(@ForAll String reminderId, @ForAll String assignee) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.assign(reminderId, assignee))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): assign to %s", reminderId, assignee);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_assign_done_reminder(@ForAll String reminderId, @ForAll String assignee) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.assign(reminderId, assignee))
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): assign to %s", reminderId, assignee);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_unassign_pending_reminder(@ForAll String reminderId, @ForAll String assignee) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.unassign(reminderId);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderUnassigned(reminderId, new StreamRevision(2), NOW)
        ));
    }

    @Property
    void should_fail_to_unassign_unknown_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.unassign(reminderId))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_unassign_cancelled_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.unassign(reminderId))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): unassign", reminderId);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_unassign_done_reminder(@ForAll String reminderId) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.unassign(reminderId))
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): unassign", reminderId);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_transfer_pending_reminder(@ForAll String reminderId, @ForAll Country country) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aPendingReminder(reminderId, clock)));

        commandHandler.transfer(reminderId, country);

        then(eventStore).should().save(aggregateWithPendingEvents(
                new ReminderEvent.ReminderUnassigned(reminderId, new StreamRevision(2), NOW),
                new ReminderEvent.ReminderTransferred(reminderId, new StreamRevision(3), NOW, country)
        ));
    }

    @Property
    void should_fail_to_transfer_unknown_reminder(@ForAll String reminderId, @ForAll Country country) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThatIllegalStateException()
                .isThrownBy(() -> commandHandler.transfer(reminderId, country))
                .withMessage("Unexpected command: reminder ID does not exist");
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_transfer_cancelled_reminder(@ForAll String reminderId, @ForAll Country country) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aCancelledReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.transfer(reminderId, country))
                .withMessage("Update denied for reminder '%s' (status: 'CANCELLED'): transfer to %s", reminderId, country);
        then(eventStore).should(never()).save(any());
    }

    @Property
    void should_fail_to_transfer_done_reminder(@ForAll String reminderId, @ForAll Country country) {
        Mockito.reset(eventStore);
        given(eventStore.find(reminderId)).willReturn(Optional.of(aDoneReminder(reminderId, clock)));

        assertThatExceptionOfType(InvalidUpdateDeniedException.class)
                .isThrownBy(() -> commandHandler.transfer(reminderId, country))
                .withMessage("Update denied for reminder '%s' (status: 'DONE'): transfer to %s", reminderId, country);
        then(eventStore).should(never()).save(any());
    }

    private static ReminderAggregate aPendingReminder(final String reminderId, final Clock clock) {
        return new ReminderAggregate(ReminderEventStream.of(
                new ReminderEvent.ReminderScheduled(reminderId, new StreamRevision(1), NOW, "INTERVENTION1", CALL_CUSTOMER, DEFAULT_COUNTRY, A_DATE_IN_THE_FUTURE)
        ), clock);
    }

    private static ReminderAggregate aCancelledReminder(final String reminderId, final Clock clock) {
        return new ReminderAggregate(ReminderEventStream.of(
                new ReminderEvent.ReminderScheduled(reminderId, new StreamRevision(1), NOW, "INTERVENTION1", CALL_CUSTOMER, DEFAULT_COUNTRY, A_DATE_IN_THE_FUTURE),
                new ReminderEvent.ReminderCancelled(reminderId, new StreamRevision(2), NOW)
        ), clock);
    }

    private static ReminderAggregate aDoneReminder(final String reminderId, final Clock clock) {
        return new ReminderAggregate(ReminderEventStream.of(
                new ReminderEvent.ReminderScheduled(reminderId, new StreamRevision(1), NOW, "INTERVENTION1", CALL_CUSTOMER, DEFAULT_COUNTRY, A_DATE_IN_THE_FUTURE),
                new ReminderEvent.ReminderMarkedAsDone(reminderId, new StreamRevision(2), NOW)
        ), clock);
    }

    private static ReminderAggregate aggregateWithPendingEvents(final ReminderEvent... events) {
        return argThat(aggregate -> aggregate.getPendingEvents().size() == events.length
                && aggregate.getPendingEvents().containsAll(asList(events)));
    }

}
