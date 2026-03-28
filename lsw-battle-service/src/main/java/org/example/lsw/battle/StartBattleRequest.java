package org.example.lsw.battle;

import org.example.lsw.core.Party;

/**
 * The request body for POST /api/battle/start to start a battle session
 * Contains the two parties and who (PVP or PVE) owns this session.
 * Simple data carrier for PVP and PVE to send two parties into battle.
 */
public class StartBattleRequest {
    private String ownerId; //campaign session ID or PVP match ID
    private String ownerType; //"PVE" or "PVP"
    private Party partyA;
    private Party partyB;

    public StartBattleRequest() {}

    public String getOwnerId()         { return ownerId; }
    public void setOwnerId(String id)  { this.ownerId = id; }
    public String getOwnerType()               { return ownerType; }
    public void setOwnerType(String ownerType) { this.ownerType = ownerType; }
    public Party getPartyA()           { return partyA; }
    public void setPartyA(Party p)     { this.partyA = p; }
    public Party getPartyB()           { return partyB; }
    public void setPartyB(Party p)     { this.partyB = p; }
}
