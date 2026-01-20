package io.github.dungeon.generator.grid;

import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.dungeon_game.danger.DangerType;
import java.util.*;

public class RoomPopulator {

    public static RoomContents populate(Room room) {
        if (room.getEntrances().isEmpty() || room.getTiles().isEmpty()) {
            return new RoomContents();
        }

        Random rng = new Random();
        RoomContents contents = new RoomContents();

        // 1. Calculate distance from entrances (one-time BFS)
        Map<Coord, Integer> distanceFromEntrances = calculateDistanceField(room);

        // 2. Find safe corridor between entrances
        Set<Coord> safePath = findMainPath(room, distanceFromEntrances);

        // 3. Get placeable tiles
        Set<Coord> placeable = new HashSet<>(room.getTiles());
        placeable.removeAll(safePath);
        placeable.removeAll(room.getEntrances());

        if (placeable.isEmpty()) {
            return contents;
        }

        // 4. Categorize tiles by strategic value
        TileCategories categories = categorizeTiles(placeable, distanceFromEntrances, safePath, room);

        // 5. Place enemies based on difficulty
        placeEnemies(contents, categories, room.getDifficulty(), rng);

        // 6. Place rewards strategically
        placeRewards(contents, categories, placeable, room.getReward(), rng);

        return contents;
    }

    private static class TileCategories {
        List<Coord> deepCorners = new ArrayList<>();      // Far from all entrances
        List<Coord> ambushSpots = new ArrayList<>();      // 2-3 tiles from safe path
        List<Coord> horizontalPaths = new ArrayList<>();  // Good for horizontal enemies
        List<Coord> verticalPaths = new ArrayList<>();    // Good for vertical enemies
        List<Coord> chokepoints = new ArrayList<>();      // Narrow passages
        List<Coord> openAreas = new ArrayList<>();        // Open spaces
    }

    private static TileCategories categorizeTiles(
        Set<Coord> placeable,
        Map<Coord, Integer> distanceFromEntrances,
        Set<Coord> safePath,
        Room room
    ) {
        TileCategories cat = new TileCategories();

        int maxDist = placeable.stream()
            .mapToInt(distanceFromEntrances::get)
            .max()
            .orElse(1);

        for (Coord tile : placeable) {
            int distToEntrance = distanceFromEntrances.get(tile);
            int distToSafePath = minDistance(tile, safePath);
            int neighborCount = countNeighbors(tile, room);

            // Categorize based on spatial properties
            float normalizedDist = (float) distToEntrance / maxDist;

            // Deep corners: far from entrances, few neighbors
            if (normalizedDist > 0.7f && neighborCount <= 2) {
                cat.deepCorners.add(tile);
            }

            // Ambush spots: close to safe path but not on it
            if (distToSafePath >= 2 && distToSafePath <= 3) {
                cat.ambushSpots.add(tile);
            }

            // Chokepoints: narrow passages (2 neighbors in line)
            if (isChokepoint(tile, room)) {
                cat.chokepoints.add(tile);
            }

            // Patrol paths for lizards
            int horizontalClearance = getHorizontalClearance(tile, room);
            int verticalClearance = getVerticalClearance(tile, room);

            if (horizontalClearance >= 4) {
                cat.horizontalPaths.add(tile);
            }
            if (verticalClearance >= 4) {
                cat.verticalPaths.add(tile);
            }

            // Open areas: 3-4 neighbors
            if (neighborCount >= 3) {
                cat.openAreas.add(tile);
            }
        }

        return cat;
    }

