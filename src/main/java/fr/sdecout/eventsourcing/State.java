package fr.sdecout.eventsourcing;

import fr.sdecout.annotations.DomainDrivenDesign;

@DomainDrivenDesign.Entity
public interface State<EVENT extends Event> {
    StreamRevision version();
    String id();
    <T extends EVENT> State apply(T event);
}
