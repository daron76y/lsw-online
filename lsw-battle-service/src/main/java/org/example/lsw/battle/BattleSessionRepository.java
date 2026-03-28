package org.example.lsw.battle;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Used by BattleService to read and write battle state info to the database
 * Automatically implemented by Spring so we don't need to write sql manually
 */
@Repository
public interface BattleSessionRepository extends JpaRepository<BattleSessionEntity, String> {
    /**
     * Retrieve a battle session based on its owner id and type, instead of its UUID
     */
    //TODO: may be unneccessary
    Optional<BattleSessionEntity> findByOwnerIdAndOwnerType(String ownerId, String ownerType);
}
