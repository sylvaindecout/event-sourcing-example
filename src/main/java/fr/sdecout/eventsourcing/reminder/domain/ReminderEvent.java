package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Event;
import fr.sdecout.eventsourcing.StreamRevision;

import java.time.Instant;
import java.time.ZonedDateTime;

import static java.util.Objects.requireNonNull;

public interface ReminderEvent extends Event {

    String reminderId();

    default String streamId() {
        return this.reminderId();
    }

    record ReminderMarkedAsDone(String reminderId,
                                StreamRevision version,
                                Instant timestamp) implements ReminderEvent {
        public ReminderMarkedAsDone {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
        }
    }

    record ReminderScheduled(String reminderId, StreamRevision version,
                             Instant timestamp, String interventionId,
                             ReminderType reminderType,
                             Country country,
                             ZonedDateTime scheduledTime) implements ReminderEvent {
        public ReminderScheduled {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
            requireNonNull(interventionId);
            requireNonNull(reminderType);
            requireNonNull(country);
            requireNonNull(scheduledTime);
        }
    }

    record ReminderRescheduled(String reminderId,
                               StreamRevision version,
                               Instant timestamp,
                               ZonedDateTime scheduledTime) implements ReminderEvent {
        public ReminderRescheduled {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
            requireNonNull(scheduledTime);
        }
    }

    record ReminderReopened(String reminderId, StreamRevision version,
                            Instant timestamp) implements ReminderEvent {
        public ReminderReopened {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
        }
    }

    record ReminderAssigned(String reminderId, StreamRevision version,
                            Instant timestamp,
                            String assignee) implements ReminderEvent {
        public ReminderAssigned {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
            requireNonNull(assignee);
        }
    }

    record ReminderUnassigned(String reminderId,
                              StreamRevision version,
                              Instant timestamp) implements ReminderEvent {
        public ReminderUnassigned {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
        }
    }

    record ReminderTransferred(String reminderId,
                               StreamRevision version,
                               Instant timestamp,
                               Country country) implements ReminderEvent {
        public ReminderTransferred {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
            requireNonNull(country);
        }
    }

    record ReminderCancelled(String reminderId, StreamRevision version,
                             Instant timestamp) implements ReminderEvent {
        public ReminderCancelled {
            requireNonNull(reminderId);
            requireNonNull(version);
            requireNonNull(timestamp);
        }
    }
}
