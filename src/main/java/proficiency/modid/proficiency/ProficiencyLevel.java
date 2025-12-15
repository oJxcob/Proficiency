package proficiency.modid.proficiency;

// "Proficiency" represents the level of progression a player can achieve with an item type

// Learning note: ordinal(): returns the index of the enum value i.e 0 for UNTRAINED, 1 for RUDIMENTARY, etc.

public enum ProficiencyLevel {
    UNTRAINED, // 0
    RUDIMENTARY, // 1
    NOVICE, // 2
    LEARNING, // 3
    BASIC, // 4
    CAPABLE, // 5
    FAMILIAR, // 6
    ACCUSTOMED, // 7
    PROFICIENT, // 8 | Special unlock progression level
    EXPERIENCED, // 9
    SKILLED, // 10
    ADEPT, // 11
    EXPERT, // 12
    VETERAN, // 13
    ELITE, // 14
    MASTERFUL, // 15

    // Special unlocks
    VIRTUOSO, // 16
    LEGENDARY, // 17
    UNRIVALED; // 18

    // Boolean checks and returns true if `this` level is at least `other` level
    public boolean atLeast(ProficiencyLevel other) {
        return this.ordinal() >= other.ordinal(); // i.e EXPERT.atLeast(PROFICIENT) | Returns true (as 12 >= 8)
    }

    // Clamps `this` level to base progression, so UNTRAINED to MASTERFUL
    // Special unlock levels are returned as MASTERFUL
    // Allows you to display a base proficiency without Special Unlocks
         // *** I low-key forgot what I wanted this for (￣_￣)？
    public ProficiencyLevel clampBase() {
        return this.ordinal() > MASTERFUL.ordinal() ? MASTERFUL : this;
    }

    // Proficiency threshold array | Computes the proficiency level using a required no. points to level up
    public static ProficiencyLevel fromPoints(long points, long[] thresholds) {
        int levelIndex = 0; // Indicates the proficiency level

        for (int i=0; i<Math.min(thresholds.length, MASTERFUL.ordinal()); i++) {
            if (points >= thresholds[i]) { // if player has enough points, update levelIndex
                levelIndex = i + 1;
            } else {
                break; // stop checking once the threshold not met is found
            }
        }
        return ProficiencyLevel.values()[Math.min(levelIndex, MASTERFUL.ordinal())];
    }

}