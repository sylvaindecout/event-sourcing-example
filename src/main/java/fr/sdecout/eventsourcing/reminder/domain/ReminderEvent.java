package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Event;
import fr.sdecout.eventsourcing.StreamRevision;
import lombok.NonNull;
import lombok.Value;

import java.time.Instant;
import java.time.ZonedDateTime;

public interface ReminderEvent extends Event {

    String getReminderId();

    default String getStreamId() {
        return this.getReminderId();
    }

    @Value
    final class ReminderMarkedAsDone implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
    }

    @Value
    final class ReminderScheduled implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
        @NonNull String interventionId;
        @NonNull ReminderType reminderType;
        @NonNull Country country;
        @NonNull ZonedDateTime scheduledTime;
    }

    @Value
    final class ReminderRescheduled implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
        @NonNull ZonedDateTime scheduledTime;
    }

    @Value
    final class ReminderReopened implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
    }

    @Value
    final class ReminderAssigned implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
        @NonNull String assignee;
    }

    @Value
    final class ReminderTransferred implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
        @NonNull Country country;
    }

    @Value
    final class ReminderCancelled implements ReminderEvent {
        @NonNull String reminderId;
        @NonNull StreamRevision version;
        @NonNull Instant timestamp;
    }
}
