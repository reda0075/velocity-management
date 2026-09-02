import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../../core/services/auth';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [],
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss'
})
export class Topbar {
  private router = inject(Router);
  private auth = inject(AuthService);

  private navigationSignal = toSignal(
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)),
    { initialValue: null }
  );

  pageTitle = computed(() => {
    this.navigationSignal();
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return (route.data['heading'] as string) ?? 'Velocity Management';
  });

  displayName = computed(() => {
    const user = this.auth.currentUser();
    return user?.username ?? '';
  });

  onLogout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
