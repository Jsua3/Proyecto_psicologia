# Spec: Rediseño de UX del Simulador de Juego

**Fecha:** 2026-05-27  
**Estado:** Aprobado por el usuario  
**Proyecto:** PsychoSim — Facultad de Psicología, Universidad Humboldt  

---

## Contexto y problema

La versión actual del simulador (`SimulationPlayComponent`) divide la experiencia en dos columnas: el canvas de Phaser a la izquierda y un panel lateral con listas de interacciones, herramientas e inventario a la derecha. Los diálogos y el feedback de herramientas aparecen como componentes Angular fuera del canvas. El resultado se siente como un dashboard con un juego incrustado — no como un juego serio.

El motor DAG del backend, los endpoints REST, el cifrado AES-GCM de bitácoras, la auditoría y el sistema de validación están correctamente implementados y **no cambian** con este rediseño.

---

## Decisiones de diseño aprobadas

| Decisión | Elección |
|----------|----------|
| Layout del juego | B+C hybrid — canvas dominante con overlays HTML |
| Modo de pantalla | Full-screen: oculta nav del portal al jugar |
| Diálogos | Undertale-style — tira inferior con portrait, typewriter, opciones |
| Herramientas | HUD de íconos (esquina inferior izquierda), no lista lateral |
| Navegación entre escenas | Transición por puerta con fade-out/fade-in |
| Gráficos del canvas | Tilemap Kenney.nl CC0 + sprites pixel art |
| Assets visuales | Kenney.nl (todo CC0, sin atribución requerida) |
| Bitácora | Sheet deslizable desde la derecha (overlay sobre el juego) |
| Feedback de herramienta | Mismo strip Undertale — supervisor de voz, no toast flotante |
| Accesibilidad | Mantener toda la capa ARIA/HTML existente (Fase 7 intacta) |

---

## Arquitectura de la solución

### 1. Modo juego en el shell del portal

El componente `ShellComponent` (`app/shared/layout/shell.component.ts`) recibirá una clase CSS `game-mode` en el `<body>` cuando la ruta activa sea `/portal/simulador/:caseVersionId`. En ese estado:

- La barra lateral de navegación se oculta (`display: none`)  
- El header se oculta  
- El contenedor principal ocupa `100vw × 100vh`  

Al salir de la ruta del simulador, el shell vuelve al estado normal. Se implementa con `Router.events` en el shell component y un signal `inGameMode`.

### 2. SimulationPlayComponent — restructuración

`SimulationPlayComponent` pasa de ser un layout de dos columnas a ser un contenedor `position: fixed; inset: 0` con:

- `GameWorldComponent` (Phaser) como fondo que llena todo  
- Todos los overlays HTML posicionados absolutamente encima con `z-index` definidos  

**Z-index stack (de atrás hacia adelante):**

```
10  — canvas Phaser (fondo)
50  — HUD superior (puntaje, estrés, etapas, salida segura)
50  — Minimap (esquina superior derecha)
50  — Tool HUD (esquina inferior izquierda)
50  — Journal button (esquina inferior derecha)
50  — Controls hint (centro inferior)
60  — Dialogue strip (bottom, Undertale)
90  — Stress vignette overlay (cobertura total, pointer-events:none)
100 — Journal panel (sheet lateral derecha)
200 — Fade overlay (transiciones de escena)
```

Los paneles laterales (`interaction-log`, `scene-briefing`) desaparecen completamente. El fallback accesible de puntos interactivos se mantiene como región `sr-only` (Fase 7).

### 3. DialoguePanelComponent — refactor a Undertale strip

El componente se rediseña como una tira de ancho completo pegada al fondo de la pantalla. Estructura:

```
[Portrait 60px] [Speaker name (uppercase, teal) + texto typewriter + botones de decisión]
```

- El portrait muestra el emoji/sprite del hablante + chip de emoción  
- El texto usa typewriter (ya implementado en `DialogueDirector`) a 22 chars/seg  
- Los botones de decisión son chips horizontales; la opción recomendada es azul, la prohibida es roja  
- Salir con `Esc` cierra el diálogo (ya implementado)  
- El feedback de supervisión de herramientas usa el mismo componente con `speakerName='Supervisión clínica'` y borde verde  

