package org.example.lsw.party;

import org.example.lsw.core.Party;
import java.util.List;

/**
 * Standard response for party operations and commands
 * It contains the updated party and any messages to display to the player.
 */
public class PartyResponse {
    private Party party;
    private List<String> messages;

    public PartyResponse() {} //for jackson
    public PartyResponse(Party party, List<String> messages) {
        this.party = party;
        this.messages = messages;
    }

    public Party getParty() { return party; }
    public void setParty(Party party) { this.party = party; }
    public List<String> getMessages() { return messages; }
    public void setMessages(List<String> messages) { this.messages = messages; }
}
