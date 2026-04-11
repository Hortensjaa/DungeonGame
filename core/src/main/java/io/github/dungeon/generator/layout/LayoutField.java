package io.github.dungeon.generator.layout;


import io.github.dungeon.common.Coord;
import io.github.dungeon.common.Direction;
import io.github.dungeon.generator.tree.NodeTypes;
import lombok.Getter;

import java.util.HashSet;

@Getter
public class LayoutField {
    NodeTypes.Base type;
    Direction parentDirection;

    HashSet<Coord> placedChildrenPositions = new HashSet<>(); // to backtrack - recursively remove children placements

    public LayoutField(NodeTypes.Base node_specs, Direction parentDirection) {
        this.type = node_specs;
        this.parentDirection = parentDirection;
    }

    public String toString() {
        return "[" + type.getShortName() + "] ";
    }
}
