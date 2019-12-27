package fr.sdecout.eventsourcing;

import java.time.Clock;
import java.util.List;

public interface Aggregate<EVENT extends Event, STATE extends State<EVENT>> {
    List<? super EVENT> getPendingEvents();
    Clock getClock();
    STATE getState();
}
