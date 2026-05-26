// GameScene.js — escena principal con mapa isométrico y movimiento del jugador

class GameScene extends Phaser.Scene {
  constructor() {
    super({ key: 'GameScene' });
    this.player = null;
    this.npcs = [];
    this.cursors = null;
    this.wasd = null;
    this.playerSpeed = 120;
    this.casoData = null;
    this.escenarioActual = 0;
    this.preguntaActual = 0;
    this.interactionZones = [];
    this.canInteract = false;
    this.npcActivo = null;
    this.indicador = null;
  }

  preload() {
    // Sprites reales generados
    this.load.spritesheet('player',       'assets/sprites/player.png',       { frameWidth: 32, frameHeight: 48 });
    this.load.spritesheet('npc_medico',   'assets/sprites/npc_medico.png',   { frameWidth: 32, frameHeight: 48 });
    this.load.spritesheet('npc_familiar', 'assets/sprites/npc_familiar.png', { frameWidth: 32, frameHeight: 48 });
    this.load.spritesheet('npc_juridico', 'assets/sprites/npc_juridico.png', { frameWidth: 32, frameHeight: 48 });
    this.load.image('iso_tiles', 'assets/tileset/iso_tiles.png');
    this.load.tilemapTiledJSON('hospital',  'assets/tilemaps/hospital.json');
    this.load.tilemapTiledJSON('comisaria', 'assets/tilemaps/comisaria.json');

    // Fallback: si algún asset falla, continuar sin él
    this.load.on('loaderror', (file) => {
      console.warn('[GameScene] Asset no cargado:', file.key, '— usando placeholder gráfico');
    });
  }

  create() {
    const W = this.scale.width;
    const H = this.scale.height;

    // Fondo del mapa
    this.add.rectangle(0, 0, W, H, 0x1a2b3c).setOrigin(0, 0);
    this.drawIsometricGrid(W, H);

    // Título del escenario
    this.scenarioText = this.add.text(W / 2, 20, 'Sala de Urgencias', {
      fontFamily: '"Press Start 2P"',
      fontSize: '10px',
      color: '#7A9EC0',
      align: 'center'
    }).setOrigin(0.5, 0).setDepth(10);

    // Animaciones del jugador (4 direcciones: arriba=0, abajo=1, izq=2, der=3)
    ['player','npc_medico','npc_familiar','npc_juridico'].forEach(key => {
      if (this.textures.exists(key)) {
        ['up','down','left','right'].forEach((dir, i) => {
          const anim = `${key}_${dir}`;
          if (!this.anims.exists(anim)) {
            this.anims.create({ key: anim, frames: [{ key, frame: i }], frameRate: 8, repeat: -1 });
          }
        });
      }
    });

    // Jugador con sprite o placeholder
    if (this.textures.exists('player')) {
      this.player = this.add.sprite(W / 2, H / 2, 'player', 1).setDepth(5).setScale(1.5);
    } else {
      this.player = this.add.rectangle(W / 2, H / 2, 24, 36, 0x3A5A8A).setDepth(5);
    }
    this.playerBody = { x: W / 2, y: H / 2, speed: this.playerSpeed };

    // NPCs de ejemplo para el caso semilla
    const npcDefs = [
      { x: W * 0.25, y: H * 0.4,  color: 0x27AE60, nombre: 'Dra. Martínez',      icon: '+', sprite: 'npc_medico'   },
      { x: W * 0.7,  y: H * 0.35, color: 0xE67E22, nombre: 'Enfermera López',     icon: '♥', sprite: 'npc_familiar' },
      { x: W * 0.5,  y: H * 0.65, color: 0x7A9EC0, nombre: 'Trabajadora Social',  icon: '★', sprite: 'npc_juridico' }
    ];
    npcDefs.forEach(def => this.crearNPC(def));

    // Input
    this.cursors = this.input.keyboard.createCursorKeys();
    this.wasd = this.input.keyboard.addKeys({
      up: Phaser.Input.Keyboard.KeyCodes.W,
      down: Phaser.Input.Keyboard.KeyCodes.S,
      left: Phaser.Input.Keyboard.KeyCodes.A,
      right: Phaser.Input.Keyboard.KeyCodes.D
    });
    this.input.keyboard.on('keydown-ENTER', () => this.tryInteract());
    this.input.keyboard.on('keydown-SPACE', () => this.tryInteract());

    // HUD
    this.hud = this.add.text(16, H - 36, 'WASD / Flechas — moverse   |   ENTER — interactuar', {
      fontFamily: '"Press Start 2P"',
      fontSize: '7px',
      color: 'rgba(255,255,255,0.5)'
    }).setDepth(10);

    this.puntajeText = this.add.text(W - 16, 20, 'Puntos: 0', {
      fontFamily: '"Press Start 2P"',
      fontSize: '9px',
      color: '#7A9EC0'
    }).setOrigin(1, 0).setDepth(10);

    this.puntaje = 0;

    // Cargar datos del caso desde el backend
    this.cargarCasoData();
  }

