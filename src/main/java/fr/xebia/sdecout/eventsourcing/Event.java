package fr.xebia.sdecout.eventsourcing;

import java.time.Instant;

public interface Event {
    String getStreamId();
    StreamRevision getVersion();
    Instant getTimestamp();
}
