package org.example.lsw.core;

import java.util.List;

public interface UnitFactory {
    Party generateEnemyParty(int playerCumulativeLevel);
    List<Unit> generateHeroRecruits(int numRecruits);
}
