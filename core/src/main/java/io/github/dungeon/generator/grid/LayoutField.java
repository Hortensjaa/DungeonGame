package io.github.dungeon.generator.grid;


import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.generator.tree.NodeTypes;

import java.util.HashSet;

public class LayoutField {
    NodeTypes.Base type;
    Direction parentDirection;

    HashSet<Coord> placedChildrenPositions = new HashSet<>(); // to backtrack - recursively remove children placements

    public LayoutField(NodeTypes.Base type, Direction parentDirection) {
        this.type = type;
        this.parentDirection = parentDirection;
    }

    public String toString() {
        return "[" + type.getShortName() + "] ";
    }
}
