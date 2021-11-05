package fr.sdecout.eventsourcing.reminder.domain;

import net.jqwik.api.Assume;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class QueryHandlerTest {

    private final ReminderEventStore eventStore = mock(ReminderEventStore.class);
    private final QueryHandler queryHandler = new QueryHandler(eventStore);

    @Example
    void should_fail_to_fetch_reminders_for_null_intervention_ID() {
        assertThatNullPointerException()
            .isThrownBy(() -> queryHandler.getReminders(null));
    }

    @Property
    void should_fetch_reminders_for_intervention_with_given_ID(@ForAll String interventionId, @ForAll List<ReminderAggregate> reminders) {
        given(eventStore.findByIntervention(interventionId)).willReturn(reminders);

        assertThat(queryHandler.getReminders(interventionId)).containsExactlyElementsOf(reminders.stream()
                .map(ReminderAggregate::getState)
                .collect(toList()));
    }

    @Property
    void should_fail_to_fetch_reminder_for_given_ID_if_intervention_ID_is_null(@ForAll String reminderId) {
        assertThatNullPointerException()
            .isThrownBy(() -> queryHandler.getReminder(null, reminderId));
    }

    @Property
    void should_fail_to_fetch_reminder_for_null_ID(@ForAll String interventionId) {
        assertThatNullPointerException()
            .isThrownBy(() -> queryHandler.getReminder(interventionId, null));
    }

    @Property
    void should_fetch_reminder_for_given_ID(@ForAll String reminderId, @ForAll ReminderAggregate reminder) {
        given(eventStore.find(reminderId)).willReturn(Optional.of(reminder));

        assertThat(queryHandler.getReminder(reminder.getState().getInterventionId(), reminderId)).contains(reminder.getState());
    }

    @Property
    void should_fail_to_fetch_reminder_for_given_ID_if_intervention_ID_does_not_match(@ForAll String interventionId, @ForAll String reminderId, @ForAll ReminderAggregate reminder) {
        Assume.that(!interventionId.equals(reminder.getState().getInterventionId()));

        given(eventStore.find(reminderId)).willReturn(Optional.of(reminder));

        assertThat(queryHandler.getReminder(interventionId, reminderId)).isEmpty();
    }

    @Property
    void should_fail_to_fetch_reminder_for_unknown_ID(@ForAll String interventionId, @ForAll String reminderId) {
        given(eventStore.find(reminderId)).willReturn(Optional.empty());

        assertThat(queryHandler.getReminder(interventionId, reminderId)).isEmpty();
    }

}
