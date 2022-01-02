package fr.sdecout.eventsourcing.reminder.domain;

import net.jqwik.api.Assume;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.time.Instant;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class ReminderStateTest {

    @Property
    void should_fail_to_apply_null_event(@ForAll ReminderState formerState) {
        assertThatNullPointerException()
                .isThrownBy(() -> formerState.apply(null));
    }

    @Property
    void should_fail_to_apply_event_with_unexpected_version(@ForAll ReminderState formerState, @ForAll ReminderEvent event) {
        Assume.that(!event.version().isNext(formerState.version()));

        assertThatIllegalStateException()
                .isThrownBy(() -> formerState.apply(event))
                .withMessage("Inconsistent stream revision for stream '%s': %s (expected: %s)",
                        event.streamId(), event.version(), formerState.version().next());
    }

    @Property
    void should_fail_to_apply_unexpected_event(@ForAll ReminderState formerState) {
        final var event = mock(ReminderEvent.class);
        given(event.version()).willReturn(formerState.version().next());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> formerState.apply(event))
                .withMessage("Unexpected event type: %s", event.getClass().getName());
    }

    @Property
    void should_apply_ReminderMarkedAsDone_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final var event = new ReminderEvent.ReminderMarkedAsDone(reminderId, formerState.version().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(ReminderState.ReminderStatus.DONE)
                .type(formerState.type())
                .country(formerState.country())
                .assignee(formerState.assignee())
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderScheduled_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp,
                                              @ForAll String interventionId, @ForAll ReminderType reminderType, @ForAll Country country, @ForAll ZonedDateTime scheduledTime) {
        final var event = new ReminderEvent.ReminderScheduled(reminderId, formerState.version().next(), timestamp, interventionId, reminderType, country, scheduledTime);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(event.reminderId())
                .interventionId(event.interventionId())
                .status(ReminderState.ReminderStatus.PENDING)
                .type(event.reminderType())
                .country(event.country())
                .assignee(formerState.assignee())
                .scheduledTime(event.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderRescheduled_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                                @ForAll Instant timestamp, @ForAll ZonedDateTime scheduledTime) {
        final var event = new ReminderEvent.ReminderRescheduled(reminderId, formerState.version().next(), timestamp, scheduledTime);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(formerState.status())
                .type(formerState.type())
                .country(formerState.country())
                .assignee(formerState.assignee())
                .scheduledTime(scheduledTime)
                .build());
    }

    @Property
    void should_apply_ReminderReopened_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final var event = new ReminderEvent.ReminderReopened(reminderId, formerState.version().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(ReminderState.ReminderStatus.PENDING)
                .type(formerState.type())
                .country(formerState.country())
                .assignee(formerState.assignee())
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderAssigned_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                             @ForAll Instant timestamp, @ForAll String assignee) {
        final var event = new ReminderEvent.ReminderAssigned(reminderId, formerState.version().next(), timestamp, assignee);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(formerState.status())
                .type(formerState.type())
                .country(formerState.country())
                .assignee(event.assignee())
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderUnassigned_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                               @ForAll Instant timestamp) {
        final var event = new ReminderEvent.ReminderUnassigned(reminderId, formerState.version().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(formerState.status())
                .type(formerState.type())
                .country(formerState.country())
                .assignee(null)
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderTransferred_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                                @ForAll Instant timestamp, @ForAll Country country) {
        final var event = new ReminderEvent.ReminderTransferred(reminderId, formerState.version().next(), timestamp, country);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(formerState.status())
                .type(formerState.type())
                .country(event.country())
                .assignee(formerState.assignee())
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderCancelled_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final var event = new ReminderEvent.ReminderCancelled(reminderId, formerState.version().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.version().next())
                .id(formerState.id())
                .interventionId(formerState.interventionId())
                .status(ReminderState.ReminderStatus.CANCELLED)
                .type(formerState.type())
                .country(formerState.country())
                .assignee(formerState.assignee())
                .scheduledTime(formerState.scheduledTime())
                .build());
    }

}
