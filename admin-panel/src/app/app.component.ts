import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <a class="psy-skip-link" href="#main-content">Saltar al contenido principal</a>
    <router-outlet />
  `
})
export class AppComponent {}