    private static void placeEnemies(
        RoomContents contents,
        TileCategories categories,
        float difficulty,
        Random rng
    ) {
        // Calculate enemy budget
        int totalPlaceableTiles = categories.deepCorners.size() +
            categories.ambushSpots.size() +
            categories.chokepoints.size() +
            categories.openAreas.size();

        int trapBudget = (int)(difficulty * totalPlaceableTiles * 0.25f);
        int lizardBudget = (int)(difficulty * totalPlaceableTiles * 0.15f);

        // STRATEGY 1: Traps in corners and ambush spots (static danger)
        int trapsInCorners = Math.min(trapBudget / 2, categories.deepCorners.size());
        placeFromList(contents::addTrap, categories.deepCorners, trapsInCorners, rng);

        int trapsInAmbush = Math.min(trapBudget - trapsInCorners, categories.ambushSpots.size());
        placeFromList(contents::addTrap, categories.ambushSpots, trapsInAmbush, rng);

        // STRATEGY 2: Lizards in patrol paths (moving danger)
        List<Coord> horizontalPaths = new ArrayList<>(categories.horizontalPaths);
        List<Coord> verticalPaths = new ArrayList<>(categories.verticalPaths);

        // Remove positions that already have enemies
        horizontalPaths.removeIf(c -> contents.getEnemies().containsKey(c));
        verticalPaths.removeIf(c -> contents.getEnemies().containsKey(c));

        int horizontalLizards = lizardBudget / 2;
        int verticalLizards = lizardBudget - horizontalLizards;

        // Spread lizards out (don't cluster on same axis)
        placeSpreadOut(contents::addLizardX, horizontalPaths, horizontalLizards, true, rng);
        placeSpreadOut(contents::addLizardY, verticalPaths, verticalLizards, false, rng);

        // STRATEGY 3: Fill remaining budget in chokepoints
        int remainingTraps = trapBudget - trapsInCorners - trapsInAmbush;
        if (remainingTraps > 0) {
            List<Coord> chokepoints = new ArrayList<>(categories.chokepoints);
            chokepoints.removeIf(c -> contents.getEnemies().containsKey(c));
            placeFromList(contents::addTrap, chokepoints, remainingTraps, rng);
        }
    }

    private static void placeRewards(
        RoomContents contents,
        TileCategories categories,
        Set<Coord> placeable,
        float reward,
        Random rng
    ) {
        int coinBudget = Math.max(2, (int)(reward * placeable.size() * 0.3f));

        // STRATEGY: 3-tier reward placement
        // - Tier 1 (40%): Near enemies (risk/reward)
        // - Tier 2 (30%): In corners/edges (exploration)
        // - Tier 3 (30%): Random scatter (breadcrumbs)

        int tier1Coins = (int)(coinBudget * 0.4f);
        int tier2Coins = (int)(coinBudget * 0.3f);
        int tier3Coins = coinBudget - tier1Coins - tier2Coins;

        // Tier 1: Next to enemies (1-2 tiles away)
        List<Coord> nearDanger = new ArrayList<>();
        for (Coord enemyPos : contents.getEnemies().keySet()) {
            for (Coord neighbor : getNeighborsInRadius(enemyPos, 2, placeable)) {
                if (!contents.getEnemies().containsKey(neighbor)) {
                    nearDanger.add(neighbor);
                }
            }
        }
        placeFromList(contents::addCoin, nearDanger, tier1Coins, rng);

        // Tier 2: Deep corners (exploration reward)
        List<Coord> corners = new ArrayList<>(categories.deepCorners);
        corners.removeIf(c -> contents.getEnemies().containsKey(c) ||
            contents.getRewards().containsKey(c));
        placeFromList(contents::addCoin, corners, tier2Coins, rng);

        // Tier 3: Random scatter
        List<Coord> available = new ArrayList<>(placeable);
        available.removeIf(c -> contents.getEnemies().containsKey(c) ||
            contents.getRewards().containsKey(c));
        placeFromList(contents::addCoin, available, tier3Coins, rng);
    }

    // === PLACEMENT HELPERS ===

    private static void placeFromList(
        java.util.function.Consumer<Coord> placer,
        List<Coord> candidates,
        int count,
        Random rng
    ) {
        List<Coord> available = new ArrayList<>(candidates);
        for (int i = 0; i < count && !available.isEmpty(); i++) {
            Coord pos = available.remove(rng.nextInt(available.size()));
            placer.accept(pos);
        }
    }

