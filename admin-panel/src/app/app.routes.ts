import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/public/landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'login',
    loadComponent: () => import('./features/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'portal',
    canActivate: [authGuard],
    loadComponent: () => import('./shared/layout/shell.component').then(m => m.ShellComponent),
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'casos',
        loadComponent: () => import('./features/casos/caso-list.component').then(m => m.CasoListComponent)
      },
      {
        path: 'casos/nuevo',
        loadComponent: () => import('./features/casos/caso-form.component').then(m => m.CasoFormComponent)
      },
      {
        path: 'casos/:id/editar',
        loadComponent: () => import('./features/casos/caso-form.component').then(m => m.CasoFormComponent)
      },
      {
        path: 'simulador',
        loadComponent: () => import('./features/simulator/simulation-catalog.component').then(m => m.SimulationCatalogComponent)
      },
      {
        path: 'simulador/:caseVersionId',
        loadComponent: () => import('./features/simulator/simulation-play.component').then(m => m.SimulationPlayComponent)
      },
      {
        path: 'casos/:caseVersionId/editor',
        loadComponent: () => import('./features/simulator/case-editor.component').then(m => m.CaseEditorComponent)
      },
      {
        path: 'docente/trazabilidad',
        loadComponent: () => import('./features/simulator/instructor-trace.component').then(m => m.InstructorTraceComponent)
      },
      {
        path: 'grupos',
        loadComponent: () => import('./features/grupos/grupo-list.component').then(m => m.GrupoListComponent)
      },
      {
        path: 'reportes',
        loadComponent: () => import('./features/reportes/reporte-grupo.component').then(m => m.ReporteGrupoComponent)
      }
    ]
  },
  { path: 'dashboard', redirectTo: 'portal/dashboard', pathMatch: 'full' },
  { path: 'casos', redirectTo: 'portal/casos', pathMatch: 'full' },
  { path: 'simulador', redirectTo: 'portal/simulador', pathMatch: 'full' },
  { path: 'docente', redirectTo: 'portal/docente/trazabilidad', pathMatch: 'full' },
  { path: 'grupos', redirectTo: 'portal/grupos', pathMatch: 'full' },
  { path: 'reportes', redirectTo: 'portal/reportes', pathMatch: 'full' },
  { path: '**', redirectTo: 'dashboard' }
];
