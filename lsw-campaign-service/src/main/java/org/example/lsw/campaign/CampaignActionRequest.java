package org.example.lsw.campaign;

/**
 * One player action in the pve campaign overworld or inn.
 * Overworld actions:  next | quit | use-item | level-up | view-party
 * Inn actions:        buy | recruit | confirm-recruit | leave
 * Battle is delegated to battle-service. Client talks to /api/battle directly.
 * Extra fields are only used for certain actions:
 *   itemName   - for "buy" and "use-item"
 *   unitName   - for "use-item", "recruit", "level-up"
 *   heroClass  - for "level-up"
 *   recruitIndex - for "recruit" (index into the recruits list returned by a previous call)
 */
public class CampaignActionRequest {
    private String action;
    private String itemName;
    private String unitName;
    private String heroClass;
    private Integer recruitIndex;

    public CampaignActionRequest() {}

    public String getAction()                         { return action; }
    public void setAction(String action)              { this.action = action; }
    public String getItemName()                       { return itemName; }
    public void setItemName(String itemName)          { this.itemName = itemName; }
    public String getUnitName()                       { return unitName; }
    public void setUnitName(String unitName)          { this.unitName = unitName; }
    public String getHeroClass()                      { return heroClass; }
    public void setHeroClass(String heroClass)        { this.heroClass = heroClass; }
    public Integer getRecruitIndex()                  { return recruitIndex; }
    public void setRecruitIndex(Integer recruitIndex) { this.recruitIndex = recruitIndex; }
}
