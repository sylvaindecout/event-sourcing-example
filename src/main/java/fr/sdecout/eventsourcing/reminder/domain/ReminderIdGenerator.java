package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.annotations.HexagonalArchitecture;

import static fr.sdecout.annotations.HexagonalArchitecture.Port.Type.DRIVEN;

@FunctionalInterface
@HexagonalArchitecture.Port(DRIVEN)
public interface ReminderIdGenerator {
    String generate();
}
