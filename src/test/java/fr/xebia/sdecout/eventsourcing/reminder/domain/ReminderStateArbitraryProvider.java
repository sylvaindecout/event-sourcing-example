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

public class ReminderStateArbitraryProvider implements ArbitraryProvider {

    @Override
    public boolean canProvideFor(final TypeUsage targetType) {
        return targetType.isOfType(ReminderState.class);
    }

    @Override
    public Set<Arbitrary<?>> provideFor(final TypeUsage targetType, final SubtypeProvider subtypeProvider) {
        return singleton(
                withBuilder(ReminderState::builder)
                        .use(defaultFor(StreamRevision.class)).in(ReminderStateBuilder::version)
                        .use(strings().injectNull(.1)).in(ReminderStateBuilder::id)
                        .use(strings().injectNull(.1)).in(ReminderStateBuilder::interventionId)
                        .use(of(ReminderState.ReminderStatus.class).injectNull(.1)).in(ReminderStateBuilder::status)
                        .use(of(ReminderType.class).injectNull(.1)).in(ReminderStateBuilder::type)
                        .use(strings().injectNull(.1)).in(ReminderStateBuilder::assignee)
                        .use(defaultFor(Country.class).injectNull(.1)).in(ReminderStateBuilder::country)
                        .use(defaultFor(ZonedDateTime.class).injectNull(.1)).in(ReminderStateBuilder::scheduledTime)
                        .build(ReminderStateBuilder::build)
        );
    }

}
