package io.github.dungeon.generator.algorithm;


import io.github.dungeon.common.Constants;
import io.github.dungeon.generator.tree.DungeonTree;
import io.github.dungeon.generator.tree.DungeonTreeSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

@Data
@AllArgsConstructor
class ArchiveRecord {
    DungeonTree tree;
    float fitness;
}

public class MapElitesArchive {
    private static final int DEFAULT_SIZE = 10;
    private final Function<DungeonTree, Float> xAxisFunction;
    private final Function<DungeonTree, Float> yAxisFunction;
    private final Function<DungeonTree, Float> fitnessFunction;

    private final float min_x;
    private final float max_x;
    private final float min_y;
    private final float max_y;
    private final ArchiveRecord[][] archive;
    private int size;

    public MapElitesArchive(
            Function<DungeonTree, Float> xAxisFunction,
            Function<DungeonTree, Float> yAxisFunction,
            Function<DungeonTree, Float> fitnessFunction,
            float min_x, float max_x, float min_y, float max_y
    ) {
        this(
                xAxisFunction,
                yAxisFunction,
                fitnessFunction,
                min_x, max_x, min_y, max_y,
                DEFAULT_SIZE
        );
    }

    public MapElitesArchive(
            Function<DungeonTree, Float> xAxisFunction,
            Function<DungeonTree, Float> yAxisFunction,
            Function<DungeonTree, Float> fitnessFunction,
            float min_x, float max_x, float min_y, float max_y,
            int size
    ) {
        this.size = size;
        archive = new ArchiveRecord[size][size];
        this.xAxisFunction = xAxisFunction;
        this.yAxisFunction = yAxisFunction;
        this.fitnessFunction = fitnessFunction;
        this.max_x = max_x;
        this.min_x = min_x;
        this.max_y = max_y;
        this.min_y = min_y;
    }

    public void populateArchive(int numIndividuals) {
        for (int i = 0; i < numIndividuals; i++) {
            DungeonTree tree = new DungeonTree();
            tree.generateRandomTree(
                    (int) (Constants.MIN_DEPTH + Math.random() * (Constants.MAX_DEPTH - Constants.MIN_DEPTH)),
                    (float) (0.25 + Math.random() * 0.5),
                    (float) (0.5 + Math.random() * 0.5)
            );
            addToArchive(tree);
        }
    }

    private int mapToIndex(float value, float min, float max) {
        if (value < min || value > max) {
            return -1; // outside archive bounds
        }
        float normalized = (value - min) / (max - min); // [0,1]
        int index = (int) (normalized * size);
        return Math.min(index, size - 1);
    }

    void addToArchive(DungeonTree tree) {
        float xValue = xAxisFunction.apply(tree);
        float yValue = yAxisFunction.apply(tree);

        int xIndex = mapToIndex(xValue, min_x, max_x);
        int yIndex = mapToIndex(yValue, min_y, max_y);

        if (xIndex < 0 || yIndex < 0) {
            return;
        }

        ArchiveRecord existingRecord = archive[xIndex][yIndex];
        float newQuality = fitnessFunction.apply(tree);

        if (existingRecord == null ||
                newQuality >= existingRecord.getFitness()) {
            archive[xIndex][yIndex] = new ArchiveRecord(tree, newQuality);
        }
    }

     public DungeonTree getRandomTree() {
        int xIndex = (int)(Math.random() * size);
        int yIndex = (int)(Math.random() * size);
        ArchiveRecord rec = archive[xIndex][yIndex];
        if (rec != null) {
            return rec.getTree();
        }
        return getRandomTree();
    }

