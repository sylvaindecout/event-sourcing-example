package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.eventsourcing.Aggregate;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

import static fr.sdecout.eventsourcing.reminder.domain.ReminderEventStream.emptyEventStream;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStatus.*;
import static java.time.Instant.now;
import static lombok.AccessLevel.PRIVATE;

@Getter
@ToString
@EqualsAndHashCode
@AllArgsConstructor(access = PRIVATE)
public final class ReminderAggregate implements Aggregate<ReminderEvent, ReminderState> {

    private final List<? super ReminderEvent> pendingEvents;
    private final Clock clock;
    private ReminderState state;

    public ReminderAggregate(final ReminderEventStream history, final Clock clock) {
        this.pendingEvents = new LinkedList<>();
        this.clock = clock;
        this.state = history.replay();
    }

    static ReminderAggregate scheduleNewReminder(final String reminderId, final String interventionId, final ReminderType reminderType,
                                                final Country country, final ZonedDateTime scheduledTime, final Clock clock) {
        final ReminderAggregate aggregate = new ReminderAggregate(emptyEventStream(), clock);
        aggregate.schedule(reminderId, interventionId, reminderType, country, scheduledTime);
        return aggregate;
    }

    // TODO: this method could be made abstract
    private <T extends ReminderEvent> void raise(final T event) {
        this.pendingEvents.add(event);
        this.state = this.state.apply(event);
    }

    private void schedule(final String reminderId, final String interventionId, final ReminderType type, final Country country, final ZonedDateTime scheduledTime) {
        if (state.getStatus() == null) {
            raise(new ReminderEvent.ReminderScheduled(reminderId, state.getVersion().next(), now(clock), interventionId, type, country, scheduledTime));
        }
    }

    void reschedule(final ZonedDateTime scheduledTime) {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderRescheduled(state.getId(), state.getVersion().next(), now(clock), scheduledTime));
        } else {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "reschedule to " + scheduledTime);
        }
    }

    void assignTo(final String operator) {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderAssigned(state.getId(), state.getVersion().next(), now(clock), operator));
        } else {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "assign to " + operator);
        }
    }

    void unassign() {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderUnassigned(state.getId(), state.getVersion().next(), now(clock)));
        } else {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "unassign");
        }
    }

    void transferTo(final Country country) {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderUnassigned(state.getId(), state.getVersion().next(), now(clock)));
            raise(new ReminderEvent.ReminderTransferred(state.getId(), state.getVersion().next(), now(clock), country));
        } else {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "transfer to " + country);
        }
    }

    void markAsDone() {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderMarkedAsDone(state.getId(), state.getVersion().next(), now(clock)));
        } else if (state.getStatus() != DONE) {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "mark as done");
        }
    }

    void reopen() {
        if (state.getStatus() != PENDING) {
            raise(new ReminderEvent.ReminderReopened(state.getId(), state.getVersion().next(), now(clock)));
        }
    }

    void cancel() {
        if (state.getStatus() == PENDING) {
            raise(new ReminderEvent.ReminderCancelled(state.getId(), state.getVersion().next(), now(clock)));
        } else if (state.getStatus() != CANCELLED) {
            throw new InvalidUpdateDeniedException(state.getId(), state.getStatus(), "cancel");
        }
    }

}
