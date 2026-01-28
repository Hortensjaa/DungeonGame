package io.github.dungeon.render;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;

import java.util.Map;

/**
 * Generic tileset that selects tiles based on neighboring tiles using bitmask.
 * Can be configured with different tile mappings for different visual styles.
 */
public class AutoTileset implements Disposable {

    private final Texture tilesheetTexture;
    private final TextureRegion[][] tiles;
    private final int tileSize;
    private final int targetTileType;
    private final TileMapping tileMapping;

    // Bit flags for each direction
    private static final int NORTH = 1;
    private static final int EAST = 2;
    private static final int SOUTH = 4;
    private static final int WEST = 8;

    /**
     * Represents a tile coordinate in the spritesheet.
     */
    public record TileCoord(int row, int col) {}

    /**
     * Maps bitmask values to tile coordinates.
     */
    public static class TileMapping {
        private final Map<Integer, TileCoord> mapping;
        private final TileCoord defaultTile;

        public TileMapping(Map<Integer, TileCoord> mapping, TileCoord defaultTile) {
            this.mapping = mapping;
            this.defaultTile = defaultTile;
        }

        public TileCoord get(int bitmask) {
            return mapping.getOrDefault(bitmask, defaultTile);
        }
    }

    public AutoTileset(String texturePath, int tileSize, int targetTileType, TileMapping tileMapping) {
        this.tilesheetTexture = new Texture(texturePath);
        this.tileSize = tileSize;
        this.targetTileType = targetTileType;
        this.tileMapping = tileMapping;

        // Split the texture into individual tiles
        int cols = tilesheetTexture.getWidth() / tileSize;
        int rows = tilesheetTexture.getHeight() / tileSize;

        TextureRegion[][] tmp = TextureRegion.split(tilesheetTexture, tileSize, tileSize);

        // Flip Y-axis
        tiles = new TextureRegion[rows][cols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                tiles[y][x] = tmp[rows - 1 - y][x];
            }
        }
    }

    public TextureRegion getTile(int[][] grid, int x, int y) {
        int bitmask = calculateBitmask(grid, x, y);
        TileCoord coord = tileMapping.get(bitmask);
        return tiles[coord.row][coord.col];
    }

    private int calculateBitmask(int[][] grid, int x, int y) {
        int bitmask = 0;

        // If neighbor is different type (or out of bounds), it's a wall
        if (!matches(grid, x, y - 1)) bitmask |= SOUTH;
        if (!matches(grid, x + 1, y)) bitmask |= EAST;
        if (!matches(grid, x, y + 1)) bitmask |= NORTH;
        if (!matches(grid, x - 1, y)) bitmask |= WEST;

        return bitmask;
    }

    private boolean matches(int[][] grid, int x, int y) {
        if (y < 0 || y >= grid.length || x < 0 || x >= grid[0].length) {
            return false; // Out of bounds = wall
        }
        return grid[y][x] == targetTileType;
    }

    @Override
    public void dispose() {
        tilesheetTexture.dispose();
    }

    // -------------------- Predefined tile mappings for common layouts --------------------

    /**
     * Creates a tile mapping for the room.
     */
    public static TileMapping createRoomTileMapping() {
        Map<Integer, TileCoord> mapping = Map.ofEntries(
            Map.entry(0, new TileCoord(6, 5)),                    // No walls (center)

            // Single walls
            Map.entry(NORTH, new TileCoord(8, 3)),                // Wall on top
            Map.entry(EAST, new TileCoord(6, 5)),                 // Wall on right
            Map.entry(SOUTH, new TileCoord(4, 3)),                // Wall on bottom
            Map.entry(WEST, new TileCoord(6, 1)),                 // Wall on left

            // Two adjacent walls (inner corners)
            Map.entry(NORTH | EAST, new TileCoord(7, 5)),         // Top-right corner
            Map.entry(SOUTH | EAST, new TileCoord(4, 4)),         // Bottom-right corner
            Map.entry(SOUTH | WEST, new TileCoord(4, 2)),         // Bottom-left corner
            Map.entry(NORTH | WEST, new TileCoord(7, 1)),         // Top-left corner

            // Opposite walls (corridors)
            Map.entry(NORTH | SOUTH, new TileCoord(6, 5)),        // Vertical corridor
            Map.entry(EAST | WEST, new TileCoord(4, 3)),          // Horizontal corridor

            // Three walls (dead ends)
            Map.entry(NORTH | EAST | WEST, new TileCoord(8, 3)),  // Dead end pointing down
            Map.entry(NORTH | SOUTH | EAST, new TileCoord(6, 5)), // Dead end pointing left
            Map.entry(SOUTH | EAST | WEST, new TileCoord(4, 3)),  // Dead end pointing up
            Map.entry(NORTH | SOUTH | WEST, new TileCoord(6, 1)), // Dead end pointing right

            // All walls (isolated)
            Map.entry(NORTH | EAST | SOUTH | WEST, new TileCoord(2, 2))
        );

        return new TileMapping(mapping, new TileCoord(0, 0)); // Default to lava
    }

    public static TileMapping createLavaTileMapping() {
        Map<Integer, TileCoord> mapping = Map.ofEntries(
            Map.entry(0, new TileCoord(0, 0)),                  // No walls (center)

            // Single walls
            Map.entry(NORTH, new TileCoord(0, 3)),                // Wall on top
            Map.entry(EAST, new TileCoord(2, 0)),                 // Wall on right
            Map.entry(SOUTH, new TileCoord(0, 0)),                // Wall on bottom
            Map.entry(WEST, new TileCoord(2, 6)),                 // Wall on left

            // Two adjacent walls (inner corners)
            Map.entry(NORTH | EAST, new TileCoord(1, 1)),         // Top-right corner
            Map.entry(SOUTH | EAST, new TileCoord(2, 0)),         // Bottom-right corner
            Map.entry(SOUTH | WEST, new TileCoord(2, 6)),         // Bottom-left corner
            Map.entry(NORTH | WEST, new TileCoord(1, 5)),         // Top-left corner

            // Opposite walls (corridors)
            Map.entry(NORTH | SOUTH, new TileCoord(0, 0)),        // Vertical corridor
            Map.entry(EAST | WEST, new TileCoord(0, 0)),          // Horizontal corridor

            // Three walls (dead ends)
            Map.entry(NORTH | EAST | WEST, new TileCoord(0, 0)),  // Dead end pointing down
            Map.entry(NORTH | SOUTH | EAST, new TileCoord(0, 0)), // Dead end pointing left
            Map.entry(SOUTH | EAST | WEST, new TileCoord(0, 0)),  // Dead end pointing up
            Map.entry(NORTH | SOUTH | WEST, new TileCoord(0, 0)), // Dead end pointing right

            // All walls (isolated)
            Map.entry(NORTH | EAST | SOUTH | WEST, new TileCoord(2, 3))
        );

        return new TileMapping(mapping, new TileCoord(0, 0)); // Default to lava
    }

    /**
     * Creates a simple tile mapping where all tiles use the same sprite.
     */
    public static TileMapping createSimpleTileMapping(int row, int col) {
        return new TileMapping(Map.of(), new TileCoord(row, col));
    }
}