### 4. ToolInventoryComponent — refactor a HUD

El componente pasa de ser una lista a ser una fila horizontal de botones cuadrados (44×44px) posicionada en la esquina inferior izquierda. Cada botón muestra icono + código corto. Estado visual:

- **Normal:** borde teal semitransparente  
- **Activa/seleccionada:** borde y glow verde  
- **Bloqueada/no disponible:** opacidad 0.3  

Clic en un botón abre el diálogo de uso de herramienta en el strip Undertale.

### 5. JournalPanelComponent — refactor a sheet deslizable

Pasa de ser un panel fijo en el layout a un sheet que entra desde la derecha (`transform: translateX(100%)` → `translateX(0)`). El juego queda visible detrás con opacidad reducida. El sheet tiene:

- Header con icono, título y botón de cierre  
- Contexto de la escena actual (readonly)  
- Textarea con `minHeight: 150px`  
- Nota de cifrado AES-GCM  
- Botón guardar  

Se activa con el botón de bitácora (overlay bottom-right) o con tecla `J`.

### 6. Transiciones de escena (door system)

Las puertas son `MapObject` con `type='EXIT'` y `decisionOptionId` vinculado en la base de datos. El flujo:

1. El jugador se acerca a la puerta → proximidad detectada → hint `E — [nombre sala] →` aparece sobre la puerta  
2. El jugador pulsa `E` → `openInteraction(door)` → backend devuelve el diálogo de confirmación de la puerta  
3. Usuario confirma → `executeDecision(door.decisionOptionId)`  
4. Frontend: fade-out (300ms) → `getWorld(newAttempt)` → Phaser `setWorld()` carga nuevo mapa → fade-in (300ms)  
5. El minimap se actualiza con el nodo actual resaltado  

No se requieren cambios en el backend. La puerta es un objeto interactivo como cualquier otro.

### 7. Minimap HTML overlay

Componente Angular standalone nuevo: `MinimapComponent`. Recibe como inputs:

- `nodes: SimulationNodeSummary[]` — lista de nodos del caso con sus keys  
- `currentNodeKey: string` — nodo activo  

Renderiza un `<svg>` o `<div>` esquemático con:

- Un rectángulo por nodo (posiciones calculadas por índice BFS del DAG)  
- El nodo actual resaltado en teal  
- Un dot animado que representa al jugador (posición aproximada, no exacta)  
- Leyenda: `[nombre sala] · nodo N/6`  

El minimap no consume nuevos endpoints — usa los datos de `SimulationAttemptState` ya disponibles.

### 8. Phaser — tilemap con Kenney.nl

`DataDrivenWorldScene` se actualiza para cargar assets desde `admin-panel/src/assets/game/kenney/`:

**Assets a descargar (todos CC0 de kenney.nl):**

| Pack | Uso |
|------|-----|
| `Tiny Town` | Tileset de interiores: suelo de madera/loseta, paredes, puertas |
| `Tiny Dungeon` | Props clínicos: escritorios, sillas, archiveros, plantas |
| `Tiny RPG Characters` | Sprites de personajes: estudiante en práctica, consultante, supervisor |

**Cambios en `DataDrivenWorldScene`:**

- `preload()`: carga spritesheet y tileset JSON  
- `create()`: construye `Phaser.Tilemaps.Tilemap` desde los datos del `WorldDefinition` (usando `map.width`, `map.height` y el array de colisiones como zonas de tile sólido)  
- Los personajes (player, NPCs) pasan de ser containers geométricos a `Phaser.GameObjects.Sprite` con animaciones de caminar (4 frames por dirección: down/up/left/right)  
- Las puertas se renderizan como tiles de puerta del tileset con un sprite de indicador parpadeante encima  
- Los objetos interactivos usan el tile/sprite correspondiente del pack Tiny Dungeon + glow pulsante (ya existente)  

La lógica de colisiones, proximidad, interacción y eventos permanece igual — solo cambia la capa visual.

