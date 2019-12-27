package fr.sdecout.eventsourcing.time;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.Instant;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.longs;

public final class InstantArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(Instant.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                longs().map(Instant::ofEpochMilli)
        );
    }

}
