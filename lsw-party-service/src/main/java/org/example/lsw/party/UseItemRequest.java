package org.example.lsw.party;

import org.example.lsw.core.Party;

/**
 * Request body for POSTing to /api/parties/use-item.
 */
public class UseItemRequest {
    private Party party;
    private String itemName;
    private String unitName;

    public UseItemRequest() {}
    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }
    public String getUnitName() { return unitName; }
    public void setUnitName(String unitName) { this.unitName = unitName; }
}
