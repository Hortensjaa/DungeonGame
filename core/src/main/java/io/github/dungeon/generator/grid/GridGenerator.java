package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.dungeon_game.danger.DangerType;
import io.github.dungeon.dungeon_game.reward.RewardType;
import io.github.dungeon.generator.layout.LayoutField;
import io.github.dungeon.generator.layout.LayoutGenerator;
import io.github.dungeon.generator.room.Room;
import io.github.dungeon.generator.room.RoomContents;
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
    private final Map<Coord, Room> rooms = new HashMap<>(); // <center: room object>

    private final int trimmedH; // Height of the trimmed layout
    private final int trimmedW; // Width of the trimmed layout
    private final int partitionWidth; // Width of each partition in the grid
    private final int partitionHeight; // Height of each partition in the grid

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

    private void placeRegularRoom(Coord leftUpperCorner) {
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
                placeRegularRoom(leftUpperCorner);

                int centerX = (int) leftUpperCorner.getX() + partitionWidth / 2;
                int centerY = (int) leftUpperCorner.getY() + partitionHeight / 2;
                Coord center = new Coord(centerX, centerY);
                // Track special rooms
                if (field.getType() instanceof NodeTypes.Start) {
                    playerStart = center;
                } else if (field.getType() instanceof NodeTypes.Exit) {
                    exitPoint = center;
                }

                rooms.put(center, new Room((int) leftUpperCorner.getX(), (int) leftUpperCorner.getY(),
                    partitionWidth, partitionHeight, field.getType().getRisk(), field.getType().getReward()));
            }
        }
    }

    /** find room that have those coordinates inside */
    private Room findRoom(int x, int y) {
        int layoutX = x / partitionWidth;
        int layoutY = y / partitionHeight;

        int centerX = layoutX * partitionWidth + partitionWidth / 2;
        int centerY = layoutY * partitionHeight + partitionHeight / 2;

        return rooms.get(new Coord(centerX, centerY));
    }

    // ----------------------------------------------- CORRIDORS -----------------------------------------------

    private void placeCorridor(Coord center, Direction direction) {
        int center_x = (int) center.getX();
        int center_y = (int) center.getY();

        int dx = direction.getDx();
        int dy = direction.getDy();

        int x = center_x + dx * (partitionWidth / 2 - Constants.WALL_OFFSET);
        int y = center_y + dy * (partitionHeight / 2 - Constants.WALL_OFFSET);

        grid[y - Math.max(dy, 0)][x - Math.max(dx, 0)] = Constants.ENTRANCE;
        Room room = rooms.get(new Coord(center_x, center_y));
        room.setEntrance(new Coord(x - Math.max(dx, 0), y - Math.max(dy, 0)));

        int parent_x = x + dx * partitionWidth;
        int parent_y = y + dy * partitionHeight;
        int exitX = x + dx * 2 * Constants.WALL_OFFSET + Math.min(dx, 0);
        int exitY = y + dy * 2 * Constants.WALL_OFFSET + Math.min(dy, 0);
        grid[exitY][exitX] = Constants.EXIT;
        Room parentRoom = findRoom(parent_x, parent_y);
        parentRoom.getExits().add(new Coord(exitX, exitY));

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

                if (field.getParentDirection() != null) placeCorridor(center, field.getParentDirection());
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
            RoomContents contents = room.getRoomContents();
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
