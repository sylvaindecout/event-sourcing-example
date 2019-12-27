package fr.xebia.sdecout.eventsourcing.reminder.domain;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidUpdateDeniedExceptionTest {

    @Property
    void should_initialize_message_with_meaningful_arguments(@ForAll String id, @ForAll ReminderState.ReminderStatus status, @ForAll String message) {
        assertThat(new InvalidUpdateDeniedException(id, status, message).getMessage())
            .isEqualTo("Update denied for reminder '%s' (status: '%s'): %s", id, status, message);
    }

    @Property
    void should_initialize_message_with_all_null_arguments() {
        assertThat(new InvalidUpdateDeniedException(null, null, null).getMessage())
            .isEqualTo("Update denied for reminder 'null' (status: 'null'): null");
    }

}
