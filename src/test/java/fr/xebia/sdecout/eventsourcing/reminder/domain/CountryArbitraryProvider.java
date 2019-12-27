package fr.xebia.sdecout.eventsourcing.reminder.domain;

import fr.xebia.sdecout.eventsourcing.StreamRevision;
import fr.xebia.sdecout.eventsourcing.reminder.domain.ReminderState.ReminderStateBuilder;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.providers.ArbitraryProvider;
import net.jqwik.api.providers.TypeUsage;

import java.time.ZonedDateTime;
import java.util.Set;

import static java.util.Collections.singleton;
import static net.jqwik.api.Arbitraries.*;
import static net.jqwik.api.Combinators.withBuilder;

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
