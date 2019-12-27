package fr.xebia.sdecout.eventsourcing.reminder.domain;

@FunctionalInterface
public interface ReminderIdGenerator {
    String generate();
}
