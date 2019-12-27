package fr.xebia.sdecout.eventsourcing.reminder.domain;

import static java.lang.String.format;

public final class InvalidUpdateDeniedException extends RuntimeException {

    public InvalidUpdateDeniedException(final String id, final ReminderState.ReminderStatus status, final String message) {
        super(format("Update denied for reminder '%s' (status: '%s'): %s", id, status, message));
    }

}