    private static void placeSpreadOut(
        java.util.function.Consumer<Coord> placer,
        List<Coord> candidates,
        int count,
        boolean horizontal,
        Random rng
    ) {
        if (candidates.isEmpty()) return;

        List<Coord> placed = new ArrayList<>();
        List<Coord> available = new ArrayList<>(candidates);

        for (int i = 0; i < count && !available.isEmpty(); i++) {
            // Sort by distance from already-placed lizards
            if (!placed.isEmpty()) {
                available.sort((a, b) -> {
                    int minDistA = placed.stream()
                        .mapToInt(p -> axisDistance(a, p, horizontal))
                        .min()
                        .orElse(Integer.MAX_VALUE);
                    int minDistB = placed.stream()
                        .mapToInt(p -> axisDistance(b, p, horizontal))
                        .min()
                        .orElse(Integer.MAX_VALUE);
                    return Integer.compare(minDistB, minDistA); // Prefer farther
                });
            }

            Coord pos = available.remove(0); // Take the furthest
            placer.accept(pos);
            placed.add(pos);
        }
    }

    private static int axisDistance(Coord a, Coord b, boolean horizontal) {
        if (horizontal) {
            return Math.abs((int)a.getY() - (int)b.getY()); // Different rows
        } else {
            return Math.abs((int)a.getX() - (int)b.getX()); // Different columns
        }
    }

    // === SPATIAL ANALYSIS ===

    private static int getHorizontalClearance(Coord tile, Room room) {
        int x = (int)tile.getX();
        int y = (int)tile.getY();

        int left = 0, right = 0;
        while (room.getTiles().contains(new Coord(x - left - 1, y))) left++;
        while (room.getTiles().contains(new Coord(x + right + 1, y))) right++;

        return left + right + 1;
    }

    private static int getVerticalClearance(Coord tile, Room room) {
        int x = (int)tile.getX();
        int y = (int)tile.getY();

        int up = 0, down = 0;
        while (room.getTiles().contains(new Coord(x, y - up - 1))) up++;
        while (room.getTiles().contains(new Coord(x, y + down + 1))) down++;

        return up + down + 1;
    }

    private static boolean isChokepoint(Coord tile, Room room) {
        List<Coord> neighbors = getNeighbors(tile, room);
        if (neighbors.size() != 2) return false;

        // Check if neighbors are opposite (forms a line)
        Coord n1 = neighbors.get(0);
        Coord n2 = neighbors.get(1);

        boolean horizontal = (int)n1.getY() == (int)tile.getY() &&
            (int)n2.getY() == (int)tile.getY();
        boolean vertical = (int)n1.getX() == (int)tile.getX() &&
            (int)n2.getX() == (int)tile.getX();

        return horizontal || vertical;
    }

    private static int countNeighbors(Coord tile, Room room) {
        return getNeighbors(tile, room).size();
    }

    private static int minDistance(Coord tile, Set<Coord> targets) {
        return targets.stream()
            .mapToInt(t -> Math.abs((int)(tile.getX() - t.getX())) +
                Math.abs((int)(tile.getY() - t.getY())))
            .min()
            .orElse(Integer.MAX_VALUE);
    }

    private static List<Coord> getNeighborsInRadius(Coord center, int radius, Set<Coord> validTiles) {
        List<Coord> result = new ArrayList<>();
        int cx = (int)center.getX();
        int cy = (int)center.getY();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                if (dx == 0 && dy == 0) continue;
                if (Math.abs(dx) + Math.abs(dy) > radius) continue;

                Coord coord = new Coord(cx + dx, cy + dy);
                if (validTiles.contains(coord)) {
                    result.add(coord);
                }
            }
        }

        return result;
    }

    // === PATHFINDING ===

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

    private static Set<Coord> findMainPath(Room room, Map<Coord, Integer> distanceField) {
        Set<Coord> path = new HashSet<>();
        List<Coord> entrances = new ArrayList<>(room.getEntrances());

        // Find paths between all entrance pairs
        for (int i = 0; i < entrances.size(); i++) {
            for (int j = i + 1; j < entrances.size(); j++) {
                path.addAll(findShortestPath(entrances.get(i), entrances.get(j), room));
            }
        }

        // Add 1-tile buffer
        Set<Coord> buffered = new HashSet<>(path);
        for (Coord tile : path) {
            buffered.addAll(getNeighbors(tile, room));
        }

        return buffered;
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
