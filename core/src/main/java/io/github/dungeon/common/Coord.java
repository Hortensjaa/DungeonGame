package io.github.dungeon.common;

import lombok.Getter;

@Getter
public class Coord {
    private float x;
    private float y;

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Coord(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Coord applyAction(Action action) {
        float new_x = this.x + action.getDx();
        float new_y = this.y + action.getDy();
        return new Coord(new_x, new_y);
    }

    public Coord applyAction(Action action, float velocity) {
        float new_x = this.x + action.getDx() * velocity;
        float new_y = this.y + action.getDy() * velocity;
        return new Coord(new_x, new_y);
    }

    public Coord add(Coord other) {
        return new Coord(this.x + other.x, this.y + other.y);
    }

    @Override
    public String toString() {
        return x + " " + y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coord coord = (Coord) o;
        return Math.abs(x - coord.x) < 1 && Math.abs(y - coord.y) < 1;
    }

    @Override
    public int hashCode() {
        return (int) (31 * x + y);
    }
}
