// DialogScene.js — overlay de preguntas y feedback normativo

class DialogScene extends Phaser.Scene {
  constructor() {
    super({ key: 'DialogScene' });
  }

  init(data) {
    this.pregunta = data.pregunta;
    this.npcNombre = data.npcNombre || 'NPC';
    this.onRespuestaCallback = data.onRespuesta;
    this.respondida = false;
    this.tiempoInicio = Date.now();
  }

  create() {
    const W = this.scale.width;
    const H = this.scale.height;
    const panelH = Math.floor(H * 0.42);
    const panelY = H - panelH;

    // Overlay semitransparente arriba
    this.add.rectangle(0, 0, W, panelY, 0x000000, 0.45).setOrigin(0, 0);

    // Panel principal
    const panel = this.add.graphics();
    panel.fillStyle(0x0A0A1E, 0.97);
    panel.fillRoundedRect(12, panelY + 8, W - 24, panelH - 16, 8);
    panel.lineStyle(3, 0x3A5A8A, 1);
    panel.strokeRoundedRect(12, panelY + 8, W - 24, panelH - 16, 8);

    // Nombre del NPC
    this.add.text(32, panelY + 22, this.npcNombre.toUpperCase(), {
      fontFamily: '"Press Start 2P"',
      fontSize: '8px',
      color: '#7A9EC0'
    });

    // Enunciado
    const enunciado = this.pregunta ? this.pregunta.enunciado : 'Sin pregunta disponible.';
    this.add.text(32, panelY + 44, enunciado, {
      fontFamily: 'Arial',
      fontSize: '13px',
      color: '#ffffff',
      wordWrap: { width: W - 64 },
      lineSpacing: 4
    });

    // Opciones
    this.botonesOpciones = [];
    const opciones = this.pregunta ? this.pregunta.opciones : [];
    const letras = ['A', 'B', 'C', 'D'];
    const startY = panelY + 110;
    const colW = (W - 48) / 2;

    opciones.forEach((op, i) => {
      const col = i % 2;
      const row = Math.floor(i / 2);
      const bx = 24 + col * (colW + 8);
      const by = startY + row * 52;

      const btn = this.add.graphics();
      btn.fillStyle(0x1a2b3c, 1);
      btn.fillRoundedRect(bx, by, colW, 44, 6);
      btn.lineStyle(2, 0x3A5A8A, 1);
      btn.strokeRoundedRect(bx, by, colW, 44, 6);

      const label = this.add.text(bx + 10, by + 14, `${letras[i]}. ${op.texto}`, {
        fontFamily: 'Arial',
        fontSize: '12px',
        color: '#ffffff',
        wordWrap: { width: colW - 20 }
      });

      // Zona interactiva
      const zone = this.add.zone(bx, by, colW, 44).setOrigin(0, 0).setInteractive();
      zone.on('pointerover', () => {
        if (!this.respondida) {
          btn.clear();
          btn.fillStyle(0x2a3b5c, 1);
          btn.fillRoundedRect(bx, by, colW, 44, 6);
          btn.lineStyle(2, 0x7A9EC0, 1);
          btn.strokeRoundedRect(bx, by, colW, 44, 6);
        }
      });
      zone.on('pointerout', () => {
        if (!this.respondida) {
          btn.clear();
          btn.fillStyle(0x1a2b3c, 1);
          btn.fillRoundedRect(bx, by, colW, 44, 6);
          btn.lineStyle(2, 0x3A5A8A, 1);
          btn.strokeRoundedRect(bx, by, colW, 44, 6);
        }
      });
      zone.on('pointerdown', () => this.seleccionarOpcion(i, op, btn, bx, by, colW));

      this.botonesOpciones.push({ btn, label, zone, bx, by, colW, opcion: op });
    });

    // Instrucción
    this.instruccion = this.add.text(W / 2, H - 18, 'Click en una opción para responder', {
      fontFamily: '"Press Start 2P"',
      fontSize: '6px',
      color: 'rgba(255,255,255,0.4)'
    }).setOrigin(0.5, 1);
  }

