import { Routes } from '@angular/router';
import { Shell } from './shared/layout/shell/shell';

export const routes: Routes = [
  {
    path: '',
    component: Shell,
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
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
        path: 'velocities',
        loadComponent: () => import('./features/velocities/velocities').then(m => m.Velocities),
        data: { heading: 'Velocities' }
      }
    ]
  }
];