package com.team6.backend.area.domain.entity;

import com.team6.backend.store.domain.entity.Store;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "p_area")
public class Area {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "area_id")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 50)
    private String city;

    @Column(nullable = false, length = 50)
    private String district;

    @Column(nullable = false)
    private boolean is_active;

    @OneToMany(mappedBy = "areaId")
    private List<Store> stores;

    public Area(String name, String city, String district, boolean is_active) {
        this.name = name;
        this.city = city;
        this.district = district;
        this.is_active = is_active;
    }

}
