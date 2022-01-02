package fr.sdecout.eventsourcing;

import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static fr.sdecout.eventsourcing.StreamRevision.defaultStreamRevision;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StreamRevisionTest {

    @Property
    void should_increment_revision(@ForAll StreamRevision revision) {
        assertThat(revision.next().value())
                .isEqualTo(revision.value() + 1);
    }

    @Property
    void should_fail_to_check_that_revision_is_the_next_in_line_for_null(@ForAll StreamRevision revision) {
        assertThatNullPointerException()
                .isThrownBy(() -> revision.isNext(null));
    }

    @Property
    void should_check_that_revision_is_the_next_in_line(@ForAll StreamRevision revision) {
        assertThat(revision.next().isNext(revision)).isTrue();
    }

    @Property
    void should_check_that_revision_is_not_the_next_in_line(@ForAll StreamRevision revision) {
        assertThat(revision.isNext(revision)).isFalse();
    }

    @Example
    void should_set_default_revision_to_0() {
        assertThat(defaultStreamRevision().value()).isZero();
    }

}
