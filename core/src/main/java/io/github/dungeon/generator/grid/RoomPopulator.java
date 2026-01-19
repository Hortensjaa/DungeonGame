package io.github.dungeon.generator.grid;

import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.dungeon_game.danger.DangerType;
import java.util.*;

public class RoomPopulator {

    private static final int MAX_INFLUENCE_ITERATIONS = 50; // Budget control
    private static final float INFLUENCE_DECAY = 0.6f; // How much difficulty spreads

    public static RoomContents populate(Room room) {
        if (room.getEntrances().isEmpty() || room.getTiles().isEmpty()) {
            return new RoomContents();
        }

        Random rng = new Random();

        // 1. Calculate distance field from entrances
        Map<Coord, Integer> distanceField = calculateDistanceField(room);

        // 2. Find safe path network
        Set<Coord> safePathTiles = findSafePathNetwork(room, distanceField);

        // 3. Get available tiles
        List<Coord> availableTiles = new ArrayList<>(room.getTiles());
        availableTiles.removeAll(safePathTiles);
        availableTiles.removeAll(room.getEntrances());

        // 4. Categorize tiles by distance
        Map<String, List<Coord>> zones = categorizeTiles(availableTiles, distanceField, safePathTiles);

        // 5. Place entities
        RoomContents contents = placeEntities(zones, room, rng);

        // 6. Build influence map for difficulty
        Map<Coord, Float> influenceMap = buildInfluenceMap(room, contents);

        // 7. Adjust placement based on influence (optional - balancing pass)
        balanceRewards(contents, influenceMap, room, rng);

        return contents;
    }

    private static Map<Coord, Float> buildInfluenceMap(Room room, RoomContents contents) {
        Map<Coord, Float> influence = new HashMap<>();

        // Initialize all tiles to base difficulty
        for (Coord tile : room.getTiles()) {
            influence.put(tile, room.getDifficulty() * 0.1f);
        }

        // Add enemy influence
        for (Map.Entry<Coord, DangerType> enemy : contents.getEnemies().entrySet()) {
            Coord pos = enemy.getKey();
            DangerType type = enemy.getValue();

            if (type.getMovingDir() != null) {
                // Moving enemy - influence along movement axis
                addMovingEnemyInfluence(influence, pos, type, room);
            } else {
                // Static trap - radial influence
                addStaticTrapInfluence(influence, pos, room);
            }
        }

        // Propagate influence with budget
        propagateInfluence(influence, room);

        return influence;
    }

    private static void addMovingEnemyInfluence(
        Map<Coord, Float> influence,
        Coord pos,
        DangerType type,
        Room room
    ) {
        Direction dir = type.getMovingDir();
        float baseDanger = 1.0f;

        // Lizards patrol horizontally or vertically until hitting a wall
        // Their influence spreads along their patrol path

        Set<Coord> patrolPath = findPatrolPath(pos, dir, room);

        for (Coord tile : patrolPath) {
            influence.put(tile, Math.max(influence.get(tile), baseDanger));

            // Add perpendicular influence (1 tile to sides)
            for (Coord adjacent : getPerpendicularNeighbors(tile, dir, room)) {
                influence.put(adjacent, Math.max(influence.get(adjacent), baseDanger * 0.7f));
            }
        }
    }

    private static Set<Coord> findPatrolPath(Coord start, Direction dir, Room room) {
        Set<Coord> path = new HashSet<>();
        path.add(start);

        int dx = 0, dy = 0;
        if (dir == Direction.RIGHT || dir == Direction.LEFT) {
            dx = 1;
        } else {
            dy = 1;
        }

        // Expand in both directions along axis until wall
        expandAlongAxis(start, dx, dy, path, room);
        expandAlongAxis(start, -dx, -dy, path, room);

        return path;
    }

    private static void expandAlongAxis(Coord start, int dx, int dy, Set<Coord> path, Room room) {
        Coord current = new Coord((int)start.getX() + dx, (int)start.getY() + dy);

        while (room.getTiles().contains(current)) {
            path.add(current);
            current = new Coord((int)current.getX() + dx, (int)current.getY() + dy);
        }
    }

