package fr.xebia.sdecout.eventsourcing.time;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static java.time.ZoneId.getAvailableZoneIds;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static net.jqwik.api.Arbitraries.of;

public final class ZoneIdArbitraryProvider implements ArbitraryProvider {

    private static final List<ZoneId> ZONE_IDS = getAvailableZoneIds().stream()
            .map(ZoneId::of)
            .collect(toList());

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(ZoneId.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                of(ZONE_IDS)
        );
    }

}
