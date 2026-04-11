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
                // trap -> field with risk 1.0, and the adjacent gets +0,1
                case FIRE -> {
                    if (inBounds(ex, ey, width, height))
                        influence[ex][ey] = 1.0f;
                    for (int dx = -1; dx <= 1; dx++)
                        for (int dy = -1; dy <= 1; dy++)
                            if (inBounds(ex + dx, ey + dy, width, height))
                                influence[ex + dx][ey + dy] = Math.min(1.0f, influence[ex + dx][ey + dy] + 0.2f);
                }
                // moving enemies -> every field on movement path gets += 1 / path_length
                case LIZARD_HORIZONTAL -> {
                    for (int x = 0; x < width; x++)
                        if (inBounds(x, ey, width, height))
                            influence[x][ey] = Math.min(1.0f, influence[x][ey] + 1.0f / width + 0.2f);
                }
                case LIZARD_VERTICAL -> {
                    for (int y = 0; y < height; y++)
                        if (inBounds(ex, y, width, height))
                            influence[ex][y] = Math.min(1.0f, influence[ex][y] + 1.0f / height + 0.2f);
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
        dist[startX][startY] = influence[startX][startY];

        PriorityQueue<float[]> pq = new PriorityQueue<>(Comparator.comparingDouble(a -> a[2]));
        pq.offer(new float[]{startX, startY, 0});

        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (!pq.isEmpty()) {
            float[] cur = pq.poll();
            float cx = cur[0], cy = cur[1];
            float curDist = cur[2];

            if (curDist > dist[(int) cx][(int) cy]) continue;

            for (int d = 0; d < 4; d++) {
                int nx = (int) cx + dx[d];
                int ny =(int)  cy + dy[d];
                if (!inBounds(nx, ny, width, height)) continue;

                float newDist = Math.min(1.0f, dist[(int) cx][(int) cy] + influence[nx][ny]);
                if (newDist < dist[nx][ny]) {
                    dist[nx][ny] = newDist;
                    pq.offer(new float[]{nx, ny, newDist});
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
            for (float[] floats : dist) {
                if (floats[y] == Float.MAX_VALUE) {
                    System.out.print(" ### ");
                } else {
                    System.out.printf("%.2f ", floats[y]);
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
        return x - left;
    }

    static private int influenceY(int y, int top) {
        return y - top;
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
            contents.addRandomHazard(pos);
        }

        for (int i = 0; i < rewardCount; i++) {
            Coord pos = randomInteriorCoord(random, left, top, right, bottom);
            contents.addRandomReward(pos);
        }

        return contents;
    }

    private static Coord randomInteriorCoord(Random random, int left, int top, int right, int bottom) {
        int x = left + random.nextInt(Math.max(1, right - left));
        int y = top + random.nextInt(Math.max(1, bottom - top));
        return new Coord(x, y);
    }

    private static float score(Room room, RoomContents contents, float[][] influence) {
        List<Float> values = collectKeyValues(room, contents, influence);
        if (values.isEmpty()) return 0f;

        boolean anyMax = values.stream().anyMatch(v -> v >= 1.0f);
        if (anyMax) return Float.MAX_VALUE;

        float avg = average(values);
        float variance = variance(values);
        float avgError = Math.abs(avg - room.getDifficulty());

        return avgError + variance;
    }

    public static RoomContents populate(Room room) {
        RoomContents contents = initializeRandomly(room);

        int maxTries = 25;
        float[][] influence = RoomInfluenceMap.compute(room, contents);
        float currentScore = score(room, contents, influence);

        for (int i = 0; i < maxTries; i++) {
            if (currentScore == 0f) break;

            List<Float> values = collectKeyValues(room, contents, influence);
            if (values.isEmpty()) break;

            float avg = average(values);
            float variance = variance(values);
            boolean anyMax = values.stream().anyMatch(v -> v >= 1.0f);

            RoomContents candidate = contents.deepcopy();
            if (anyMax) {
                removeRandomHazard(candidate);
            } else if (avg < room.getDifficulty() - 0.2f) {
                addRandomHazard(room, candidate);
            } else if (avg > room.getDifficulty() + 0.2f) {
                removeRandomHazard(candidate);
            } else if (variance > 0.1f) {
                nudgeRandomHazard(room, candidate);
            } else {
                break;
            }

            float[][] candidateInfluence = RoomInfluenceMap.compute(room, candidate);
            float candidateScore = score(room, candidate, candidateInfluence);
            if (candidateScore < currentScore) {
                contents = candidate;
                influence = candidateInfluence;
                currentScore = candidateScore;
            }
        }
        return contents;
    }

    private static List<Float> collectKeyValues(Room room, RoomContents contents, float[][] influence) {
        List<Float> values = new ArrayList<>();
        contents.getRewards().forEach((coord, type) -> {
            int lx = influenceX((int) coord.getX(), room.getLeft());
            int ly = influenceY((int) coord.getY(), room.getTop());
            values.add(influence[lx][ly]);
        });
        room.getExits().forEach(ex -> {
            int lx = influenceX((int) ex.getX(), room.getLeft());
            int ly = influenceY((int) ex.getY(), room.getTop());
            values.add(influence[lx][ly]);
        });
        return values;
    }

    private static float average(List<Float> values) {
        return (float) values.stream().mapToDouble(Float::doubleValue).average().orElse(0);
    }

    private static float variance(List<Float> values) {
        float avg = average(values);
        return (float) values.stream().mapToDouble(v -> (v - avg) * (v - avg)).average().orElse(0);
    }

    private static void removeRandomHazard(RoomContents contents) {
        Map<Coord, DangerType> enemies = contents.getEnemies();
        if (enemies.isEmpty()) return;
        Coord toRemove = new ArrayList<>(enemies.keySet()).get((int) (Math.random() * enemies.size()));
        enemies.remove(toRemove);
    }

    private static void addRandomHazard(Room room, RoomContents contents) {
        Random random = new Random();
        Coord end = room.getEnd();
        Coord pos = randomInteriorCoord(random, room.getLeft(), room.getTop(), (int) end.getX(), (int) end.getY());
        contents.addRandomHazard(pos);
    }

    private static void nudgeRandomHazard(Room room, RoomContents contents) {
        Map<Coord, DangerType> enemies = contents.getEnemies();
        if (enemies.isEmpty()) return;

        Random random = new Random();
        List<Coord> keys = new ArrayList<>(enemies.keySet());
        Coord old = keys.get(random.nextInt(keys.size()));
        DangerType type = enemies.remove(old);

        Coord end = room.getEnd();
        int newX = Math.max(room.getLeft(), Math.min((int) old.getX() + random.nextInt(3) - 1, (int) end.getX() - 1));
        int newY = Math.max(room.getTop(), Math.min((int) old.getY() + random.nextInt(3) - 1, (int) end.getY() - 1));
        enemies.put(new Coord(newX, newY), type);
    }

    // -------------- test --------------
    public static void main(String[] args) {
        Room room = new Room(0, 0, 8, 6, 0.6f, 0.4f);
        room.setEntrance(new Coord(room.getLeft() + 4, room.getTop()));
        room.getExits().add(new Coord(room.getLeft() + 3, room.getEnd().getY() - 1));

        RoomContents contents = populate(room);

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
        int lx = influenceX((int) room.getEntrance().getX(), room.getLeft());
        int ly = influenceY((int) room.getEntrance().getY(), room.getTop());
        System.out.println("Entrance at (" + room.getEntrance() + "): " + influence[lx][ly]);
    }
}
