package com.travelmate.activites.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity(name = "ActiviteFavori")   // integration : le module 4 a deja une entite "Favori"
@Table(name = "favori",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "activite_id"}))
@Getter
@Setter
@NoArgsConstructor
public class Favori {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identifiant de l'utilisateur (géré par le module 1). */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "activite_id")
    private Activite activite;

    @Column(nullable = false)
    private LocalDateTime dateAjout = LocalDateTime.now();

    public Favori(Long userId, Activite activite) {
        this.userId = userId;
        this.activite = activite;
    }
}
