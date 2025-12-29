package io.github.dungeon.generator.tree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
public class NodeTypes {

    public static Base fromString(String type, float difficulty, float reward) {
        switch (type) {
            case "Empty":
                return new Empty();
            case "Start":
                return new Start();
            case "Exit":
                return new Exit();
            case "Enemies":
                return new Enemies(difficulty);
            case "Treasure":
                return new Treasure(reward);
            case "EnemyAndTreasure":
                return new EnemyAndTreasure(difficulty, reward);
            default:
                throw new IllegalArgumentException("Unknown node type: " + type);
        }
    }

    public static Base getRandomRoom() {
        double r = Math.random();
        if (r < 0.25) {
            return new Empty();
        } else if (r < 0.5) {
            return new Enemies((float) (Math.random()));
        } else if (r < 0.75) {
            return new EnemyAndTreasure((float) (Math.random()), (float) (Math.random()));
        } else {
            return new Treasure((float) (Math.random()));
        }
    }

    @AllArgsConstructor
    @Getter
    public static abstract class Base {
        float difficulty, reward;  // normalised [0,1]
        String name;
        String shortName;
    }

    public static class Empty extends Base {
        Empty() {
            super(0, 0, "Empty", ".");
        }
    }

    public static class Start extends Base {
        public Start() {
            super(0, 0, "Start", "S");
        }
    }

    public static class Exit extends Base {
        public Exit() {
            super(0, 0, "Exit", "X");
        }
    }

    public static class Enemies extends Base {
        Enemies(float difficulty) {
            super(difficulty, 0, "Enemies", "E");
        }
    }

    public static class Treasure extends Base {
        Treasure(float reward) {
            super(0, reward, "Treasure", "T");
        }
    }

    public static class EnemyAndTreasure extends Base {
        EnemyAndTreasure(float difficulty, float reward) {
            super(difficulty, reward, "EnemyAndTreasure", "B");
        }
    }
}
