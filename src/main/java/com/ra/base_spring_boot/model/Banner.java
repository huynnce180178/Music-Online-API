package com.ra.base_spring_boot.model;

import com.ra.base_spring_boot.model.base.BaseObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Tag(name = "Banner")
public class Banner extends BaseObject {
    @Column(name = "url")
    private String url;
    @Column(name = "position",unique = true)
    private String position;

}
