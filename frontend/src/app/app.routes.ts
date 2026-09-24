import { Routes } from '@angular/router';
import { Shell } from './shared/layout/shell/shell';
import { Login } from './features/auth/login/login';
import { authGuard, roleGuard } from './core/guards/auth-guard';

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
        canActivate: [roleGuard('ADMIN')],
        data: { heading: 'Collaborators' }
      },
      {
        path: 'rituals',
        loadComponent: () => import('./features/rituals/rituals').then(m => m.Rituals),
        canActivate: [roleGuard('ADMIN')],
        data: { heading: 'Rituals' }
      },
      {
        path: 'teams',
        loadComponent: () => import('./features/teams/teams').then(m => m.Teams),
        canActivate: [roleGuard('ADMIN')],
        data: { heading: 'Teams' }
      },
      {
        path: 'velocities',
        loadComponent: () => import('./features/velocities/velocities').then(m => m.Velocities),
        canActivate: [roleGuard('ADMIN')],
        data: { heading: 'Velocities' }
      },
      {
        path: 'reporting',
        loadComponent: () => import('./features/reporting/reporting').then(m => m.Reporting),
        canActivate: [roleGuard('ADMIN')],
        data: { heading: 'Reporting' }
      },
      {
        path: 'my-profile',
        loadComponent: () => import('./features/my-profile/my-profile').then(m => m.MyProfile),
        canActivate: [roleGuard('COLLABORATOR')],
        data: { heading: 'My Profile' }
      },
      {
        path: 'my-team',
        loadComponent: () => import('./features/my-team/my-team').then(m => m.MyTeam),
        canActivate: [roleGuard('COLLABORATOR')],
        data: { heading: 'My Team' }
      },
      {
        path: 'my-velocities',
        loadComponent: () => import('./features/my-velocities/my-velocities').then(m => m.MyVelocities),
        canActivate: [roleGuard('COLLABORATOR')],
        data: { heading: 'My Velocities' }
      }
    ]
  }
];
