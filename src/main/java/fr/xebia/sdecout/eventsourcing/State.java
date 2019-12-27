package fr.xebia.sdecout.eventsourcing;

public interface State<EVENT extends Event> {
    StreamRevision getVersion();
    String getId();
    <T extends EVENT> State apply(T event);
}
