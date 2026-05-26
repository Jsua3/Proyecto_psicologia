import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map } from 'rxjs/operators';
import { Dashboard, ReporteGrupo } from '../models/sesion.model';

interface ApiResponse<T> { data: T; }

@Injectable({ providedIn: 'root' })
export class ReporteService {
  private http = inject(HttpClient);
  private readonly API = '/api/reportes';

  getDashboard() {
    return this.http.get<ApiResponse<Dashboard>>(`${this.API}/dashboard`).pipe(map(r => r.data));
  }

  getReporteGrupo(grupoId: number, casoId: number) {
    return this.http.get<ApiResponse<ReporteGrupo>>(
      `${this.API}/grupo/${grupoId}?casoId=${casoId}`
    ).pipe(map(r => r.data));
  }

  exportarCsv(grupoId: number, casoId: number) {
    return this.http.get(`${this.API}/grupo/${grupoId}/export?casoId=${casoId}`, {
      responseType: 'blob'
    });
  }
}
