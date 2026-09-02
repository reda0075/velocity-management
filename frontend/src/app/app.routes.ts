import { Routes } from '@angular/router';
import { Shell } from './shared/layout/shell/shell';
import { Login } from './features/auth/login/login';
import { authGuard } from './core/guards/auth-guard';

export const routes: Routes = [
  { path: 'login', component: Login },
  {
    path: '',
    component: Shell,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then(m => m.Dashboard),
        data: { heading: 'Dashboard' }
      },
      {
        path: 'collaborators',
        loadComponent: () => import('./features/collaborators/collaborators').then(m => m.Collaborators),
        data: { heading: 'Collaborators' }
      },
      {
        path: 'rituals',
        loadComponent: () => import('./features/rituals/rituals').then(m => m.Rituals),
        data: { heading: 'Rituals' }
      },
      {
        path: 'teams',
        loadComponent: () => import('./features/teams/teams').then(m => m.Teams),
        data: { heading: 'Teams' }
      },
      {
        path: 'velocities',
        loadComponent: () => import('./features/velocities/velocities').then(m => m.Velocities),
        data: { heading: 'Velocities' }
      }
    ]
  }
];
