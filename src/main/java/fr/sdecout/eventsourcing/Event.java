package fr.sdecout.eventsourcing;

import fr.sdecout.annotations.DomainDrivenDesign;

import java.time.Instant;

@DomainDrivenDesign.Entity
public interface Event {
    String streamId();
    StreamRevision version();
    Instant timestamp();
}
