package environment;

/**
 * @author mtabara
 * The class to define the pair of bidder and bid
 */
public class BidderTuple {
	/**
	 * The agent that placed the bid
	 */
	private String agentName;
	
	/**
	 * The actual bid the agent has made
	 */
	private int bid;

	public BidderTuple(String agentName, int bid) {
		this.agentName = agentName;
		this.bid = bid;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public int getBid() {
		return bid;
	}

	public void setBid(int bid) {
		this.bid = bid;
	}

	@Override
	public String toString() {
		return "BidderTuple [agentName=" + agentName + ", bid=" + bid + "]";
	}

}
