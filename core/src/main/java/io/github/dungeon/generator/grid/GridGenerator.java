package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.dungeon_game.danger.DangerType;
import io.github.dungeon.dungeon_game.reward.RewardType;
import io.github.dungeon.generator.tree.DungeonTree;
import io.github.dungeon.generator.tree.DungeonTreeSerializer;
import io.github.dungeon.generator.tree.NodeTypes;

import java.io.File;
import java.util.*;

/**
 * This class generates a dungeon layout from a given layout or dungeon tree.
 * It handles the placement of rooms, corridors, and special elements like the player start,
 * exit point, and enemies.
 */
public class GridGenerator extends Generator {
    private final LayoutField[][] layout; // The layout of the dungeon as a 2D array of fields
    private Coord playerStart; // The starting position of the player
    private Coord exitPoint; // The exit point of the dungeon
    private final Map<Coord, DangerType> enemies = new HashMap<>(); // Map of enemy positions and their types
    private final Map<Coord, RewardType> rewards = new HashMap<>(); // Map of rewards positions and their types
    private int[][] grid; // The grid representation of the dungeon
    private final Map<Coord, Room> rooms = new HashMap<>(); // center: room object
    private final Set<Coord> entrances = new HashSet<>(); // fixme: debug

    private final int trimmedH; // Height of the trimmed layout
    private final int trimmedW; // Width of the trimmed layout
    private final int partitionWidth; // Width of each partition in the grid
    private final int partitionHeight; // Height of each partition in the grid

    private final boolean irregularRooms; // Flag to determine if rooms should be irregular

    /**
     * Constructor for GeneratorFromLayout.
     *
     * @param layout The layout of the dungeon as a 2D array of LayoutField objects.
     */
    public GridGenerator(LayoutField[][] layout) {
        this.layout = layout;
        trimmedH = layout.length;
        trimmedW = layout[0].length;
        partitionWidth = Constants.COLUMNS / trimmedW;
        partitionHeight = Constants.ROWS / trimmedH;

        irregularRooms = partitionWidth * partitionHeight >= Constants.IRREGULAR_ROOM_THRESHOLD;
    }

    /**
     * Converts layout coordinates to grid coordinates.
     *
     * @param layoutCoord The coordinates in the layout.
     * @return The corresponding coordinates in the grid.
     */
    private Coord toGridCoords(Coord layoutCoord) {
        return new Coord(layoutCoord.getX() * partitionWidth, layoutCoord.getY() * partitionHeight);
    }

    // ----------------------------------------------- ROOMS -----------------------------------------------

