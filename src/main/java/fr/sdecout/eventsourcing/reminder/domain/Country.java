package fr.sdecout.eventsourcing.reminder.domain;

import fr.sdecout.annotations.DomainDrivenDesign;

import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;

@DomainDrivenDesign.ValueObject
public record Country(String code) {

    private static final Pattern pattern = Pattern.compile("\\p{Alpha}{2}");

    public Country(final String code) {
        checkArgument(pattern.matcher(code).matches(),
                "Country code is expected to consist in exactly 2 upper-case alphabetic characters - value: '%s'", code);
        this.code = code.toUpperCase();
    }

}
