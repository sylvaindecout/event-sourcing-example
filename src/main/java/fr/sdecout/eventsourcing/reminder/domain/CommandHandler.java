package fr.sdecout.eventsourcing.reminder.domain;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class CommandHandler {

    /*
    FIXME: For one reminder ID, only 1 DB node (application nodes are irrelevant as long as they access the same DB) should be involved.
    Otherwise, the decision function is likely to decide based on an outdated state.
     */

    public static final Country DEFAULT_COUNTRY = new Country("FR"); // This should depend on configuration

    private final ReminderEventStore eventStore;
    private final ReminderIdGenerator idGenerator;
    private final Clock clock;

    public CommandHandler(final ReminderEventStore eventStore,
                          final ReminderIdGenerator idGenerator,
                          final Clock clock) {
        this.eventStore = eventStore;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    public ReminderState schedule(final ReminderType reminderType, final String interventionId, final ZonedDateTime scheduledTime) {
        final var reminderId = idGenerator.generate();
        final var aggregate = handleFirstCommand(reminderId, () -> ReminderAggregate.scheduleNewReminder(reminderId, interventionId, reminderType, DEFAULT_COUNTRY, scheduledTime, clock));
        return aggregate.state();
    }

    public void reschedule(final String reminderId, final ZonedDateTime scheduledTime) {
        handle(reminderId, reminderAggregate -> reminderAggregate.reschedule(scheduledTime));
    }

    public void reopen(final String reminderId) {
        handle(reminderId, ReminderAggregate::reopen);
    }

    public void cancel(final String reminderId) {
        handle(reminderId, ReminderAggregate::cancel);
    }

    public void markAsDone(final String reminderId) {
        handle(reminderId, ReminderAggregate::markAsDone);
    }

    public void assign(final String reminderId, final String operator) {
        handle(reminderId, reminderAggregate -> reminderAggregate.assignTo(operator));
    }

    public void unassign(final String reminderId) {
        handle(reminderId, reminderAggregate -> reminderAggregate.unassign());
    }

    public void transfer(final String reminderId, final Country country) {
        handle(reminderId, reminderAggregate -> reminderAggregate.transferTo(country));
    }

    private ReminderAggregate handleFirstCommand(final String reminderId, final Supplier<ReminderAggregate> decide) {
        final var aggregate = eventStore.find(reminderId);
        if (aggregate.isPresent()) {
            throw new IllegalStateException("Unexpected command: reminder ID is already present");
        }
        final var updatedAggregate = decide.get();
        eventStore.save(updatedAggregate);
        return updatedAggregate;
    }

    private void handle(final String reminderId, final Consumer<ReminderAggregate> decide) {
        final var aggregate = eventStore.find(reminderId)
                .orElseThrow(() -> new IllegalStateException("Unexpected command: reminder ID does not exist"));
        decide.accept(aggregate);
        eventStore.save(aggregate);
    }

}
