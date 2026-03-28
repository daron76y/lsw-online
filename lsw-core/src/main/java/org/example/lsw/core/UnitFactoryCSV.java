package org.example.lsw.core;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class UnitFactoryCSV implements UnitFactory {
    private static final Random random = new Random();
    private static final List<String> ENEMY_NAMES = loadNamesFromFile("/org/example/lsw/core/enemy_names.txt");
    private static final List<String> HERO_NAMES = loadNamesFromFile("/org/example/lsw/core/hero_names.txt");
    private static final HeroClass[] HERO_CLASSES = HeroClass.values();

    private static List<String> loadNamesFromFile(String filename) {
        List<String> names = new ArrayList<>();
        try (InputStream is = UnitFactoryCSV.class.getResourceAsStream(filename)) {
            if (is == null) throw new FileNotFoundException("Resource not found: " + filename);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) names.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading " + filename);
            throw new RuntimeException(e);
        }
        return names;
    }

    private static String getRandomName(List<String> names) {
        if  (names.isEmpty()) return "Unknown";
        return names.get(random.nextInt(names.size()));
    }

    public Party generateEnemyParty(int playerCumulativeLevel) {
        Party enemyParty = new Party("Enemy Party");
        int numEnemies = 1 + random.nextInt(4); //1-5 enemies

        //get a random total party level based on the player total level
        int minCumulative = Math.max(1, playerCumulativeLevel - 10);
        int maxCumulative = Math.max(1, playerCumulativeLevel);
        int enemyCumulativeLevel = minCumulative + random.nextInt(maxCumulative - minCumulative + 1);

        // Ensure we have at least 1 level per enemy
        if (enemyCumulativeLevel < numEnemies) enemyCumulativeLevel = numEnemies;

        //distribute the enemy levels across all its units
        int remainingLevels = enemyCumulativeLevel;
        for (int i = 0; i < numEnemies; i++) {
            int enemiesLeft = numEnemies - i;
            // Reserve 1 level for each remaining enemy so none get level 0
            int maxLevelForEnemy = Math.max(1, remainingLevels - (enemiesLeft - 1));
            int level = 1 + random.nextInt(maxLevelForEnemy);

            // Don't take more than what's available after reserving 1 for each remaining enemy
            int maxAllowed = remainingLevels - (enemiesLeft - 1);
            if (level > maxAllowed) level = maxAllowed;
            level = Math.max(1, level); // always at least level 1
            remainingLevels -= level;

            //get a random name thats not already in the party
            String name = getRandomName(ENEMY_NAMES);
            while (enemyParty.getUnitByName(name) != null) name = getRandomName(ENEMY_NAMES);

            //create the enemy unit, with stats related to its level
            Unit enemy = new Unit(
                    name,
                    3 * level,
                    level,
                    10 + level,
                    0,
                    HeroClass.WARRIOR
            );

            //add unit to party
            enemyParty.addUnit(enemy);
        }
        return enemyParty;
    }

    public List<Unit> generateHeroRecruits(int numRecruits) {
        List<Unit> recruits = new ArrayList<>();
        for (int i = 0; i < numRecruits; i++) {
            String name = getRandomName(HERO_NAMES);
            int level = 1 + random.nextInt(3); //lvl 1-3

            //ensure recruits only get assigned to a base class, not a combo class
            HeroClass[] baseClasses = Arrays.stream(HeroClass.values())
                                            .filter(HeroClass::isBase)
                                            .toArray(HeroClass[]::new);
            HeroClass heroClass = baseClasses[random.nextInt(baseClasses.length)];

            recruits.add(new Unit(name, heroClass));
        }
        return recruits;
    }
}