package io.github.dungeon.generator.room;

import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.danger.DangerType;

import java.util.*;

class RoomInfluenceMap {

    public static float[][] compute(Room room, RoomContents contents) {
        Coord end = room.getEnd();
        int left = room.getLeft();
        int top = room.getTop();
        int width = (int) end.getX() - left;
        int height = (int) end.getY() - top;

        float[][] influence = buildInfluenceTable(contents, left, top, width, height);
        return applyDijkstra(influence, room.getEntrance(), left, top, width, height);
    }

    private static float[][] buildInfluenceTable(RoomContents contents, int left, int top, int width, int height) {
        float[][] influence = new float[width][height];

        for (Map.Entry<Coord, DangerType> entry : contents.getEnemies().entrySet()) {
            Coord pos = entry.getKey();
            DangerType type = entry.getValue();
            int ex = (int) pos.getX() - left;
            int ey = (int) pos.getY() - top;

            switch (type) {
                case FIRE -> {
                    if (inBounds(ex, ey, width, height))
                        influence[ex][ey] = 1.0f;
                }
                case LIZARD_HORIZONTAL -> {
                    for (int x = 0; x < width; x++)
                        if (inBounds(x, ey, width, height))
                            influence[x][ey] = Math.min(1.0f, influence[x][ey] + 1.0f / width);
                }
                case LIZARD_VERTICAL -> {
                    for (int y = 0; y < height; y++)
                        if (inBounds(ex, y, width, height))
                            influence[ex][y] = Math.min(1.0f, influence[ex][y] + 1.0f / height);
                }
            }
        }

        return influence;
    }

    private static float[][] applyDijkstra(float[][] influence, Coord entrance, int left, int top, int width, int height) {
        float[][] dist = new float[width][height];
        for (float[] row : dist) Arrays.fill(row, Float.MAX_VALUE);

        if (entrance == null) return dist;

        int startX = (int) entrance.getX() - left;
        int startY = (int) entrance.getY() - top;
        dist[startX][startY] = 0f;

        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[2]));
        pq.offer(new int[]{startX, startY, 0});

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int cx = cur[0], cy = cur[1];
            float curDist = Float.intBitsToFloat(cur[2]);

            if (curDist > dist[cx][cy]) continue;

            for (int d = 0; d < 4; d++) {
                int nx = cx + dx[d];
                int ny = cy + dy[d];
                if (!inBounds(nx, ny, width, height)) continue;

                float newDist = Math.min(1.0f, dist[cx][cy] + influence[nx][ny]);
                if (newDist < dist[nx][ny]) {
                    dist[nx][ny] = newDist;
                    pq.offer(new int[]{nx, ny, Float.floatToIntBits(newDist)});
                }
            }
        }
        return dist;
    }

    private static boolean inBounds(int x, int y, int width, int height) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    public static void printInfluenceMap(float[][] dist) {
        for (int y = 0; y < dist[0].length; y++) {
            for (int x = 0; x < dist.length; x++) {
                if (dist[x][y] == Float.MAX_VALUE) {
                    System.out.print(" ### ");
                } else {
                    System.out.printf("%.2f ", dist[x][y]);
                }
            }
            System.out.println();
        }
    }
}

public class RoomPopulator {
    static final int difficultyMultiplier = 5;
    static final int rewardMultiplier = 5;

    static private int influenceX(int x, int left) {
        return (int) x - left;
    }

    static private int influenceY(int y, int top) {
        return (int) y - top;
    }

    private static RoomContents initializeRandomly(Room room) {
        RoomContents contents = new RoomContents();
        Random random = new Random();

        Coord end = room.getEnd();
        int left = room.getLeft();
        int top = room.getTop();
        int right = (int) end.getX();
        int bottom = (int) end.getY();

        // Number of enemies/rewards scaled by difficulty and reward
        int enemyCount = Math.round(room.getDifficulty() * difficultyMultiplier);
        int rewardCount = Math.round(room.getReward() * rewardMultiplier);

        for (int i = 0; i < enemyCount; i++) {
            Coord pos = randomInteriorCoord(random, left, top, right, bottom);
            double roll = random.nextDouble();
            if (roll < 0.4) {
                contents.addTrap(pos);
            } else if (roll < 0.7) {
                contents.addLizardX(pos);
            } else {
                contents.addLizardY(pos);
            }
        }

        for (int i = 0; i < rewardCount; i++) {
            Coord pos = randomInteriorCoord(random, left, top, right, bottom);
            contents.addCoin(pos);
        }

        return contents;
    }

    private static Coord randomInteriorCoord(Random random, int left, int top, int right, int bottom) {
        int x = left + random.nextInt(Math.max(1, right - left));
        int y = top + random.nextInt(Math.max(1, bottom - top));
        return new Coord(x, y);
    }

    public static RoomContents populate(Room room) {
        RoomContents contents = initializeRandomly(room);
        float[][] influence = RoomInfluenceMap.compute(room, contents);

        // if difficulty of rewards and exits is too high/low -> change
        contents.getRewards().forEach((coord, type) -> {
            int lx = influenceX((int) coord.getX(), room.getLeft());
            int ly = influenceY((int) coord.getY(), room.getTop());
            System.out.println(type + " at (" + coord + "): " + influence[lx][ly]);
        });
        room.getExits().forEach(ex -> {
            int lx = influenceX((int) ex.getX(), room.getLeft());
            int ly = influenceY((int) ex.getY(), room.getTop());
            System.out.println("Exit at (" + ex + "): " + influence[lx][ly]);
        });

        return contents;
    }

    // -------------- test --------------
    public static void main(String[] args) {
        Room room = new Room(18, 6, 8, 6, 0.6f, 0.4f);
        room.setEntrance(new Coord(room.getLeft() + 4, room.getTop()));
        room.getExits().add(new Coord(room.getLeft() + 3, room.getEnd().getY() - 1));

        RoomContents contents = initializeRandomly(room);

        System.out.println("Enemies:");
        contents.getEnemies().forEach((coord, type) ->
            System.out.println("  " + type + " at " + coord));

        System.out.println("Rewards:");
        contents.getRewards().forEach((coord, type) ->
            System.out.println("  " + type + " at " + coord));

        System.out.println("\nInfluence map:");
        float[][] influence = RoomInfluenceMap.compute(room, contents);
        RoomInfluenceMap.printInfluenceMap(influence);
        System.out.println("-------------------------------------------\n");
        contents.getRewards().forEach((coord, type) -> {
            int lx = influenceX((int) coord.getX(), room.getLeft());
            int ly = influenceY((int) coord.getY(), room.getTop());
            System.out.println(type + " at (" + coord + "): " + influence[lx][ly]);
        });
        room.getExits().forEach(ex -> {
            int lx = influenceX((int) ex.getX(), room.getLeft());
            int ly = influenceY((int) ex.getY(), room.getTop());
            System.out.println("Exit at (" + ex + "): " + influence[lx][ly]);
        });
    }
}
