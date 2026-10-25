package com.travelmate.restaurants.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.util.LinkedHashSet;
import java.util.Set;

/*
 * MODULE 4 - Restaurants
 * Catalogue des restaurants. La note moyenne et le nombre d'avis sont stockes ici
 * pour que la recherche et le tri restent rapides, et recalculés à chaque nouvel avis.
 */
@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nom;

    @Column(nullable = false, length = 60)
    private String cuisine;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 200)
    private String adresse;

    @Column(nullable = false, length = 80)
    private String ville;

    // Prix moyen par personne, en euros
    @Column(nullable = false)
    private double prixMoyen;

    @Column(length = 500)
    private String imageUrl;

    // Halal, Végétarien, Vegan, Sans gluten...
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "restaurant_options", joinColumns = @JoinColumn(name = "restaurant_id"))
    @Column(name = "option_alimentaire", length = 40)
    private Set<String> optionsAlimentaires = new LinkedHashSet<>();

    @Column(nullable = false)
    private double noteMoyenne;

    @Column(nullable = false)
    private int nombreAvis;

    public Restaurant() {
    }

    public Restaurant(String nom, String cuisine, String description, String adresse, String ville,
                      double prixMoyen, String imageUrl, Set<String> optionsAlimentaires) {
        this.nom = nom;
        this.cuisine = cuisine;
        this.description = description;
        this.adresse = adresse;
        this.ville = ville;
        this.prixMoyen = prixMoyen;
        this.imageUrl = imageUrl;
        this.optionsAlimentaires = optionsAlimentaires;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getCuisine() { return cuisine; }
    public void setCuisine(String cuisine) { this.cuisine = cuisine; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public double getPrixMoyen() { return prixMoyen; }
    public void setPrixMoyen(double prixMoyen) { this.prixMoyen = prixMoyen; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Set<String> getOptionsAlimentaires() { return optionsAlimentaires; }
    public void setOptionsAlimentaires(Set<String> optionsAlimentaires) { this.optionsAlimentaires = optionsAlimentaires; }

    public double getNoteMoyenne() { return noteMoyenne; }
    public void setNoteMoyenne(double noteMoyenne) { this.noteMoyenne = noteMoyenne; }

    public int getNombreAvis() { return nombreAvis; }
    public void setNombreAvis(int nombreAvis) { this.nombreAvis = nombreAvis; }
}
