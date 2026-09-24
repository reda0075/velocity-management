import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth';
import { extractErrorMessage } from '../../../core/utils/http-error';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.html',
  styleUrl: './login.scss'
})
export class Login {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  private auth = inject(AuthService);

  username = signal('');
  password = signal('');
  isLoading = signal(false);
  errorMsg = signal<string | null>(null);

  constructor() {
    if (this.auth.isAuthenticated()) {
      this.router.navigateByUrl(this.auth.landingPath());
    }
  }

  onUsernameChange(value: string): void {
    this.username.set(value);
  }

  onPasswordChange(value: string): void {
    this.password.set(value);
  }

  onSubmit(): void {
    if (!this.username() || !this.password()) {
      this.errorMsg.set('Username and password are required.');
      return;
    }

    this.isLoading.set(true);
    this.errorMsg.set(null);

    this.auth.login({
      username: this.username(),
      password: this.password()
    }).subscribe({
      next: () => {
        this.isLoading.set(false);
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(
          returnUrl?.startsWith('/') ? returnUrl : this.auth.landingPath()
        );
      },
      error: (err: HttpErrorResponse) => {
        this.isLoading.set(false);
        this.errorMsg.set(extractErrorMessage(err, 'Login failed. Please try again.'));
      }
    });
  }
}
