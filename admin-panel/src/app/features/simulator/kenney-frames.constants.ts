/**
 * Kenney.nl Tiny Town & Tiny RPG Characters frame indices for Phaser spritesheets.
 *
 * HOW TO FIND FRAME NUMBERS:
 * Open the PNG in a tile inspector (e.g. Tiled map editor) or count manually:
 *   - Tile row 0: frames 0..cols-1
 *   - Tile row 1: frames cols..(2*cols-1)
 * Tiny Town packed sheet is 12 cols wide. Tiny RPG Characters is also 12 cols wide.
 *
 * Adjust the values below after inspecting the downloaded files.
 */
export const KenneyTownFrames = {
  /** Light wooden floor tile — row 1, col 4 of tiny-town packed */
  FLOOR_WOOD: 16,
  /** Stone floor tile — row 1, col 6 */
  FLOOR_STONE: 18,
  /** Horizontal wall segment */
  WALL_H: 0,
  /** Vertical wall segment */
  WALL_V: 12,
  /** Closed door tile */
  DOOR_CLOSED: 44,
  /** Open door tile */
  DOOR_OPEN: 45,
} as const;

export const KenneyDungeonFrames = {
  /** Desk / examination table */
  DESK: 8,
  /** Chair */
  CHAIR: 20,
  /** Filing cabinet */
  CABINET: 32,
  /** Plant */
  PLANT: 44,
} as const;

export const KenneyCharFrames = {
  /**
   * Each character occupies 3 columns × 4 rows (down, left, right, up).
   * Character 0 (intern/student) starts at frame 0.
   * Character 1 (patient/client) starts at frame 12.
   * Character 2 (supervisor) starts at frame 24.
   *
   * Within each character block:
   *   frames +0..+2 = walk down
   *   frames +3..+5 = walk left
   *   frames +6..+8 = walk right
   *   frames +9..+11 = walk up
   */
  PLAYER_WALK_DOWN:  [0, 1, 2],
  PLAYER_WALK_LEFT:  [3, 4, 5],
  PLAYER_WALK_RIGHT: [6, 7, 8],
  PLAYER_WALK_UP:    [9, 10, 11],
  PLAYER_IDLE:       0,

  NPC_PATIENT_IDLE:     12,
  NPC_SUPERVISOR_IDLE:  24,
} as const;
