package com.example.demo.stego;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class TilePermutationUtil {

    // ================= ENCODE =================
    public static List<Integer> permutePositions(
            int size, byte[] aesKey) throws Exception {

        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < size; i++) indices.add(i);

        long seed = deriveSeed(aesKey);

        Collections.shuffle(indices, new Random(seed));

        return indices;
    }

    // ================= DECODE =================
    public static List<String> inversePermute(
            List<String> permutedSymbols,
            byte[] aesKey) throws Exception {

        int size = permutedSymbols.size();
        List<Integer> permutation =
                permutePositions(size, aesKey);

        List<String> original =
                new ArrayList<>(Collections.nCopies(size, ""));

        for (int i = 0; i < size; i++) {
            original.set(permutation.get(i),
                         permutedSymbols.get(i));
        }

        return original;
    }

    // ================= SEED =================
    private static long deriveSeed(byte[] key)
            throws Exception {

        MessageDigest md =
                MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(key);

        long seed = 0;
        for (int i = 0; i < 8; i++) {
            seed = (seed << 8) | (hash[i] & 0xff);
        }
        return seed;
    }
}
