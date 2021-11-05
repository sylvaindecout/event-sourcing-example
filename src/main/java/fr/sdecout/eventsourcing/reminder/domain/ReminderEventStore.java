package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.annotations.DomainDrivenDesign;

import java.util.List;
import java.util.Optional;

@DomainDrivenDesign.Repository(ReminderAggregate.class)
public interface ReminderEventStore {

    /**
     * Get events and rehydrate aggregate
     */
    Optional<ReminderAggregate> find(String reminderId);
    List<ReminderAggregate> findByIntervention(String interventionId);

    /**
     * Append la liste des events de l'aggregate dans la DB
     * Faire en sorte que l'ajout dans la DB publie un truc écoutable par ailleurs
     */
    void save(ReminderAggregate reminderAggregate);

}
