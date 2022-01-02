package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.annotations.DomainDrivenDesign;
import fr.sdecout.eventsourcing.Aggregate;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static fr.sdecout.eventsourcing.reminder.domain.ReminderEventStream.emptyEventStream;
import static fr.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStatus.*;
import static java.time.Instant.now;

@DomainDrivenDesign.Aggregate(aggregateRoot = ReminderAggregate.class, members = {ReminderEvent.class})
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
        final var aggregate = new ReminderAggregate(emptyEventStream(), clock);
        aggregate.schedule(reminderId, interventionId, reminderType, country, scheduledTime);
        return aggregate;
    }

    // TODO: this method could be made abstract
    private <T extends ReminderEvent> void raise(final T event) {
        this.pendingEvents.add(event);
        this.state = this.state.apply(event);
    }

    private void schedule(final String reminderId, final String interventionId, final ReminderType type, final Country country, final ZonedDateTime scheduledTime) {
        if (state.status() == null) {
            raise(new ReminderEvent.ReminderScheduled(reminderId, state.version().next(), now(clock), interventionId, type, country, scheduledTime));
        }
    }

    void reschedule(final ZonedDateTime scheduledTime) {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderRescheduled(state.id(), state.version().next(), now(clock), scheduledTime));
        } else {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "reschedule to " + scheduledTime);
        }
    }

    void assignTo(final String operator) {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderAssigned(state.id(), state.version().next(), now(clock), operator));
        } else {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "assign to " + operator);
        }
    }

    void unassign() {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderUnassigned(state.id(), state.version().next(), now(clock)));
        } else {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "unassign");
        }
    }

    void transferTo(final Country country) {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderUnassigned(state.id(), state.version().next(), now(clock)));
            raise(new ReminderEvent.ReminderTransferred(state.id(), state.version().next(), now(clock), country));
        } else {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "transfer to " + country);
        }
    }

    void markAsDone() {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderMarkedAsDone(state.id(), state.version().next(), now(clock)));
        } else if (state.status() != DONE) {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "mark as done");
        }
    }

    void reopen() {
        if (state.status() != PENDING) {
            raise(new ReminderEvent.ReminderReopened(state.id(), state.version().next(), now(clock)));
        }
    }

    void cancel() {
        if (state.status() == PENDING) {
            raise(new ReminderEvent.ReminderCancelled(state.id(), state.version().next(), now(clock)));
        } else if (state.status() != CANCELLED) {
            throw new InvalidUpdateDeniedException(state.id(), state.status(), "cancel");
        }
    }

    public List<? super ReminderEvent> pendingEvents() {
        return this.pendingEvents;
    }

    public Clock clock() {
        return this.clock;
    }

    public ReminderState state() {
        return this.state;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReminderAggregate that = (ReminderAggregate) o;
        return Objects.equals(pendingEvents, that.pendingEvents) && Objects.equals(clock, that.clock) && Objects.equals(state, that.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pendingEvents, clock, state);
    }

    @Override
    public String toString() {
        return "ReminderAggregate{" +
                "pendingEvents=" + pendingEvents +
                ", clock=" + clock +
                ", state=" + state +
                '}';
    }
}
