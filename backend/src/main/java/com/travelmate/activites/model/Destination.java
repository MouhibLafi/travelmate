package com.travelmate.activites.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "destination")
@Getter
@Setter
@NoArgsConstructor
public class Destination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String pays;

    @Column(length = 1000)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    /** Tags séparés par des virgules, ex : "plage,culture,gastronomie". */
    @Column(length = 300)
    private String tags;

    public Destination(String nom, String pays, String description, String imageUrl, String tags) {
        this.nom = nom;
        this.pays = pays;
        this.description = description;
        this.imageUrl = imageUrl;
        this.tags = tags;
    }
}
