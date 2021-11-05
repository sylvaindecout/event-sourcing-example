package fr.sdecout.eventsourcing.reminder.domain;

import lombok.AllArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
public final class QueryHandler {

    private final ReminderEventStore eventStore;

    public Optional<ReminderState> getReminder(@NonNull final String interventionId, @NonNull final String reminderId) {
        return eventStore.find(reminderId)
                .filter(reminderAggregate -> interventionId.equals(reminderAggregate.getState().getInterventionId()))
                .map(ReminderAggregate::getState);
    }

    public List<ReminderState> getReminders(@NonNull final String interventionId) {
        return eventStore.findByIntervention(interventionId).stream()
                .map(ReminderAggregate::getState)
                .collect(toList());
    }

    //TODO: how about global search? (/type, /status, /timerange)

}