    private static List<Coord> getPerpendicularNeighbors(Coord tile, Direction dir, Room room) {
        List<Coord> neighbors = new ArrayList<>();

        if (dir == Direction.RIGHT || dir == Direction.LEFT) {
            // Moving horizontally, add vertical neighbors
            Coord up = new Coord((int)tile.getX(), (int)tile.getY() - 1);
            Coord down = new Coord((int)tile.getX(), (int)tile.getY() + 1);
            if (room.getTiles().contains(up)) neighbors.add(up);
            if (room.getTiles().contains(down)) neighbors.add(down);
        } else {
            // Moving vertically, add horizontal neighbors
            Coord left = new Coord((int)tile.getX() - 1, (int)tile.getY());
            Coord right = new Coord((int)tile.getX() + 1, (int)tile.getY());
            if (room.getTiles().contains(left)) neighbors.add(left);
            if (room.getTiles().contains(right)) neighbors.add(right);
        }

        return neighbors;
    }

    private static void addStaticTrapInfluence(
        Map<Coord, Float> influence,
        Coord pos,
        Room room
    ) {
        float baseDanger = 0.8f;
        influence.put(pos, Math.max(influence.get(pos), baseDanger));

        // Radial falloff around trap
        Queue<Coord> queue = new LinkedList<>();
        Map<Coord, Integer> distances = new HashMap<>();
        queue.add(pos);
        distances.put(pos, 0);

        int maxRadius = 3;

        while (!queue.isEmpty()) {
            Coord current = queue.poll();
            int dist = distances.get(current);

            if (dist >= maxRadius) continue;

            for (Coord neighbor : getNeighbors(current, room)) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, dist + 1);
                    queue.add(neighbor);

                    float falloff = baseDanger * (float)Math.pow(INFLUENCE_DECAY, dist + 1);
                    influence.put(neighbor, Math.max(influence.get(neighbor), falloff));
                }
            }
        }
    }

    private static void propagateInfluence(Map<Coord, Float> influence, Room room) {
        // Budget-controlled diffusion
        for (int iter = 0; iter < MAX_INFLUENCE_ITERATIONS; iter++) {
            Map<Coord, Float> newInfluence = new HashMap<>(influence);
            boolean changed = false;

            for (Coord tile : room.getTiles()) {
                float currentInfluence = influence.get(tile);
                float totalNeighborInfluence = 0;
                int neighborCount = 0;

                for (Coord neighbor : getNeighbors(tile, room)) {
                    totalNeighborInfluence += influence.get(neighbor);
                    neighborCount++;
                }

                if (neighborCount > 0) {
                    float avgNeighbor = totalNeighborInfluence / neighborCount;
                    float blended = currentInfluence * 0.7f + avgNeighbor * 0.3f * INFLUENCE_DECAY;

                    if (Math.abs(blended - currentInfluence) > 0.01f) {
                        newInfluence.put(tile, blended);
                        changed = true;
                    }
                }
            }

            influence.putAll(newInfluence);

            // Early exit if converged
            if (!changed) break;
        }
    }

    private static void balanceRewards(
        RoomContents contents,
        Map<Coord, Float> influenceMap,
        Room room,
        Random rng
    ) {
        // Place bonus coins in high-danger areas (risk/reward)
        List<Map.Entry<Coord, Float>> sortedByDanger = new ArrayList<>(influenceMap.entrySet());
        sortedByDanger.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        int bonusCoins = (int)(room.getReward() * 5); // Extra high-value coins

        for (int i = 0; i < bonusCoins && i < sortedByDanger.size(); i++) {
            Coord pos = sortedByDanger.get(i).getKey();

            // Only place if not already occupied and influence is high
            if (!contents.getEnemies().containsKey(pos) &&
                !contents.getRewards().containsKey(pos) &&
                influenceMap.get(pos) > 0.5f) {
                contents.addCoin(pos);
            }
        }
    }

    // Original helper methods

    private static Map<Coord, Integer> calculateDistanceField(Room room) {
        Map<Coord, Integer> distances = new HashMap<>();
        Queue<Coord> queue = new LinkedList<>(room.getEntrances());

        for (Coord entrance : room.getEntrances()) {
            distances.put(entrance, 0);
        }

        while (!queue.isEmpty()) {
            Coord current = queue.poll();
            int currentDist = distances.get(current);

            for (Coord neighbor : getNeighbors(current, room)) {
                if (!distances.containsKey(neighbor)) {
                    distances.put(neighbor, currentDist + 1);
                    queue.add(neighbor);
                }
            }
        }

        return distances;
    }

    private static Set<Coord> findSafePathNetwork(Room room, Map<Coord, Integer> distanceField) {
        Set<Coord> safePath = new HashSet<>();
        List<Coord> entrances = new ArrayList<>(room.getEntrances());

        for (int i = 0; i < entrances.size(); i++) {
            for (int j = i + 1; j < entrances.size(); j++) {
                Set<Coord> path = findShortestPath(entrances.get(i), entrances.get(j), room);
                safePath.addAll(path);

                Set<Coord> buffer = new HashSet<>();
                for (Coord tile : path) {
                    buffer.addAll(getNeighbors(tile, room));
                }
                safePath.addAll(buffer);
            }
        }

        return safePath;
    }

    private static Set<Coord> findShortestPath(Coord start, Coord end, Room room) {
        Map<Coord, Coord> cameFrom = new HashMap<>();
        Queue<Coord> queue = new LinkedList<>();
        queue.add(start);
        cameFrom.put(start, null);

        while (!queue.isEmpty()) {
            Coord current = queue.poll();
            if (current.equals(end)) break;

            for (Coord neighbor : getNeighbors(current, room)) {
                if (!cameFrom.containsKey(neighbor)) {
                    queue.add(neighbor);
                    cameFrom.put(neighbor, current);
                }
            }
        }

        Set<Coord> path = new HashSet<>();
        Coord current = end;
        while (current != null) {
            path.add(current);
            current = cameFrom.get(current);
        }

        return path;
    }

    private static Map<String, List<Coord>> categorizeTiles(
        List<Coord> availableTiles,
        Map<Coord, Integer> distanceField,
        Set<Coord> safePathTiles
    ) {
        Map<String, List<Coord>> zones = new HashMap<>();
        zones.put("deep", new ArrayList<>());
        zones.put("medium", new ArrayList<>());
        zones.put("edge", new ArrayList<>());
        zones.put("horizontal", new ArrayList<>()); // For horizontal lizards
        zones.put("vertical", new ArrayList<>());   // For vertical lizards

        if (availableTiles.isEmpty()) return zones;

        int maxDist = availableTiles.stream()
            .mapToInt(distanceField::get)
            .max()
            .orElse(1);

        for (Coord tile : availableTiles) {
            int dist = distanceField.get(tile);
            float normalizedDist = (float) dist / maxDist;

            int distToSafePath = Integer.MAX_VALUE;
            for (Coord safeTile : safePathTiles) {
                int d = Math.abs((int)(tile.getX() - safeTile.getX())) +
                    Math.abs((int)(tile.getY() - safeTile.getY()));
                distToSafePath = Math.min(distToSafePath, d);
            }

            // Check if tile is in a corridor-like space (good for lizards)
            if (isHorizontalCorridor(tile, availableTiles)) {
                zones.get("horizontal").add(tile);
            } else if (isVerticalCorridor(tile, availableTiles)) {
                zones.get("vertical").add(tile);
            }

            if (normalizedDist > 0.6f) {
                zones.get("deep").add(tile);
            } else if (distToSafePath <= 2) {
                zones.get("edge").add(tile);
            } else {
                zones.get("medium").add(tile);
            }
        }

        return zones;
    }

    private static boolean isHorizontalCorridor(Coord tile, List<Coord> availableTiles) {
        // Check if there's a clear horizontal path (3+ tiles in a row)
        int x = (int)tile.getX();
        int y = (int)tile.getY();

        int leftCount = 0, rightCount = 0;
        for (int dx = 1; dx <= 3; dx++) {
            if (availableTiles.contains(new Coord(x - dx, y))) leftCount++;
            if (availableTiles.contains(new Coord(x + dx, y))) rightCount++;
        }

        return (leftCount + rightCount) >= 3;
    }

    private static boolean isVerticalCorridor(Coord tile, List<Coord> availableTiles) {
        // Check if there's a clear vertical path (3+ tiles in a column)
        int x = (int)tile.getX();
        int y = (int)tile.getY();

        int upCount = 0, downCount = 0;
        for (int dy = 1; dy <= 3; dy++) {
            if (availableTiles.contains(new Coord(x, y - dy))) upCount++;
            if (availableTiles.contains(new Coord(x, y + dy))) downCount++;
        }

        return (upCount + downCount) >= 3;
    }

    private static RoomContents placeEntities(
        Map<String, List<Coord>> zones,
        Room room,
        Random rng
    ) {
        RoomContents contents = new RoomContents();

        float difficulty = room.getDifficulty();
        float reward = room.getReward();
        int totalTiles = room.getTiles().size();

        int numTraps = Math.max(1, (int)(difficulty * totalTiles * 0.2f));
        int numLizards = Math.max(1, (int)(difficulty * totalTiles * 0.15f));
        int numCoins = Math.max(2, (int)(reward * totalTiles * 0.25f));

        // Place traps in deep zones
        List<Coord> deepZone = zones.get("deep");
        for (int i = 0; i < numTraps && !deepZone.isEmpty(); i++) {
            Coord pos = deepZone.remove(rng.nextInt(deepZone.size()));
            contents.addTrap(pos);
        }

        // Place lizards - prefer corridor-like spaces
        List<Coord> horizontalZone = zones.get("horizontal");
        List<Coord> verticalZone = zones.get("vertical");
        List<Coord> mediumZone = zones.get("medium");

        int horizontalLizards = numLizards / 2;
        int verticalLizards = numLizards - horizontalLizards;

        for (int i = 0; i < horizontalLizards; i++) {
            if (!horizontalZone.isEmpty()) {
                Coord pos = horizontalZone.remove(rng.nextInt(horizontalZone.size()));
                contents.addLizardX(pos);
            } else if (!mediumZone.isEmpty()) {
                Coord pos = mediumZone.remove(rng.nextInt(mediumZone.size()));
                contents.addLizardX(pos);
            }
        }

        for (int i = 0; i < verticalLizards; i++) {
            if (!verticalZone.isEmpty()) {
                Coord pos = verticalZone.remove(rng.nextInt(verticalZone.size()));
                contents.addLizardY(pos);
            } else if (!mediumZone.isEmpty()) {
                Coord pos = mediumZone.remove(rng.nextInt(mediumZone.size()));
                contents.addLizardY(pos);
            }
        }

        // Coins - spread across zones
        List<Coord> allAvailable = new ArrayList<>();
        allAvailable.addAll(zones.get("deep"));
        allAvailable.addAll(zones.get("medium"));
        allAvailable.addAll(zones.get("edge"));

        for (int i = 0; i < numCoins && !allAvailable.isEmpty(); i++) {
            Coord pos = allAvailable.remove(rng.nextInt(allAvailable.size()));
            contents.addCoin(pos);
        }

        return contents;
    }

    private static List<Coord> getNeighbors(Coord coord, Room room) {
        List<Coord> neighbors = new ArrayList<>();
        int[][] deltas = {{0,1}, {1,0}, {0,-1}, {-1,0}};

        for (int[] d : deltas) {
            Coord neighbor = new Coord((int)coord.getX() + d[0], (int)coord.getY() + d[1]);
            if (room.getTiles().contains(neighbor)) {
                neighbors.add(neighbor);
            }
        }

        return neighbors;
    }
}
