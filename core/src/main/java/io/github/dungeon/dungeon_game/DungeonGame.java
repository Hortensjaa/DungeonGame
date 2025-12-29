package io.github.dungeon.dungeon_game;


import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import io.github.dungeon.common.Action;
import io.github.dungeon.common.Constants;
import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.dungeon_game.game_objects.Enemy;
import io.github.dungeon.dungeon_game.game_objects.GameObject;
import io.github.dungeon.dungeon_game.game_objects.Player;
import io.github.dungeon.dungeon_game.game_objects.Reward;
import io.github.dungeon.generator.grid.GridDefinition;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;


@Getter
public class DungeonGame {

    private final int[][] grid;
    private final Player player;
    private final Coord exit;
    private final List<Enemy> enemies;
    private final List<Reward> rewards;

    public DungeonGame(GridDefinition def) {
        this.grid = def.getGrid();
        this.player = new Player(def.getPlayerStart());
        this.exit = def.getExit();
        this.enemies = def.getEnemies().entrySet()
                .stream()
                .map(entry -> new Enemy(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
        this.rewards = def.getRewards().entrySet()
                .stream()
                .map(entry -> new Reward(entry.getValue(), entry.getKey()))
                .collect(Collectors.toList());
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

        rewards.removeIf(reward -> {
            reward.update(delta);
            if (reward.getPosition().equals(playerPos)) {
                reward.applyEffect(player);
                return true;
            }
            return false;
        });

        for (Enemy enemy : enemies) {
            enemy.update(delta);
            Coord next = enemy.move(enemy.getAction());
            if (isOutOfBounds(enemy) || collidesWithWall(enemy)) {
                enemy.undoMove();
                enemy.setAction(enemy.getAction().opposite());
            }
            if (enemy.getPosition().equals(playerPos)) {
                enemy.attack(player);
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
