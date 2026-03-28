package org.example.lsw.pvp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PvpMatchRepository extends JpaRepository<PvpMatchEntity, String> {}
