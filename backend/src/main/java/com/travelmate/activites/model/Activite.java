package com.travelmate.activites.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "activite")
@Getter
@Setter
@NoArgsConstructor
public class Activite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 1000)
    private String description;

    /** Prix par personne en euros (0 = gratuit). */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prix;

    /** Durée en minutes. */
    @Column(nullable = false)
    private Integer dureeMinutes;

    @Column(length = 500)
    private String imageUrl;

    /** Mots-clés séparés par des virgules, ex : "musée,art,histoire". */
    @Column(length = 300)
    private String tags;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "destination_id")
    private Destination destination;
}
