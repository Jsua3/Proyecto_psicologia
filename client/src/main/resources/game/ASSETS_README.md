# Assets del juego — instrucciones

## Phaser 3 (REQUERIDO)

Descargar `phaser.min.js` desde la versión oficial y copiarlo en esta misma carpeta (`game/`):

```
https://cdn.jsdelivr.net/npm/phaser@3.70.0/dist/phaser.min.js
```

Comando PowerShell:
```powershell
Invoke-WebRequest -Uri "https://cdn.jsdelivr.net/npm/phaser@3.70.0/dist/phaser.min.js" `
  -OutFile "phaser.min.js"
```

## Sprites (placeholders)

Los sprites de la carpeta `assets/sprites/` son opcionales para el prototipo.
El juego usa formas geométricas de colores como placeholders.
Para producción, reemplazarlos por spritesheets 32×48px con 4 direcciones de animación.

## Tilemaps

Los archivos `assets/tilemaps/hospital.json` y `comisaria.json` son estructuras Tiled.
Para producción, diseñarlos con Tiled Editor (https://www.mapeditor.org/).
