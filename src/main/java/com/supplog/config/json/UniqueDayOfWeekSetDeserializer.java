package com.supplog.config.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.supplog.enums.DayOfWeek;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

public class UniqueDayOfWeekSetDeserializer
        extends JsonDeserializer<Set<DayOfWeek>> {

    @Override
    public Set<DayOfWeek> deserialize(
            JsonParser parser,
            DeserializationContext context
    ) throws IOException {
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
                throw JsonMappingException.from(
                        parser,
                        "Routine day cannot be null"
                );
            }

            if (!days.add(day)) {
                throw JsonMappingException.from(
                        parser,
                        "Routine day cannot be repeated: " + day
                );
            }
        }

        return days;
    }
}
