package com.travelmate.restaurants.entity;

import com.travelmate.users.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/*
 * Avis d'un utilisateur CONNECTE sur un restaurant.
 * L'auteur n'est plus un simple texte : c'est une entité User du module 1,
 * récupérée grâce à l'email contenu dans le jeton JWT.
 */
@Entity
@Table(name = "restaurant_reviews")
public class Avis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User auteur;

    @Column(nullable = false)
    private int note;

    @Column(nullable = false, length = 1000)
    private String commentaire;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Sentiment sentiment = Sentiment.NEUTRE;

    @Column(nullable = false)
    private double scoreSentiment;

    /** OLLAMA si l'avis a été analysé par Llama 3, LOCAL sinon. */
    @Column(nullable = false, length = 10)
    private String sourceSentiment = "LOCAL";

    @Column(nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();

    public Avis() {
    }

    public Avis(Restaurant restaurant, User auteur, int note, String commentaire) {
        this.restaurant = restaurant;
        this.auteur = auteur;
        this.note = note;
        this.commentaire = commentaire;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Restaurant getRestaurant() { return restaurant; }
    public void setRestaurant(Restaurant restaurant) { this.restaurant = restaurant; }

    public User getAuteur() { return auteur; }
    public void setAuteur(User auteur) { this.auteur = auteur; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public Sentiment getSentiment() { return sentiment; }
    public void setSentiment(Sentiment sentiment) { this.sentiment = sentiment; }

    public double getScoreSentiment() { return scoreSentiment; }
    public void setScoreSentiment(double scoreSentiment) { this.scoreSentiment = scoreSentiment; }

    public String getSourceSentiment() { return sourceSentiment; }
    public void setSourceSentiment(String sourceSentiment) { this.sourceSentiment = sourceSentiment; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
