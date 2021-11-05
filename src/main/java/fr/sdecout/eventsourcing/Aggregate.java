package fr.sdecout.eventsourcing;

import fr.sdecout.annotations.DomainDrivenDesign;

import java.time.Clock;
import java.util.List;

@DomainDrivenDesign.Aggregate(aggregateRoot = Aggregate.class, members = {Event.class})
public interface Aggregate<EVENT extends Event, STATE extends State<EVENT>> {
    List<? super EVENT> getPendingEvents();
    Clock getClock();
    STATE getState();
}
