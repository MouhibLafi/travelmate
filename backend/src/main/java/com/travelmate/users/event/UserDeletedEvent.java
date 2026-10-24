package com.travelmate.users.event;

// Evenement publie par le module 1 JUSTE AVANT de supprimer un utilisateur.
// Chaque module qui a des donnees liees a un utilisateur (voyages, avis, favoris...) l'ecoute
// et supprime ses donnees. Exemple : TripService.onUserDeleted() dans le module 2.
public record UserDeletedEvent(Long userId) {
}
