import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

const routerDirectives = [RouterLink, RouterLinkActive];

interface NavItem {
  label: string;
  path: string;
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
  collapsed = signal(false);

  navItems: NavItem[] = [
    { label: 'Dashboard', path: '/dashboard', icon: 'M3 13h4v8H3zM10 3h4v18h-4zM17 8h4v13h-4z' },
    { label: 'Collaborators', path: '/collaborators', icon: 'M16 11a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm-8 0a4 4 0 1 0-4-4 4 4 0 0 0 4 4zm0 2c-3.3 0-6 1.6-6 3.6V19h9v-2.4c0-1 .4-1.9 1.1-2.6-1-.6-2.4-1-4.1-1zm8 0c-.5 0-1.1 0-1.7.1.8.9 1.2 1.9 1.2 2.9V19h8v-2.4c0-2-2.7-3.6-7.5-3.6z' },
    { label: 'Rituals', path: '/rituals', icon: 'M12 2a1 1 0 0 1 1 1v1.06A8 8 0 0 1 20 12h1a1 1 0 1 1 0 2h-1a8 8 0 0 1-7 7.94V23a1 1 0 1 1-2 0v-1.06A8 8 0 0 1 4 14H3a1 1 0 1 1 0-2h1a8 8 0 0 1 7-7.94V3a1 1 0 0 1 1-1zm0 4a6 6 0 1 0 6 6 6 6 0 0 0-6-6z' },
    { label: 'Velocities', path: '/velocities', icon: 'M13 3a1 1 0 0 0-1 1v9.6l-2.3-2.3a1 1 0 1 0-1.4 1.4l4 4a1 1 0 0 0 1.4 0l4-4a1 1 0 1 0-1.4-1.4L14 13.6V4a1 1 0 0 0-1-1zM5 19a1 1 0 1 0 0 2h14a1 1 0 1 0 0-2z' }
  ];

  toggle(): void {
    this.collapsed.update(v => !v);
  }
}