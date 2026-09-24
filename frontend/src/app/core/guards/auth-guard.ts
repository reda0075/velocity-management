import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot } from '@angular/router';
import { AuthRole, AuthService } from '../services/auth';

export const authGuard: CanActivateFn = (_route: ActivatedRouteSnapshot, state: RouterStateSnapshot) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (auth.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

export const roleGuard = (role: AuthRole): CanActivateFn => () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.hasRole(role) ? true : router.createUrlTree([auth.landingPath()]);
};
