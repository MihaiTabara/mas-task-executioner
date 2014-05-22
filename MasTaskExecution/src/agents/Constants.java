package agents;

/**
 * @author mtabara
 * The constants class used across the project by all entities
 */
public final class Constants {

	/**
	 * Private Constructor of the constants class
	 */
	private Constants() {
		
	}

	/**
	 * STAGE0 is the starting stage in which the Facilitator
	 * assigns the tasks to their corresponding agents
	 */
	public static final String STAGE0 = "ASSIGNMENT";
	
	/**
	 * STAGE1 is an optional stage that may be used 
	 * by some agents to gather more information about 
	 * their tasks from Facilitator
	 */
	public static final String STAGE1 = "YELLOWPAGES";
	
	/**
	 * STAGE2 represents the most complex stage of all.
	 * It consists of all the protocol-based messages exchanged
	 * between the agents in their attempt to achieve social welfare
	 */
	public static final String STAGE2 = "BIDDING";
	
	/**
	 * STAGE3 represents the final stage in which the agents
	 * communicate their results to the Facilitator 
	 */
	public static final String STAGE3 = "EXECUTION";
	
	/**
	 * INFINIT is used to default all the offers exchanged 
	 * by the agents in the system
	 */
	public static final int INFINIT = 100000;
}