### 9. GameFeelSystem (Fase 3, completar)

El `GameFeelSystem` ya tiene el esqueleto. En esta iteración se activan:

- Sub-pixel movement y aceleración/fricción (ya codificado)  
- Animaciones de walking (flip horizontal para izquierda/derecha)  
- Screen-shake leve en penalización de conducta prohibida  
- Reducción de animaciones si `prefers-reduced-motion` (ya implementado)  

El `AudioDirector` y las capas de audio quedan como pendiente de Fase 3 completa (requieren assets de audio).

---

## Componentes afectados

| Componente | Cambio |
|---|---|
| `ShellComponent` | + signal `inGameMode`, + clase CSS `game-mode` en body |
| `SimulationPlayComponent` | Restructuración completa de template/styles. Lógica TS intacta. |
| `DialoguePanelComponent` | Refactor visual a Undertale strip. API de inputs/outputs intacta. |
| `ToolInventoryComponent` | Refactor a HUD de íconos. API intacta. |
| `JournalPanelComponent` | Refactor a sheet deslizable. Lógica de guardado intacta. |
| `SimulationHudComponent` | Refactor a franja superior compacta. API intacta. |
| `GameWorldComponent` | + soporte tilemap Kenney. Outputs/inputs intactos. |
| `DataDrivenWorldScene` | + `preload()`, + tilemap, + sprites con animaciones, + door tiles |
| `MinimapComponent` | Nuevo componente standalone |
| `styles.scss` | + clase `.game-mode` para ocultar nav/header |

## Componentes que NO cambian

- Todos los servicios Angular (`SimulationService`, etc.)  
- Todos los endpoints REST del backend  
- Motor DAG, `SimulationGameService`, `SimulationWorldService`  
- Cifrado AES-GCM de bitácoras  
- Sistema de auditoría AOP  
- ARIA live regions y rutas narrativas accesibles (Fase 7)  
- Lógica de proximidad en Phaser (solo cambia el renderer)  
- Tests unitarios e IT de backend  

---

## Assets a descargar antes de implementar

```
https://kenney.nl/assets/tiny-town          → tileset interior clínico
https://kenney.nl/assets/tiny-dungeon        → props de consultorio
https://kenney.nl/assets/tiny-rpg-characters → sprites de personajes
```

Destino: `admin-panel/src/assets/game/kenney/`  
Licencia: CC0 (dominio público, sin atribución requerida)

---

## Criterios de aceptación

1. Al entrar a `/portal/simulador/:id`, la nav lateral y el header del portal desaparecen completamente.  
2. El canvas de Phaser ocupa toda la pantalla disponible.  
3. Los diálogos de NPC aparecen como tira inferior con portrait, typewriter y botones de decisión — sin paneles laterales.  
4. Las herramientas aparecen como HUD de íconos en la esquina inferior izquierda.  
5. La bitácora se abre como sheet deslizable desde la derecha con tecla `J` o botón.  
6. Los objetos del mundo muestran sprites Kenney, no círculos de color.  
7. Los personajes (player y NPCs) tienen sprites Kenney con animaciones de caminar.  
8. Las puertas son tiles de puerta con indicador `E` cuando el jugador está cerca.  
9. Cruzar una puerta produce fade-out → carga del siguiente mapa → fade-in.  
10. El minimap muestra los nodos del caso y resalta el nodo actual.  
11. El feedback de uso de herramienta aparece en el mismo strip Undertale (sin toasts flotantes separados).  
12. Al salir del simulador, la nav del portal vuelve a aparecer.  
13. `npm run build` pasa sin errores.  
14. La ruta accesible (sr-only, Fase 7) sigue funcionando para lectores de pantalla.  

---

## Pendientes fuera de scope (para fases posteriores)

- Assets de audio y activación del `AudioDirector` con Howler.js  
- Drag-and-drop en el editor DAG  
- Diseño pixel art personalizado (más allá de Kenney.nl base)  
- Efectos de partículas avanzados en `GameFeelSystem`  
- Playwright E2E del nuevo flujo de juego  
