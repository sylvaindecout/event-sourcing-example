package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.annotations.DomainDrivenDesign;
import fr.sdecout.eventsourcing.EventStream;

import java.util.List;

import static fr.sdecout.eventsourcing.StreamRevision.defaultStreamRevision;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

@DomainDrivenDesign.Aggregate(aggregateRoot = ReminderEventStream.class, members = {ReminderEvent.class})
public record ReminderEventStream(List<? extends ReminderEvent> events) implements EventStream<ReminderEvent, ReminderState> {

    private static final ReminderEventStream EMPTY = new ReminderEventStream(emptyList());
    private static final ReminderState BLANK_STATE = ReminderState.builder().version(defaultStreamRevision()).build();

    static ReminderEventStream of(final ReminderEvent... events) {
        return new ReminderEventStream(asList(events));
    }

    public ReminderState replay() {
        return events.stream().reduce(
                BLANK_STATE,
                ReminderState::apply,
                (formerState, updatedState) -> updatedState
        );
    }

    static ReminderEventStream emptyEventStream() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }
}
