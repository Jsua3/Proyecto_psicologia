import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of } from 'rxjs';
import { catchError, map, tap } from 'rxjs/operators';
import { User, LoginResponse } from '../models/user.model';

interface ApiResponse<T> {
  success: boolean;
  message: string | null;
  data: T;
}

interface UserSummary {
  id: number;
  nombre: string;
  apellido: string;
  email: string;
  role: User['role'];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'psychosim_token';
  private readonly API = '/api/auth';

  currentUser = signal<User | null>(this.loadUser());

  constructor(private http: HttpClient, private router: Router) {}

  login(email: string, password: string) {
    return this.http.post<ApiResponse<LoginResponse>>(`${this.API}/login`, { email, password })
      .pipe(tap(res => {
        localStorage.setItem(this.TOKEN_KEY, res.data.token);
        this.currentUser.set(res.data.user);
      }));
  }

  syncCurrentUser(): Observable<User | null> {
    if (!this.getToken()) {
      this.currentUser.set(null);
      return of(null);
    }

    return this.http.get<ApiResponse<UserSummary>>(`${this.API}/me`).pipe(
      tap(res => this.currentUser.set(this.toUser(res.data))),
      map(() => this.currentUser()),
      catchError(() => {
        this.logout();
        return of(null);
      })
    );
  }

  logout() {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }

  hasRole(...roles: string[]): boolean {
    const user = this.currentUser();
    return user ? roles.includes(user.role) : false;
  }

  private loadUser(): User | null {
    const token = localStorage.getItem(this.TOKEN_KEY);
    if (!token) return null;
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return {
        id: payload.userId,
        email: payload.sub,
        role: payload.role,
        nombre: '',
        apellido: ''
      };
    } catch {
      return null;
    }
  }

  private toUser(summary: UserSummary): User {
    return {
      id: summary.id,
      nombre: summary.nombre,
      apellido: summary.apellido,
      email: summary.email,
      role: summary.role
    };
  }
}
