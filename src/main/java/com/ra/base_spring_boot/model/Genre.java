package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import jakarta.persistence.*;
import lombok.*;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "genres")
public class Genre extends BaseObject {
    @Column(name = "genre_name", length = 255, nullable = false)
    private String genreName;
    @Column(name="genre_image")
    private String genreImage;
}
