// ResultsScene.js — pantalla de resultados al completar el caso

class ResultsScene extends Phaser.Scene {
  constructor() {
    super({ key: 'ResultsScene' });
  }

  init(data) {
    this.puntajeTotal = data.puntajeTotal || 0;
    this.casoTitulo   = data.casoTitulo   || 'Caso completado';
  }

  create() {
    const W = this.scale.width;
    const H = this.scale.height;

    // Fondo
    this.add.rectangle(0, 0, W, H, 0x0A0A1E).setOrigin(0, 0);

    // Estrellitas decorativas
    for (let i = 0; i < 60; i++) {
      const star = this.add.circle(
        Phaser.Math.Between(0, W),
        Phaser.Math.Between(0, H),
        Phaser.Math.Between(1, 2),
        0xffffff,
        Phaser.Math.FloatBetween(0.2, 0.8)
      );
      this.tweens.add({
        targets: star,
        alpha: 0.1,
        duration: Phaser.Math.Between(1000, 3000),
        yoyo: true,
        repeat: -1,
        delay: Phaser.Math.Between(0, 2000)
      });
    }

    // Título
    this.add.text(W / 2, H * 0.18, 'CASO COMPLETADO', {
      fontFamily: '"Press Start 2P"',
      fontSize: '16px',
      color: '#7A9EC0',
      align: 'center'
    }).setOrigin(0.5);

    // Nombre del caso
    this.add.text(W / 2, H * 0.28, this.casoTitulo, {
      fontFamily: 'Arial',
      fontSize: '15px',
      color: '#ffffff',
      align: 'center',
      wordWrap: { width: W * 0.7 }
    }).setOrigin(0.5);

    // Puntaje
    const puntajeLabel = this.add.text(W / 2, H * 0.44, '0', {
      fontFamily: '"Press Start 2P"',
      fontSize: '48px',
      color: '#27AE60',
      align: 'center'
    }).setOrigin(0.5);

    this.add.text(W / 2, H * 0.57, 'PUNTOS', {
      fontFamily: '"Press Start 2P"',
      fontSize: '10px',
      color: '#7A9EC0'
    }).setOrigin(0.5);

    // Animación de contador
    const target = this.puntajeTotal;
    let current = 0;
    const duration = 1500;
    const step = target / (duration / 50);
    const timer = this.time.addEvent({
      delay: 50,
      repeat: Math.ceil(target / step),
      callback: () => {
        current = Math.min(current + step, target);
        puntajeLabel.setText(Math.floor(current).toString());
        if (current >= target) timer.remove();
      }
    });

    // Mensaje según desempeño
    let mensaje = '';
    if (target >= 50)      mensaje = '¡Excelente desempeño!';
    else if (target >= 30) mensaje = '¡Buen trabajo! Sigue practicando.';
    else                   mensaje = 'Revisa los protocolos y vuelve a intentarlo.';

    this.add.text(W / 2, H * 0.67, mensaje, {
      fontFamily: '"Press Start 2P"',
      fontSize: '8px',
      color: '#ffffff',
      align: 'center'
    }).setOrigin(0.5);

    // Botón volver al lobby
    const btnLobby = this.add.text(W / 2, H * 0.80, '[ VOLVER AL LOBBY ]', {
      fontFamily: '"Press Start 2P"',
      fontSize: '9px',
      color: '#0A0A1E',
      backgroundColor: '#3A5A8A',
      padding: { x: 16, y: 10 }
    }).setOrigin(0.5).setInteractive({ useHandCursor: true });

    btnLobby.on('pointerover', () => btnLobby.setBackgroundColor('#7A9EC0'));
    btnLobby.on('pointerout',  () => btnLobby.setBackgroundColor('#3A5A8A'));
    btnLobby.on('pointerdown', () => {
      // Notificar a JavaFX para volver al lobby
      if (window.javaBridge && typeof window.javaBridge.volverAlLobby === 'function') {
        window.javaBridge.volverAlLobby();
      }
    });

    // Entrada del mouse
    this.input.on('pointerdown', () => {});
  }
}
