import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Caso, CasoRequest } from '../models/caso.model';
import { map } from 'rxjs/operators';

interface ApiResponse<T> { data: T; }

@Injectable({ providedIn: 'root' })
export class CasoService {
  private http = inject(HttpClient);
  private readonly API = '/api/casos';

  listar() {
    return this.http.get<ApiResponse<Caso[]>>(this.API).pipe(map(r => r.data));
  }

  obtener(id: number) {
    return this.http.get<ApiResponse<Caso>>(`${this.API}/${id}`).pipe(map(r => r.data));
  }

  crear(req: CasoRequest) {
    return this.http.post<ApiResponse<Caso>>(this.API, req).pipe(map(r => r.data));
  }

  actualizar(id: number, req: CasoRequest) {
    return this.http.put<ApiResponse<Caso>>(`${this.API}/${id}`, req).pipe(map(r => r.data));
  }

  eliminar(id: number) {
    return this.http.delete(`${this.API}/${id}`);
  }
}
