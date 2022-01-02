package fr.sdecout.eventsourcing;

import fr.sdecout.annotations.DomainDrivenDesign;

import java.util.List;

@DomainDrivenDesign.Aggregate(aggregateRoot = EventStream.class, members = {Event.class})
public interface EventStream<EVENT extends Event, STATE extends State<EVENT>> {
    STATE replay();
    List<? extends EVENT> events();
}
