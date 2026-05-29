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
  casoId: number | null;
  caseVersionId: number | null;
  totalSesiones: number;
  puntajePromedio: number;
  tasaAciertos: number;
  tiempoPromedioMs: number;
  estudiantes: EstudianteReporte[];
  simulacion: ReporteSimulacionGrupo | null;
}

export interface ReporteSimulacionGrupo {
  totalIntentos: number;
  intentosCompletados: number;
  intentosEnProgreso: number;
  intentosSalidaSegura: number;
  puntajePromedio: number;
  decisionesAdecuadas: number;
  decisionesRiesgosas: number;
  decisionesInadecuadas: number;
  bitacorasRegistradas: number;
  rubricasAplicadas: number;
  estudiantes: EstudianteSimulacionReporte[];
}

export interface EstudianteSimulacionReporte {
  id: number;
  nombre: string;
  totalIntentos: number;
  intentosCompletados: number;
  intentosEnProgreso: number;
  intentosSalidaSegura: number;
  puntajePromedio: number;
  decisionesAdecuadas: number;
  decisionesRiesgosas: number;
  decisionesInadecuadas: number;
  bitacorasRegistradas: number;
  rubricasAplicadas: number;
  estado: string;
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
  simulacionesCompletadas: number;
  simulacionesEnProgreso: number;
  puntajePromedioSimulacion: number;
  decisionesAdecuadas: number;
  decisionesRiesgosas: number;
  decisionesInadecuadas: number;
  decisionesProhibidas: number;
  ultimosIntentos: SimulacionResumen[];
  intentosRecientes: IntentoReciente[];
}

export interface SimulacionResumen {
  id: string;
  casoTitulo: string;
  estudiante: string;
  puntaje: number;
  estado: string;
}

export interface IntentoReciente {
  id: string;
  casoTitulo: string;
  estudiante: string;
  puntaje: number;
  estado: string;
  origen: 'LEGACY' | 'SIMULATION';
}

export interface UltimaSesion {
  id: number;
  casoTitulo: string;
  estudiante: string;
  puntaje: number;
  completado: boolean;
}
