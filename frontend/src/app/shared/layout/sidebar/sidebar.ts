import { Component, computed, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthRole, AuthService } from '../../../core/services/auth';

const routerDirectives = [RouterLink, RouterLinkActive];

interface NavItem {
  label: string;
  path: string;
  roles?: AuthRole[];
  icon: string; // SVG path data, kept tiny/inline — no icon library dependency
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: routerDirectives,
  templateUrl: './sidebar.html',
  styleUrl: './sidebar.scss'
})
export class Sidebar {
  private auth = inject(AuthService);
  collapsed = signal(false);

  navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'M3 13h4v8H3zM10 3h4v18h-4zM17 8h4v13h-4z' },
    { label: 'Collaborators', path: '/collaborators', icon: 'M16 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm-8 0a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-3.3 0-6 1.6-6 3.6V19h9v-2.4c0-1 .4-1.9 1.1-2.6-1-.6-2.4-1-4.1-1zm8 0c-.5 0-1.1 0-1.7.1.8.9 1.2 1.9 1.2 2.9V19h8v-2.4c0-2-2.7-3.6-7.5-3.6z' },
    { label: 'Rituals', path: '/rituals', icon: 'M12 2a1 1 0 0 1 1 1v1.06A8 8 0 0 1 20 12h1a1 1 0 1 1 0 2h-1a8 8 0 0 1-7 7.94V23a1 1 0 1 1-2 0v-1.06A8 8 0 0 1 4 14H3a1 1 0 1 1 0-2h1a8 8 0 0 1 7-7.94V3a1 1 0 0 1 1-1zm0 4a6 6 0 1 0 6 6 6 6 0 0 0-6-6z' },
    { label: 'Teams', path: '/teams', icon: 'M12 7a3 3 0 1 0 0 6 3 3 0 0 0 0-6zm0-4a7 7 0 0 0-7 7 7 7 0 0 0 7 7 7 7 0 0 0 7-7 7 7 0 0 0-7-7zm0 12a5 5 0 0 1-5-5h10a5 5 0 0 1-5 5z' },
    { label: 'Velocities', path: '/velocities', icon: 'M13 3a1 1 0 0 0-1 1v9.6l-2.3-2.3a1 1 0 1 0-1.4 1.4l4 4a1 1 0 0 0 1.4 0l4-4a1 1 0 1 0-1.4-1.4L14 13.6V4a1 1 0 0 0-1-1zM5 19a1 1 0 1 0 0 2h14a1 1 0 1 0 0-2z' }
  ];

  visibleNavItems = computed(() => {
    const role = this.auth.currentUser()?.role;
    if (role === 'ADMIN') {
      return [
        ...this.navItems,
        { label: 'Reporting', path: '/reporting', icon: 'M4 19h16v2H4zm2-3h3V8H6zm5 0h3V4h-3zm5 0h3v-6h-3z' }
      ];
    }

    if (role === 'COLLABORATOR') {
      return [
        this.navItems[0],
        { label: 'My Profile', path: '/my-profile', icon: 'M12 12a5 5 0 1 0 0-10 5 5 0 0 0 0 10zm0 2c-5 0-9 2.5-9 5.5V22h18v-2.5c0-3-4-5.5-9-5.5z' },
        { label: 'My Team', path: '/my-team', icon: 'M7 11a3 3 0 1 0 0-6 3 3 0 0 0 0 6zm10 0a3 3 0 1 0 0-6 3 3 0 0 0 0 6zM2 20v-2c0-2.2 2.2-4 5-4s5 1.8 5 4v2zm10 0v-2c0-.7-.2-1.4-.5-2 1.1-.8 2.4-1.2 3.5-1.2 2.8 0 5 1.8 5 4V20z' },
        { label: 'My Velocities', path: '/my-velocities', icon: 'M5 19h14v2H5zm2-3h2V8H7zm4 0h2V4h-2zm4 0h2v-6h-2z' }
      ];
    }

    return [];
  });

  toggle(): void {
    this.collapsed.update(v => !v);
  }
}
