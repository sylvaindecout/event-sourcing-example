package fr.sdecout.eventsourcing.reminder.domain;

import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.util.Set;

import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.strings;

public class CountryArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(Country.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                strings()
                        .alpha()
                        .ofLength(2)
                        .map(Country::new)
        );
    }

}
