package com.example.demo.stego;

import java.util.ArrayList;
import java.util.List;

public class HeatmapGenerator {

    // Calendar heatmap capacity
    public static final int TILES_PER_HEATMAP = 365;
    public static List<HeatmapLayout> generateHeatmaps(
            List<String> symbols,
            List<Integer> permutation) {

        List<HeatmapLayout> layouts = new ArrayList<>();

        int heatmapIndex = 0;
        int tileIndex = 0;

        for (int i = 0; i < permutation.size(); i++) {

            int originalIndex = permutation.get(i);

            // Safety check
            if (originalIndex >= symbols.size()) {
                continue;
            }

            String symbol = symbols.get(originalIndex);

            layouts.add(
                new HeatmapLayout(
                    heatmapIndex,  
                    tileIndex,      
                    symbol
                )
            );

            tileIndex++;
            if (tileIndex == TILES_PER_HEATMAP) {
                heatmapIndex++;
                tileIndex = 0;
            }
        }

        return layouts;
    }
}
