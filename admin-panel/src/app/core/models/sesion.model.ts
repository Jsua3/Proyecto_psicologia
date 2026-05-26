export interface SesionResumen {
  id: number;
  casoId: number;
  casoTitulo: string;
  fechaInicio: string;
  fechaFin: string | null;
  puntajeTotal: number;
  completado: boolean;
}

export interface ReporteGrupo {
  grupoId: number;
  casoId: number;
  totalSesiones: number;
  puntajePromedio: number;
  tasaAciertos: number;
  tiempoPromedioMs: number;
  estudiantes: EstudianteReporte[];
}

export interface EstudianteReporte {
  id: number;
  nombre: string;
  puntaje: number;
  porcentajeAciertos: number;
  tiempoPromedioMs: number;
  estado: 'COMPLETADO' | 'EN_PROGRESO' | 'PENDIENTE';
}

export interface Dashboard {
  estudiantesActivos: number;
  casosCompletadosHoy: number;
  puntajePromedioGlobal: number;
  ultimasSesiones: UltimaSesion[];
}

export interface UltimaSesion {
  id: number;
  casoTitulo: string;
  estudiante: string;
  puntaje: number;
  completado: boolean;
}
