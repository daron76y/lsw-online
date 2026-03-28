package org.example.lsw.party;

import org.example.lsw.core.Party;

/**
 * Request body for POSTing to /api/parties/level-up.
 * The caller passes the full party, the unit name, and the class to level up.
 * Party-service applies the level-up and returns the updated party.
 */
public class LevelUpRequest {
    //fields
    private Party party;
    private String unitName;
    private String heroClass;

    //constructor
    public LevelUpRequest() {}

    //getters and setters
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
    public String getHeroClass() { return heroClass; }
    public void setHeroClass(String h) { this.heroClass = h; }
}
