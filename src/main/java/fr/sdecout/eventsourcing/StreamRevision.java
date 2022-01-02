package fr.sdecout.eventsourcing;

import fr.sdecout.annotations.DomainDrivenDesign;

import static java.lang.String.format;

@DomainDrivenDesign.ValueObject
public record StreamRevision(int value) {

    private static final StreamRevision DEFAULT = new StreamRevision(0);

    public static StreamRevision defaultStreamRevision() {
        return DEFAULT;
    }

    public StreamRevision next() {
        return new StreamRevision(value + 1);
    }

    public boolean isNext(final StreamRevision streamRevision) {
        return this.equals(streamRevision.next());
    }

    @Override
    public String toString() {
        return format("V%s", value);
    }
}
