package fr.xebia.sdecout.eventsourcing;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

import static fr.xebia.sdecout.eventsourcing.StreamRevision.defaultStreamRevision;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class StreamRevisionTest {

    @Property
    void should_increment_revision(@ForAll StreamRevision revision) {
        assertThat(revision.next().getValue())
            .isEqualTo(revision.getValue() + 1);
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

    @Property
    void should_set_default_revision_to_0() {
        assertThat(defaultStreamRevision().getValue()).isZero();
    }

}
