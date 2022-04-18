package com.example.springbootr2dbcspannersampleapp;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@AllArgsConstructor
@Data
@Table("activities")
public class Activity {

    @Column("id")
    private long id;

    @Column("description")
    private String description;

    @Column("created_at")
    private OffsetDateTime createdAt;

    public static Activity of(Activity a, OffsetDateTime odt) {
        return new Activity(a.getId(), a.getDescription(), odt);
    }
}
