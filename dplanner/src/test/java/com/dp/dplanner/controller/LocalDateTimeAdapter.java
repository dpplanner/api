package com.dp.dplanner.controller;

import com.nimbusds.jose.shaded.gson.TypeAdapter;
import com.nimbusds.jose.shaded.gson.stream.JsonReader;
import com.nimbusds.jose.shaded.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

class LocalDateTimeAdapter extends TypeAdapter<LocalDateTime> {

    private static final String PATTERN_DATE = "yyyy-MM-dd";
    private static final String PATTERN_TIME = "HH:mm:ss";
    private static final String PATTERN_DATETIME = String.format("%s %s", PATTERN_DATE, PATTERN_TIME);

    DateTimeFormatter format = DateTimeFormatter.ofPattern(PATTERN_DATETIME);

    @Override
    public void write(JsonWriter out, LocalDateTime value) throws IOException {
        if(value != null)
            out.value(value.format(format));
    }

    @Override
    public LocalDateTime read(JsonReader in) throws IOException {
        return LocalDateTime.parse(in.nextString(), format);
    }
}