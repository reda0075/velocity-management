import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.getToken();

  if (token) {
    const cloned = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
    return next(cloned).pipe(catchError(error => handleUnauthorized(error, auth, router)));
  }

  return next(req).pipe(catchError(error => handleUnauthorized(error, auth, router)));
};

function handleUnauthorized(error: { status?: number }, auth: AuthService, router: Router) {
  if (error.status === 401 && !router.url.startsWith('/login')) {
    const returnUrl = router.url;
    auth.logout();
    router.navigate(['/login'], { queryParams: { returnUrl } });
  }

  return throwError(() => error);
}
