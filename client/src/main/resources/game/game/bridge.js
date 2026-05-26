// bridge.js — comunicación bidireccional Phaser ↔ JavaFX

window.PsychoSimBridge = {
  _callbacks: {},

  // Llamado por Phaser para guardar una respuesta
  saveAnswer: function(preguntaId, opcionId, tiempoMs) {
    if (window.javaBridge) {
      window.javaBridge.saveAnswer(preguntaId, opcionId, tiempoMs);
    } else {
      console.warn('[Bridge] javaBridge no disponible — modo demo');
      // Simular respuesta en modo demo
      setTimeout(() => {
        window.onRespuestaRecibida(JSON.stringify({
          success: true,
          data: {
            esCorrecta: Math.random() > 0.5,
            feedback: 'Respuesta de prueba (modo demo)',
            normativaRef: null,
            puntosObtenidos: 10,
            opcionCorrectaId: opcionId
          }
        }));
      }, 300);
    }
  },

  finalizarSesion: function(sesionId) {
    if (window.javaBridge) {
      window.javaBridge.finalizarSesion(sesionId);
    } else {
      setTimeout(() => {
        window.onSesionFinalizada(JSON.stringify({
          success: true,
          data: { puntajeTotal: 60, completado: true }
        }));
      }, 300);
    }
  },

  getSesionId: function() {
    return window.PSYCHOSIM_SESION_ID || -1;
  },

  getCasoId: function() {
    return window.PSYCHOSIM_CASO_ID || -1;
  }
};

// Callbacks invocados por JavaFX → Phaser
window.onRespuestaRecibida = function(jsonStr) {
  var data = JSON.parse(jsonStr);
  if (window.PsychoSimBridge._callbacks.onRespuesta) {
    window.PsychoSimBridge._callbacks.onRespuesta(data);
  }
};

window.onSesionFinalizada = function(jsonStr) {
  var data = JSON.parse(jsonStr);
  if (window.PsychoSimBridge._callbacks.onFinalizar) {
    window.PsychoSimBridge._callbacks.onFinalizar(data);
  }
};

window.onCasoDataRecibida = function(jsonStr) {
  var data = JSON.parse(jsonStr);
  if (window.PsychoSimBridge._callbacks.onCasoData) {
    window.PsychoSimBridge._callbacks.onCasoData(data);
  }
};
