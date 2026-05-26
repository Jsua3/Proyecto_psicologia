export interface Opcion {
  id: number;
  texto: string;
}

export interface Pregunta {
  id: number;
  orden: number;
  enunciado: string;
  puntosCorrecta: number;
  opciones: Opcion[];
}

export interface Escenario {
  id: number;
  orden: number;
  nombre: string;
  contexto: string;
  mapaKey: string;
  preguntas: Pregunta[];
}

export interface Caso {
  id: number;
  titulo: string;
  descripcion: string;
  contextoNarrativo: string;
  activo: boolean;
  createdAt: string;
  escenarios?: Escenario[];
}

export interface CasoRequest {
  titulo: string;
  descripcion: string;
  contextoNarrativo: string;
  escenarios: EscenarioRequest[];
}

export interface EscenarioRequest {
  orden: number;
  nombre: string;
  contexto: string;
  mapaKey: string;
  preguntas: PreguntaRequest[];
}

export interface PreguntaRequest {
  orden: number;
  enunciado: string;
  puntosCorrecta: number;
  opciones: OpcionRequest[];
}

export interface OpcionRequest {
  texto: string;
  esCorrecta: boolean;
  feedbackTexto: string;
  normativaRef: string;
}
