package fr.sdecout.eventsourcing.reminder.domain;

import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public final class QueryHandler {

    private final ReminderEventStore eventStore;

    public QueryHandler(final ReminderEventStore eventStore) {
        this.eventStore = eventStore;
    }

    public Optional<ReminderState> getReminder(final String interventionId, final String reminderId) {
        requireNonNull(interventionId);
        requireNonNull(reminderId);
        return eventStore.find(reminderId)
                .filter(reminderAggregate -> interventionId.equals(reminderAggregate.state().interventionId()))
                .map(ReminderAggregate::state);
    }

    public List<ReminderState> getReminders(final String interventionId) {
        requireNonNull(interventionId);
        return eventStore.findByIntervention(interventionId).stream()
                .map(ReminderAggregate::state)
                .collect(toList());
    }

    //TODO: how about global search? (/type, /status, /timerange)

}
