package com.supplog.config.json;

import com.supplog.enums.DayOfWeek;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.LinkedHashSet;
import java.util.Set;

public class UniqueDayOfWeekSetDeserializer
        extends ValueDeserializer<Set<DayOfWeek>> {

    @Override
    public Set<DayOfWeek> deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws JacksonException {
        if (!parser.isExpectedStartArrayToken()) {
            context.reportInputMismatch(
                    Set.class,
                    "Routine days must be provided as an array"
            );
            return Set.of();
        }

        Set<DayOfWeek> days = new LinkedHashSet<>();

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            DayOfWeek day = context.readValue(parser, DayOfWeek.class);

            if (day == null) {
                return context.reportInputMismatch(
                        Set.class,
                        "Routine day cannot be null"
                );
            }

            if (!days.add(day)) {
                return context.reportInputMismatch(
                        Set.class,
                        "Routine day cannot be repeated: " + day
                );
            }
        }

        return days;
    }
}
