package org.example.lsw.party;

import org.example.lsw.core.Party;
import org.example.lsw.core.Unit;

/**
 * Request body for POSTing to /api/parties/recruit.
 * Client first calls GET /api/parties/recruits to get the recruit candidates,
 * then it passes the chosen unit back here along with the party.
 */
public class RecruitRequest {
    private Party party;
    private Unit recruit;

    public RecruitRequest() {}
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public Unit getRecruit() { return recruit; }
    public void setRecruit(Unit recruit) { this.recruit = recruit; }
}
