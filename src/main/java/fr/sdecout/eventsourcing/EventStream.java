package fr.sdecout.eventsourcing;

import java.util.List;

public interface EventStream<EVENT extends Event, STATE extends State<EVENT>> {
    STATE replay();
    List<? extends EVENT> getEvents();
}
