package org.example.lsw.battle;

/**
 * Request body for POSTing to /api/battle/{battleId}/action for submitting battle commands/actions
 * Represents one player action for the current units turn.
 * Simple data carrier for storing the name of the action, and any targets or abilities if relevant (for CAST and ATTACK)
 */
public class BattleActionRequest {
    private String action;       // i.e, "attack" | "defend" | "wait" | "cast"
    private String targetName;   // name of the target unit for attacking or casting offensive spells
    private String abilityName;  // name of the ability, if the action is "cast"

    public BattleActionRequest() {}

    public String getAction()               { return action; }
    public void setAction(String action)    { this.action = action; }
    public String getTargetName()                   { return targetName; }
    public void setTargetName(String targetName)    { this.targetName = targetName; }
    public String getAbilityName()                  { return abilityName; }
    public void setAbilityName(String abilityName)  { this.abilityName = abilityName; }
}
