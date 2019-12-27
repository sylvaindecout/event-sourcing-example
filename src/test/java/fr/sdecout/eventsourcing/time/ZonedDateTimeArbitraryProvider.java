package fr.sdecout.eventsourcing.time;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.defaultFor;
import static net.jqwik.api.Combinators.combine;

public final class ZonedDateTimeArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(ZonedDateTime.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                combine(
                        Arbitraries.defaultFor(Instant.class),
                        Arbitraries.defaultFor(ZoneId.class)
                ).as(ZonedDateTime::ofInstant)
        );
    }

}
