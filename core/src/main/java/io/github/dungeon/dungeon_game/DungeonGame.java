package io.github.dungeon.dungeon_game;


import io.github.dungeon.common.Action;
import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.dungeon_game.danger.Enemy;
import io.github.dungeon.dungeon_game.danger.Trap;
import io.github.dungeon.dungeon_game.game_objects.GameObject;
import io.github.dungeon.dungeon_game.game_objects.Interactable;
import io.github.dungeon.dungeon_game.reward.Reward;
import io.github.dungeon.generator.grid.GridDefinition;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;


@Getter
public class DungeonGame {

    private final int[][] grid;
    private final Player player;
    private final Coord exit;
    private final List<Interactable> interactables;

    public DungeonGame(GridDefinition def) {
        this.grid = def.getGrid();
        this.player = new Player(def.getPlayerStart());
        this.exit = def.getExit();
        this.interactables = def.getDangers().entrySet()
                .stream()
                .map(entry -> {
                    if (entry.getValue().isMoving()) {
                        return new Enemy(entry.getValue(), entry.getKey());
                    } else {
                        return new Trap(entry.getValue(), entry.getKey());
                    }
                })
                .collect(Collectors.toList());
        interactables.addAll(def.getRewards().entrySet().stream().map(
            entry -> new Reward(entry.getValue(), entry.getKey())
        ).toList());
    }

    public boolean move(Action action) {
        Coord next = player.move(action);

        if (isOutOfBounds(player) || collidesWithWall(player)) {
            player.undoMove();
            return false;
        }
        return true;
    }

    public void update(float delta) {
        player.update(delta);
        Coord playerPos = player.getPosition();

        Iterator<Interactable> it = interactables.iterator();

        while (it.hasNext()) {
            Interactable obj = it.next();
            obj.update(delta);

            if (obj instanceof Enemy enemy) {
                Coord next = enemy.move(enemy.getAction());
                if (isOutOfBounds(enemy) || collidesWithWall(enemy)) {
                    enemy.undoMove();
                    enemy.setAction(enemy.getAction().opposite());
                }
            }

            if (obj.getPosition().equals(playerPos)) {
                obj.onInteraction(player);

                if (obj instanceof Trap || obj instanceof Reward) {
                    it.remove();
                }
            }
        }

        if (hasWon()) {
            System.out.println("You have reached the exit and won the game!");
        }
    }

    public boolean hasWon() {
        return player.getPosition().equals(exit);
    }

    private boolean isOutOfBounds(GameObject o) {
        return o.left() < 0
            || o.top() < 0
            || o.right() >= Constants.COLUMNS
            || o.bottom() >= Constants.ROWS;
    }

    private boolean collidesWithWall(GameObject o) {
        int minX = (int) Math.floor(o.left());
        int maxX = (int) Math.floor(o.right());
        int minY = (int) Math.floor(o.top());
        int maxY = (int) Math.floor(o.bottom());

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                if (x < 0 || y < 0
                    || x >= Constants.COLUMNS
                    || y >= Constants.ROWS)
                    return true;

                if (grid[y][x] == Constants.WALL)
                    return true;
            }
        }
        return false;
    }

}
