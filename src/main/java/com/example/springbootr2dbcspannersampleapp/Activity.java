package com.example.springbootr2dbcspannersampleapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;

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
