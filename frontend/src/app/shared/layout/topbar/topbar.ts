import { Component, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-topbar',
  standalone: true,
  templateUrl: './topbar.html',
  styleUrl: './topbar.scss'
})
export class Topbar {
  private router = inject(Router);

  private navigationSignal = toSignal(
    this.router.events.pipe(filter(e => e instanceof NavigationEnd)),
    { initialValue: null }
  );

  pageTitle = computed(() => {
    this.navigationSignal(); // re-evaluate on navigation
    let route = this.router.routerState.snapshot.root;
    while (route.firstChild) {
      route = route.firstChild;
    }
    return (route.data['heading'] as string) ?? 'Velocity Management';
  });
}