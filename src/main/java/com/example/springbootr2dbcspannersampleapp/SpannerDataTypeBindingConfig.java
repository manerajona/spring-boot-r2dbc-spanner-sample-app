package com.example.springbootr2dbcspannersampleapp;

import com.google.cloud.Timestamp;
import com.google.cloud.spanner.r2dbc.springdata.SpannerR2dbcDialect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;

import java.time.OffsetDateTime;
import java.util.List;

@Configuration
public class SpannerDataTypeBindingConfig {

    @Bean
    public R2dbcCustomConversions customConversions() {
        return R2dbcCustomConversions.of(new SpannerR2dbcDialect(),
            List.of(new TimestampToOffsetDateTimeConverter(),
                new OffsetDateTimeToTimestampConverter()));
    }

    @ReadingConverter
    static class TimestampToOffsetDateTimeConverter implements Converter<Timestamp, OffsetDateTime> {

        @Override
        public OffsetDateTime convert(Timestamp row) {
            return OffsetDateTime.parse(row.toString());
        }
    }

    @WritingConverter
    static class OffsetDateTimeToTimestampConverter implements Converter<OffsetDateTime, Timestamp> {

        @Override
        public Timestamp convert(OffsetDateTime source) {
            return Timestamp.parseTimestamp(source.toString());
        }
    }
}