  drawIsometricGrid(W, H) {
    const gfx = this.add.graphics();
    gfx.lineStyle(1, 0x3A5A8A, 0.25);
    const tileW = 80, tileH = 40;
    const cols = Math.ceil(W / tileW) + 2;
    const rows = Math.ceil(H / tileH) + 2;
    const offsetX = W / 2;
    const offsetY = 80;
    for (let row = 0; row < rows; row++) {
      for (let col = 0; col < cols; col++) {
        const px = offsetX + (col - row) * (tileW / 2);
        const py = offsetY + (col + row) * (tileH / 2);
        gfx.strokePoints([
          { x: px, y: py },
          { x: px + tileW / 2, y: py + tileH / 2 },
          { x: px, y: py + tileH },
          { x: px - tileW / 2, y: py + tileH / 2 },
          { x: px, y: py }
        ]);
      }
    }
  }

  crearNPC(def) {
    const npcBody = this.textures.exists(def.sprite)
      ? this.add.sprite(def.x, def.y, def.sprite, 1).setDepth(4).setScale(1.4)
      : this.add.rectangle(def.x, def.y, 28, 40, def.color).setDepth(4);
    const npcLabel = this.add.text(def.x, def.y - 30, def.icon, {
      fontFamily: 'Arial', fontSize: '16px', color: '#fff'
    }).setOrigin(0.5).setDepth(4);
    const nameTag = this.add.text(def.x, def.y + 30, def.nombre, {
      fontFamily: '"Press Start 2P"', fontSize: '6px', color: '#fff',
      backgroundColor: 'rgba(0,0,0,0.6)', padding: { x: 4, y: 2 }
    }).setOrigin(0.5, 0).setDepth(4);

    const zone = { x: def.x, y: def.y, radius: 80, nombre: def.nombre };
    this.interactionZones.push(zone);

    // Indicador "!" (oculto por defecto)
    const indicator = this.add.text(def.x, def.y - 55, '!', {
      fontFamily: '"Press Start 2P"', fontSize: '14px', color: '#FFD700'
    }).setOrigin(0.5).setDepth(6).setVisible(false);
    zone.indicator = indicator;

    // Animación idle del NPC (blink)
    this.time.addEvent({
      delay: 3000 + Math.random() * 1000,
      loop: true,
      callback: () => {
        this.tweens.add({
          targets: npcBody,
          scaleY: 0.05,
          duration: 80,
          yoyo: true,
          ease: 'Linear'
        });
      }
    });

    npcBody.setInteractive({ useHandCursor: true });
    npcBody.on('pointerdown', () => this.iniciarDialogo(zone));

    this.npcs.push({ body: npcBody, zone, label: npcLabel, nameTag });
  }

  cargarCasoData() {
    const casoId = PsychoSimBridge.getCasoId();
    if (casoId <= 0) return;

    PsychoSimBridge._callbacks.onCasoData = (res) => {
      if (res.success) {
        this.casoData = res.data;
        if (this.casoData.escenarios && this.casoData.escenarios.length > 0) {
          const esc = this.casoData.escenarios[this.escenarioActual];
          if (this.scenarioText) this.scenarioText.setText(esc.nombre);
        }
      }
    };
    if (window.javaBridge) {
      window.javaBridge.getCasoData(casoId);
    }
  }

  tryInteract() {
    if (!this.canInteract || !this.npcActivo) return;
    this.iniciarDialogo(this.npcActivo);
  }

