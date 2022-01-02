package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Event;
import fr.sdecout.eventsourcing.State;
import fr.sdecout.eventsourcing.StreamRevision;

import java.time.ZonedDateTime;

import static java.lang.String.format;

public record ReminderState(StreamRevision version, String id,
                            String interventionId,
                            ReminderStatus status,
                            ReminderType type, String assignee,
                            Country country,
                            ZonedDateTime scheduledTime) implements State<ReminderEvent> {

    static ReminderStateBuilder builder() {
        return new ReminderStateBuilder();
    }

    public <T extends ReminderEvent> ReminderState apply(final T event) {
        validateVersion(event);
        final var builder = this.toBuilder()
                .version(event.version());
        if (event instanceof ReminderEvent.ReminderMarkedAsDone) {
            builder.status(ReminderStatus.DONE);
        } else if (event instanceof ReminderEvent.ReminderScheduled) {
            builder.id(event.reminderId())
                    .interventionId(((ReminderEvent.ReminderScheduled) event).interventionId())
                    .status(ReminderStatus.PENDING)
                    .type(((ReminderEvent.ReminderScheduled) event).reminderType())
                    .country(((ReminderEvent.ReminderScheduled) event).country())
                    .scheduledTime(((ReminderEvent.ReminderScheduled) event).scheduledTime());
        } else if (event instanceof ReminderEvent.ReminderRescheduled) {
            builder.scheduledTime(((ReminderEvent.ReminderRescheduled) event).scheduledTime());
        } else if (event instanceof ReminderEvent.ReminderReopened) {
            builder.status(ReminderStatus.PENDING);
        } else if (event instanceof ReminderEvent.ReminderAssigned) {
            builder.assignee(((ReminderEvent.ReminderAssigned) event).assignee());
        } else if (event instanceof ReminderEvent.ReminderUnassigned) {
            builder.assignee(null);
        } else if (event instanceof ReminderEvent.ReminderTransferred) {
            builder.country(((ReminderEvent.ReminderTransferred) event).country());
        } else if (event instanceof ReminderEvent.ReminderCancelled) {
            builder.status(ReminderStatus.CANCELLED);
        } else {
            throw new IllegalArgumentException("Unexpected event type: " + event.getClass().getName());
        }
        return builder.build();
    }

    private void validateVersion(final Event event) {
        if (!event.version().isNext(this.version())) {
            throw new IllegalStateException(format("Inconsistent stream revision for stream '%s': %s (expected: %s)",
                    event.streamId(), event.version(), this.version().next()));
        }
    }

    ReminderStateBuilder toBuilder() {
        return new ReminderStateBuilder()
                .version(this.version)
                .id(this.id)
                .interventionId(this.interventionId)
                .status(this.status)
                .type(this.type)
                .assignee(this.assignee)
                .country(this.country)
                .scheduledTime(this.scheduledTime);
    }

    public enum ReminderStatus {
        PENDING, CANCELLED, DONE
    }

    static class ReminderStateBuilder {
        private StreamRevision version;
        private String id;
        private String interventionId;
        private ReminderStatus status;
        private ReminderType type;
        private String assignee;
        private Country country;
        private ZonedDateTime scheduledTime;

        ReminderStateBuilder() {
        }

        ReminderStateBuilder version(StreamRevision version) {
            this.version = version;
            return this;
        }

        ReminderStateBuilder id(String id) {
            this.id = id;
            return this;
        }

        ReminderStateBuilder interventionId(String interventionId) {
            this.interventionId = interventionId;
            return this;
        }

        ReminderStateBuilder status(ReminderStatus status) {
            this.status = status;
            return this;
        }

        ReminderStateBuilder type(ReminderType type) {
            this.type = type;
            return this;
        }

        ReminderStateBuilder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }

        ReminderStateBuilder country(Country country) {
            this.country = country;
            return this;
        }

        ReminderStateBuilder scheduledTime(ZonedDateTime scheduledTime) {
            this.scheduledTime = scheduledTime;
            return this;
        }

        ReminderState build() {
            return new ReminderState(version, id, interventionId, status, type, assignee, country, scheduledTime);
        }

        public String toString() {
            return "ReminderState.ReminderStateBuilder(version=" + this.version + ", id=" + this.id + ", interventionId=" + this.interventionId + ", status=" + this.status + ", type=" + this.type + ", assignee=" + this.assignee + ", country=" + this.country + ", scheduledTime=" + this.scheduledTime + ")";
        }
    }
}
