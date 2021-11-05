package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Event;
import fr.sdecout.eventsourcing.State;
import fr.sdecout.eventsourcing.StreamRevision;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.ZonedDateTime;

import static java.lang.String.format;
import static lombok.AccessLevel.PACKAGE;
import static lombok.AccessLevel.PRIVATE;

@Value
@Builder(access = PACKAGE)
@AllArgsConstructor(access = PRIVATE)
public class ReminderState implements State<ReminderEvent> {

    StreamRevision version;
    String id;
    String interventionId;
    ReminderStatus status;
    ReminderType type;
    String assignee;
    Country country;
    ZonedDateTime scheduledTime;

    public <T extends ReminderEvent> ReminderState apply(final T event) {
        validateVersion(event);
        final var builder = copy(this)
            .version(event.getVersion());
        if (event instanceof ReminderEvent.ReminderMarkedAsDone) {
            builder.status(ReminderStatus.DONE);
        } else if (event instanceof ReminderEvent.ReminderScheduled) {
            builder.id(event.getReminderId())
                .interventionId(((ReminderEvent.ReminderScheduled) event).getInterventionId())
                .status(ReminderStatus.PENDING)
                .type(((ReminderEvent.ReminderScheduled) event).getReminderType())
                .country(((ReminderEvent.ReminderScheduled) event).getCountry())
                .scheduledTime(((ReminderEvent.ReminderScheduled) event).getScheduledTime());
        } else if (event instanceof ReminderEvent.ReminderRescheduled) {
            builder.scheduledTime(((ReminderEvent.ReminderRescheduled) event).getScheduledTime());
        } else if (event instanceof ReminderEvent.ReminderReopened) {
            builder.status(ReminderStatus.PENDING);
        } else if (event instanceof ReminderEvent.ReminderAssigned) {
            builder.assignee(((ReminderEvent.ReminderAssigned) event).getAssignee());
        } else if (event instanceof ReminderEvent.ReminderUnassigned) {
            builder.assignee(null);
        } else if (event instanceof ReminderEvent.ReminderTransferred) {
            builder.country(((ReminderEvent.ReminderTransferred) event).getCountry());
        } else if (event instanceof ReminderEvent.ReminderCancelled) {
            builder.status(ReminderStatus.CANCELLED);
        } else {
            throw new IllegalArgumentException("Unexpected event type: " + event.getClass().getName());
        }
        return builder.build();
    }

    private void validateVersion(final Event event) {
        if (!event.getVersion().isNext(this.getVersion())) {
            throw new IllegalStateException(format("Inconsistent stream revision for stream '%s': %s (expected: %s)",
                event.getStreamId(), event.getVersion(), this.getVersion().next()));
        }
    }

    private static ReminderStateBuilder copy(final ReminderState instance) {
        return builder()
            .id(instance.getId())
            .interventionId(instance.getInterventionId())
            .status(instance.getStatus())
            .type(instance.getType())
            .assignee(instance.getAssignee())
            .country(instance.getCountry())
            .scheduledTime(instance.getScheduledTime());
    }

    public enum ReminderStatus {
        PENDING, CANCELLED, DONE
    }
}