  iniciarDialogo(zone) {
    if (!this.casoData) {
      this.scene.launch('DialogScene', {
        pregunta: {
          id: 1,
          enunciado: 'Cargando datos del servidor...',
          opciones: []
        },
        npcNombre: zone.nombre,
        onRespuesta: () => {}
      });
      return;
    }

    const escenarios = this.casoData.escenarios;
    if (!escenarios || escenarios.length === 0) return;
    const esc = escenarios[this.escenarioActual];
    if (!esc || !esc.preguntas || esc.preguntas.length === 0) return;
    if (this.preguntaActual >= esc.preguntas.length) return;

    const pregunta = esc.preguntas[this.preguntaActual];
    this.scene.pause('GameScene');
    this.scene.launch('DialogScene', {
      pregunta,
      npcNombre: zone.nombre,
      onRespuesta: (resultado) => {
        this.puntaje += resultado.puntosObtenidos || 0;
        this.puntajeText.setText('Puntos: ' + this.puntaje);
        this.preguntaActual++;

        // Verificar si completó el escenario
        if (this.preguntaActual >= esc.preguntas.length) {
          this.escenarioActual++;
          this.preguntaActual = 0;

          if (this.escenarioActual >= escenarios.length) {
            // Caso completado
            const sesionId = PsychoSimBridge.getSesionId();
            PsychoSimBridge._callbacks.onFinalizar = (res) => {
              this.scene.start('ResultsScene', {
                puntajeTotal: res.data.puntajeTotal || this.puntaje,
                casoTitulo: this.casoData.titulo
              });
            };
            PsychoSimBridge.finalizarSesion(sesionId);
          } else {
            this.mostrarTransicion(escenarios[this.escenarioActual].nombre);
          }
        }
      }
    });
  }

  mostrarTransicion(nombreEscenario) {
    const overlay = this.add.rectangle(
      this.scale.width / 2, this.scale.height / 2,
      this.scale.width, this.scale.height, 0x000000, 0
    ).setDepth(20);

    this.tweens.add({
      targets: overlay,
      alpha: 1,
      duration: 500,
      onComplete: () => {
        const txt = this.add.text(
          this.scale.width / 2, this.scale.height / 2,
          'ESCENARIO COMPLETADO\n\n' + nombreEscenario, {
            fontFamily: '"Press Start 2P"',
            fontSize: '12px',
            color: '#7A9EC0',
            align: 'center',
            lineSpacing: 10
          }
        ).setOrigin(0.5).setDepth(21);

        this.time.delayedCall(2500, () => {
          this.tweens.add({
            targets: [overlay, txt],
            alpha: 0,
            duration: 500,
            onComplete: () => { overlay.destroy(); txt.destroy(); }
          });
          if (this.casoData.escenarios[this.escenarioActual]) {
            this.scenarioText.setText(this.casoData.escenarios[this.escenarioActual].nombre);
          }
        });
      }
    });
  }

  update(time, delta) {
    const dt = delta / 1000;
    const speed = this.playerSpeed;
    let dx = 0, dy = 0;

    if (this.cursors.left.isDown  || this.wasd.left.isDown)  dx = -speed;
    if (this.cursors.right.isDown || this.wasd.right.isDown) dx =  speed;
    if (this.cursors.up.isDown    || this.wasd.up.isDown)    dy = -speed;
    if (this.cursors.down.isDown  || this.wasd.down.isDown)  dy =  speed;

    // Normalizar diagonal
    if (dx !== 0 && dy !== 0) { dx *= 0.707; dy *= 0.707; }

    this.player.x = Phaser.Math.Clamp(this.player.x + dx * dt, 20, this.scale.width - 20);
    this.player.y = Phaser.Math.Clamp(this.player.y + dy * dt, 20, this.scale.height - 20);

    // Animación de movimiento con sprite
    if (this.player.anims) {
      if (dy < 0)       { this.player.anims.play('player_up',    true); }
      else if (dy > 0)  { this.player.anims.play('player_down',  true); }
      else if (dx < 0)  { this.player.anims.play('player_left',  true); }
      else if (dx > 0)  { this.player.anims.play('player_right', true); }
      else              { this.player.anims.stop(); }
    }

    // Detectar zonas de interacción
    this.canInteract = false;
    this.npcActivo = null;
    for (const zone of this.interactionZones) {
      const dist = Phaser.Math.Distance.Between(this.player.x, this.player.y, zone.x, zone.y);
      const inZone = dist < zone.radius;
      zone.indicator.setVisible(inZone);
      if (inZone) {
        this.canInteract = true;
        this.npcActivo = zone;
      }
    }
  }
}