  seleccionarOpcion(index, opcion, btn, bx, by, colW) {
    if (this.respondida) return;
    this.respondida = true;
    const tiempoMs = Date.now() - this.tiempoInicio;

    // Deshabilitar todas las zonas
    this.botonesOpciones.forEach(b => b.zone.disableInteractive());

    // Callback al bridge
    PsychoSimBridge._callbacks.onRespuesta = (res) => {
      const data = res.success ? res.data : { esCorrecta: false, puntosObtenidos: 0, feedback: '' };
      this.mostrarFeedback(index, data, bx, by, colW);
    };

    PsychoSimBridge.saveAnswer(
      this.pregunta.id,
      opcion.id,
      tiempoMs
    );
  }

  mostrarFeedback(index, resultado, bx, by, colW) {
    const { esCorrecta, feedback, normativaRef, puntosObtenidos, opcionCorrectaId } = resultado;
    const W = this.scale.width;
    const H = this.scale.height;

    // Colorear botón seleccionado
    const seleccionado = this.botonesOpciones[index];
    seleccionado.btn.clear();
    seleccionado.btn.fillStyle(esCorrecta ? 0x1a4a2a : 0x4a1a1a, 1);
    seleccionado.btn.fillRoundedRect(bx, by, colW, 44, 6);
    seleccionado.btn.lineStyle(3, esCorrecta ? 0x27AE60 : 0xC0392B, 1);
    seleccionado.btn.strokeRoundedRect(bx, by, colW, 44, 6);
    seleccionado.label.setColor(esCorrecta ? '#27AE60' : '#C0392B');

    // Mostrar puntos flotantes
    if (puntosObtenidos > 0) {
      const floatText = this.add.text(bx + colW / 2, by - 10, '+' + puntosObtenidos, {
        fontFamily: '"Press Start 2P"',
        fontSize: '10px',
        color: '#27AE60'
      }).setOrigin(0.5);
      this.tweens.add({
        targets: floatText,
        y: by - 50,
        alpha: 0,
        duration: 1200,
        onComplete: () => floatText.destroy()
      });
    }

    // Panel de feedback
    const fbY = H * 0.58 - 20;
    const fbPanel = this.add.graphics();
    fbPanel.fillStyle(esCorrecta ? 0x0a2a0a : 0x2a0a0a, 0.95);
    fbPanel.fillRoundedRect(12, fbY, W - 24, 60, 6);
    fbPanel.lineStyle(2, esCorrecta ? 0x27AE60 : 0xC0392B, 1);
    fbPanel.strokeRoundedRect(12, fbY, W - 24, 60, 6);

    const icon = esCorrecta ? '✓ CORRECTO' : '✗ INCORRECTO';
    this.add.text(32, fbY + 8, icon, {
      fontFamily: '"Press Start 2P"',
      fontSize: '8px',
      color: esCorrecta ? '#27AE60' : '#C0392B'
    });

    if (feedback) {
      this.add.text(32, fbY + 26, feedback, {
        fontFamily: '"Roboto Mono", monospace',
        fontSize: '11px',
        color: '#cccccc',
        wordWrap: { width: W - 64 }
      });
    }

    if (normativaRef) {
      this.add.text(W - 32, fbY + 50, normativaRef, {
        fontFamily: '"Roboto Mono", monospace',
        fontSize: '9px',
        color: '#7A9EC0',
        align: 'right'
      }).setOrigin(1, 1);
    }

    // Botón Continuar
    const btnCont = this.add.text(W / 2, H - 30, '[ CONTINUAR ]', {
      fontFamily: '"Press Start 2P"',
      fontSize: '9px',
      color: '#3A5A8A',
      backgroundColor: '#D6E4F0',
      padding: { x: 12, y: 6 }
    }).setOrigin(0.5, 1).setInteractive({ useHandCursor: true });

    btnCont.on('pointerover', () => btnCont.setBackgroundColor('#7A9EC0'));
    btnCont.on('pointerout',  () => btnCont.setBackgroundColor('#D6E4F0'));
    btnCont.on('pointerdown', () => {
      this.scene.stop('DialogScene');
      this.scene.resume('GameScene');
      if (this.onRespuestaCallback) {
        this.onRespuestaCallback(resultado);
      }
    });
  }
}