    public DungeonTree getRandomGoodTree() {
        int width = archive.length;
        int height = archive[0].length;

        int startX = (int)(Math.random() * width);
        int startY = (int)(Math.random() * height);

        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int x = (startX + dx) % width;
                int y = (startY + dy) % height;

                ArchiveRecord rec = archive[x][y];
                if (rec != null && rec.getFitness() > 0.0f) {
                    System.out.println("Selected tree at (" + x + ", " + y + ") with fitness " + rec.getFitness());
                    return rec.getTree();
                }
            }
        }

        System.out.println("No good tree found in archive.");
        return null;
    }


    public DungeonTree getTreeAt(int xIndex, int yIndex) {
        if (xIndex < 0 || xIndex >= size || yIndex < 0 || yIndex >= size) {
            throw new IndexOutOfBoundsException("Index out of bounds for archive");
        }
        ArchiveRecord rec = archive[xIndex][yIndex];
        if (rec != null) {
            return rec.getTree();
        }
        return null;
    }

    public void print() {
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                ArchiveRecord rec = archive[x][y];
                if (rec != null) {
                    System.out.printf("[%.2f] ", rec.getFitness());
                } else {
                    System.out.print("[----] ");
                }
            }
            System.out.println();
        }
    }

//    ----------- serialization --------------
    private static String nowAsDirName() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
        return LocalDateTime.now().format(fmt);
    }

    private void createArchiveInfo(File baseDir, int generationCount) {
        StringBuilder sb = new StringBuilder();

        sb.append("Generations: ").append(generationCount).append("\n");
        sb.append("Fitness: ").append("quality-control").append("\n");
        sb.append("X function: ").append("difficulty").append("\n");
        sb.append("X bounds: ").append("min: ").append(min_x).append(" max: ").append(max_x).append("\n");
        sb.append("Y function: ").append("reward").append("\n");
        sb.append("Y bounds: ").append("min: ").append(min_y).append(" max: ").append(max_y).append("\n");
        sb.append("Buckets: ").append(size * size).append(" (").append(size).append(" per line)\n");

        int filledBuckets = 0;
        int validBuckets = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                ArchiveRecord rec = archive[x][y];
                if (rec != null) {
                    filledBuckets++;
                    if (rec.getFitness() > 0.0) {
                        validBuckets++;
                    }
                }
            }
        }

        double filledPercent = 100.0 * filledBuckets / (size * size);
        double validPercent = 100.0 * validBuckets / (size * size);

        sb.append("Filled buckets: ")
                .append(filledBuckets)
                .append(" / ")
                .append(size * size)
                .append(String.format(" (%.0f%%)", filledPercent))
                .append("\n");

        sb.append("Valid buckets (fitness>0.0): ")
                .append(validBuckets)
                .append(" / ")
                .append(size * size)
                .append(String.format(" (%.0f%%)", validPercent))
                .append("\n\n");

        sb.append("Archive map:\n    ");
        for (int x = 0; x < size; x++) {
            sb.append(String.format("  %2d   ", x));
        }
        sb.append("\n");

        for (int y = 0; y < size; y++) {
            sb.append(String.format("%2d ", y));
            for (int x = 0; x < size; x++) {
                ArchiveRecord rec = archive[x][y];
                if (rec != null) {
                    sb.append(String.format("[%.2f] ", rec.getFitness()));
                } else {
                    sb.append("[----] ");
                }
            }
            sb.append("\n");
        }

        File infoFile = new File(baseDir, "info.txt");

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(
                new java.io.FileWriter(infoFile)
        )) {
            writer.write(sb.toString());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to write " + infoFile.getAbsolutePath(), e);
        }
    }


    public String serializeArchive(int generationCount) {
        String timestampDir = nowAsDirName();

        File baseDir = new File("levels", timestampDir);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            throw new RuntimeException("Cannot create directory: " + baseDir);
        }

        createArchiveInfo(baseDir, generationCount);
        for (int y = size - 1; y >= 0; y--) {
            for (int x = 0; x < size; x++) {
                ArchiveRecord rec = archive[x][y];
                if (rec != null) {
                    File out = new File(
                            baseDir,
                            String.format("x_%02d_y_%02d.json", x, y)
                    );

                    try {
                        DungeonTreeSerializer.writeToFile(rec.tree, rec.fitness, out);
                    } catch (IOException e) {
                        throw new RuntimeException(
                                "Failed to write " + out.getAbsolutePath(), e
                        );
                    }
                }
            }
        }
        return timestampDir;
    }

    // todo: add functions like getEasyLevel(), getHardLevel(), etc.
}
