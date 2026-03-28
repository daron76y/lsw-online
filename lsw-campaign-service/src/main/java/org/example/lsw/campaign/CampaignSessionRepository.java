package org.example.lsw.campaign;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

//TODO: is this still needed?
@Repository
public interface CampaignSessionRepository extends JpaRepository<CampaignSessionEntity, String> {
    List<CampaignSessionEntity> findByUsernameAndFinishedFalse(String username);
}
