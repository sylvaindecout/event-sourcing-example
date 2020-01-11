package fr.xebia.sdecout.eventsourcing.reminder.domain;

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
        Assume.that(!event.getVersion().isNext(formerState.getVersion()));

        assertThatIllegalStateException()
                .isThrownBy(() -> formerState.apply(event))
                .withMessage("Inconsistent stream revision for stream '%s': %s (expected: %s)",
                        event.getStreamId(), event.getVersion(), formerState.getVersion().next());
    }

    @Property
    void should_fail_to_apply_unexpected_event(@ForAll ReminderState formerState) {
        final ReminderEvent event = mock(ReminderEvent.class);
        given(event.getVersion()).willReturn(formerState.getVersion().next());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> formerState.apply(event))
                .withMessage("Unexpected event type: %s", event.getClass().getName());
    }

    @Property
    void should_apply_ReminderMarkedAsDone_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final ReminderEvent event = new ReminderEvent.ReminderMarkedAsDone(reminderId, formerState.getVersion().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(ReminderState.ReminderStatus.DONE)
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderScheduled_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp,
                                          @ForAll String interventionId, @ForAll ReminderType reminderType, @ForAll Country country, @ForAll ZonedDateTime scheduledTime) {
        final ReminderEvent.ReminderScheduled event = new ReminderEvent.ReminderScheduled(reminderId, formerState.getVersion().next(), timestamp, interventionId, reminderType, country, scheduledTime);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(event.getReminderId())
                .interventionId(event.getInterventionId())
                .status(ReminderState.ReminderStatus.PENDING)
                .type(event.getReminderType())
                .country(event.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(event.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderRescheduled_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                            @ForAll Instant timestamp, @ForAll ZonedDateTime scheduledTime) {
        final ReminderEvent event = new ReminderEvent.ReminderRescheduled(reminderId, formerState.getVersion().next(), timestamp, scheduledTime);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(formerState.getStatus())
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(scheduledTime)
                .build());
    }

    @Property
    void should_apply_ReminderReopened_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final ReminderEvent event = new ReminderEvent.ReminderReopened(reminderId, formerState.getVersion().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(ReminderState.ReminderStatus.PENDING)
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderAssigned_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                         @ForAll Instant timestamp, @ForAll String assignee) {
        final ReminderEvent.ReminderAssigned event = new ReminderEvent.ReminderAssigned(reminderId, formerState.getVersion().next(), timestamp, assignee);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(formerState.getStatus())
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(event.getAssignee())
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderUnassigned_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                             @ForAll Instant timestamp) {
        final ReminderEvent.ReminderUnassigned event = new ReminderEvent.ReminderUnassigned(reminderId, formerState.getVersion().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(formerState.getStatus())
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(null)
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderTransferred_event(@ForAll ReminderState formerState, @ForAll String reminderId,
                                            @ForAll Instant timestamp, @ForAll Country country) {
        final ReminderEvent.ReminderTransferred event = new ReminderEvent.ReminderTransferred(reminderId, formerState.getVersion().next(), timestamp, country);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(formerState.getStatus())
                .type(formerState.getType())
                .country(event.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

    @Property
    void should_apply_ReminderCancelled_event(@ForAll ReminderState formerState, @ForAll String reminderId, @ForAll Instant timestamp) {
        final ReminderEvent event = new ReminderEvent.ReminderCancelled(reminderId, formerState.getVersion().next(), timestamp);

        final ReminderState updatedState = formerState.apply(event);

        assertThat(updatedState).isEqualTo(ReminderState.builder()
                .version(formerState.getVersion().next())
                .id(formerState.getId())
                .interventionId(formerState.getInterventionId())
                .status(ReminderState.ReminderStatus.CANCELLED)
                .type(formerState.getType())
                .country(formerState.getCountry())
                .assignee(formerState.getAssignee())
                .scheduledTime(formerState.getScheduledTime())
                .build());
    }

}
