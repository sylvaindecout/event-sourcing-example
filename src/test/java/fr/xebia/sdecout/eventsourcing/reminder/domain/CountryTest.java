package fr.xebia.sdecout.eventsourcing.reminder.domain;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.AlphaChars;
import net.jqwik.api.constraints.StringLength;
import net.jqwik.api.constraints.UpperChars;

import static org.assertj.core.api.Assertions.*;

class CountryTest {

    @Property
    void should_initialize_from_valid_code(@ForAll @StringLength(2) @UpperChars String code) {
        assertThat(new Country(code).getCode()).isEqualTo(code);
    }

    @Property
    void should_initialize_from_code_with_lower_case(@ForAll @StringLength(2) @AlphaChars String code) {
        assertThat(new Country(code).getCode()).isEqualTo(code.toUpperCase());
    }

    @Property
    void should_fail_to_initialize_from_null_code() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Country(null));
    }

    @Property
    void should_fail_to_initialize_from_code_too_short(@ForAll @StringLength(max = 1) @UpperChars String code) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Country(code))
                .withMessage("Country code is expected to consist in exactly 2 upper-case alphabetic characters - value: '%s'", code);
    }

    @Property
    void should_fail_to_initialize_from_code_too_long(@ForAll @StringLength(min = 3) @UpperChars String code) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Country(code))
                .withMessage("Country code is expected to consist in exactly 2 upper-case alphabetic characters - value: '%s'", code);
    }

    @Property
    void should_fail_to_initialize_from_code_with_non_alpha_characters(@ForAll @StringLength(2) String code) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new Country(code))
                .withMessage("Country code is expected to consist in exactly 2 upper-case alphabetic characters - value: '%s'", code);
    }

}