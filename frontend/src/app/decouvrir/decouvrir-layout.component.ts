import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { IconComponent } from '../components/icon/icon.component';

// Integration du module 3 (Fatma) : sous-menu commun a ses 4 pages.
// Dans son application d'origine, ces liens etaient dans sa propre barre de menu ;
// ici le menu principal n'a qu'un lien "Destinations" et ce sous-menu donne acces au reste.
@Component({
  selector: 'tm-decouvrir-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, IconComponent],
  template: `
    <nav class="subnav" aria-label="Destinations et activités">
      <div class="inner">
        <a routerLink="/destinations" routerLinkActive="active" ariaCurrentWhenActive="page">
          <tm-icon name="map" [size]="20"></tm-icon> Destinations
        </a>
        <a routerLink="/activites" routerLinkActive="active" ariaCurrentWhenActive="page">
          <tm-icon name="compass" [size]="20"></tm-icon> Activités
        </a>
        <a routerLink="/recommandations" routerLinkActive="active" ariaCurrentWhenActive="page" class="ai">
          <tm-icon name="sparkles" [size]="20"></tm-icon> Recommandations IA
        </a>
        <a routerLink="/favoris" routerLinkActive="active" ariaCurrentWhenActive="page">
          <tm-icon name="heart" [size]="20"></tm-icon> Mes favoris
        </a>
      </div>
    </nav>
    <router-outlet />
  `,
  styles: `
    .subnav { background: #fff; border-bottom: 1px solid var(--tm-mist); }
    .inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; display: flex; gap: 4px; overflow-x: auto; }
    a {
      display: inline-flex; align-items: center; gap: 8px; height: 48px; padding: 0 16px; white-space: nowrap;
      color: var(--tm-ink); font: 500 14px/20px var(--font-body); border-bottom: 2px solid transparent;
    }
    a:hover { color: var(--tm-teal); text-decoration: none; }
    a.active { color: var(--tm-teal); border-bottom-color: var(--tm-teal); font-weight: 600; }
    a.ai tm-icon { color: var(--tm-violet); }   /* violet reserve a l'IA (charte) */
    a:focus-visible { outline: 2px solid var(--tm-teal); outline-offset: -2px; }
  `,
})
export class DecouvrirLayoutComponent {}