    private void placeRegularRoom(Coord leftUpperCorner, float difficulty, float reward) {
        int startX = (int) leftUpperCorner.getX() + Constants.WALL_OFFSET;
        int startY = (int) leftUpperCorner.getY() + Constants.WALL_OFFSET;
        int endX = startX + partitionWidth - 2 * Constants.WALL_OFFSET;
        int endY = startY + partitionHeight - 2 * Constants.WALL_OFFSET;

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                grid[y][x] = Constants.ROOM;
            }
        }
    }

    private void roomFlooding(int x, int y, int startX, int startY, int endX, int endY, float pp) {
        if (x < startX || x >= endX || y < startY || y >= endY) return;
        if (grid[y][x] == Constants.ROOM) return;
        float pp_multiplier = 0.95f;
        if (Math.random() < pp) {
            grid[y][x] = Constants.ROOM;
            roomFlooding(x + 1, y, startX, startY, endX, endY, pp * pp_multiplier);
            roomFlooding(x - 1, y, startX, startY, endX, endY, pp * pp_multiplier);
            roomFlooding(x, y + 1, startX, startY, endX, endY, pp * pp_multiplier);
            roomFlooding(x, y - 1, startX, startY, endX, endY, pp * pp_multiplier);
        }
    }

    private void placeIrregularRoom(Coord leftUpperCorner, float difficulty, float reward) {
        int startX = (int) leftUpperCorner.getX() + Constants.WALL_OFFSET;
        int startY = (int) leftUpperCorner.getY() + Constants.WALL_OFFSET;
        int endX = startX + partitionWidth - 2 * Constants.WALL_OFFSET;
        int endY = startY + partitionHeight - 2 * Constants.WALL_OFFSET;

        int centerX = (startX + endX) / 2;
        int centerY = (startY + endY) / 2;

        roomFlooding(centerX, centerY, startX, startY, endX, endY, 1.0f);
    }

    /**
     * Places all rooms in the grid based on the layout.
     * Tracks special rooms like the player start, exit, and enemy positions.
     */
    private void placeRooms() {
        grid = Generator.initialGridWalls(Constants.ROWS, Constants.COLUMNS);

        for (int y = 0; y < trimmedH; y++) {
            for (int x = 0; x < trimmedW; x++) {
                LayoutField field = layout[y][x];
                if (field == null) continue;

                Coord leftUpperCorner = toGridCoords(new Coord(x, y));
                if (!irregularRooms)
                    placeRegularRoom(leftUpperCorner, field.type.getDifficulty(), field.type.getReward());
                else
                    placeIrregularRoom(leftUpperCorner, field.type.getDifficulty(), field.type.getReward());

                int centerX = (int) leftUpperCorner.getX() + partitionWidth / 2;
                int centerY = (int) leftUpperCorner.getY() + partitionHeight / 2;
                Coord center = new Coord(centerX, centerY);
                // Track special rooms
                if (field.type instanceof NodeTypes.Start) {
                    playerStart = center;
                } else if (field.type instanceof NodeTypes.Exit) {
                    exitPoint = center;
                }

                rooms.put(center, new Room((int) leftUpperCorner.getX(), (int) leftUpperCorner.getY(),
                    partitionWidth, partitionHeight, grid, field.type.getDifficulty(), field.type.getReward()));
            }
        }
    }

    // ----------------------------------------------- CORRIDORS -----------------------------------------------

    private void placeCorridor(Coord center, Direction direction) {
        int x = (int) center.getX();
        int y = (int) center.getY();

        int parent_x = x + direction.getDx() * partitionWidth;
        int parent_y = y + direction.getDy() * partitionHeight;

        while (x != parent_x || y != parent_y) {
            if (grid[y][x] == Constants.WALL) {
                grid[y][x] = Constants.CORRIDOR;
            }

            if (x != parent_x) x += direction.getDx();
            if (y != parent_y) y += direction.getDy();
        }
    }

    /**
     * Places all corridors in the grid based on the layout.
     */
    private void placeCorridors() {
        for (int y = 0; y < trimmedH; y++) {
            for (int x = 0; x < trimmedW; x++) {
                LayoutField field = layout[y][x];
                if (field == null) continue;

                Coord leftUpperCorner = toGridCoords(new Coord(x, y));

                int centerX = (int) leftUpperCorner.getX() + partitionWidth / 2;
                int centerY = (int) leftUpperCorner.getY() + partitionHeight / 2;
                Coord center = new Coord(centerX, centerY);

                if (field.parentDirection != null) placeCorridor(center, field.parentDirection);
            }
        }
    }

    // ----------------- API -----------------

    /**
     * Generates a dungeon grid from a DungeonTree.
     *
     * @param tree The DungeonTree to generate the dungeon from.
     * @return A GridDefinition object representing the generated dungeon.
     */
    public static GridDefinition generate(DungeonTree tree, int maxRetries) {
        LayoutField[][] layout = LayoutGenerator.generateLayout(tree, maxRetries);
        LayoutGenerator.printLayout(layout);

        GridGenerator generator = new GridGenerator(layout);

        generator.placeRooms();
        generator.placeCorridors();
        for (Room room : generator.rooms.values()) {
            RoomContents contents = room.getRoomContents(generator.grid);
            generator.enemies.putAll(contents.getEnemies());
            generator.rewards.putAll(contents.getRewards());
        }

        return GridDefinition.builder()
                .grid(generator.grid)
                .playerStart(generator.playerStart)
                .exit(generator.exitPoint)
                .dangers(generator.enemies)
                .rewards(generator.rewards)
                .build();
    }

    /**
     * Generates a dungeon grid from a file.
     *
     * @param folder The name of the file containing the DungeonTree.
     * @param x      The x coordinate of the layout file.
     * @param y      The y coordinate of the layout file.
     * @return A GridDefinition object representing the generated dungeon.
     * @throws RuntimeException If the file cannot be read or the dungeon cannot be generated.
     */
    public static GridDefinition generate(String folder, int x, int y, int maxRetries) {
        String filename = "levels/" + folder + "/x_" + String.format("%02d", x) + "_y_" + String.format("%02d", y) + ".json";
        File file = new File(filename);
        try {
            DungeonTree tree = DungeonTreeSerializer.readFromFile(file);
            return generate(tree, maxRetries);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate from file: " + filename, e);
        }
    }
}
