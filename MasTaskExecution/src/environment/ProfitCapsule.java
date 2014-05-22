/**
 * 
 */
package environment;

import java.io.Serializable;

import agents.FacilitatorAgent;

/**
 * @author mtabara
 * The structure to define the capsule sent to the {@link FacilitatorAgent}
 * by the agents to communicate their results after execution
 */
public class ProfitCapsule implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The profit of the current iteration cycle
	 */
	private int profit = 0;
	
	/**
	 *  The number of leftovers task for the current iteration
	 */
	private int leftOversNo = 0;
	
	public ProfitCapsule(int profit, int leftOversNo) {
		this.profit = profit;
		this.leftOversNo = leftOversNo;
	}

	public int getProfit() {
		return profit;
	}

	public void setProfit(int profit) {
		this.profit = profit;
	}

	public int getLeftOversNo() {
		return leftOversNo;
	}

	public void setLeftOversNo(int leftOversNo) {
		this.leftOversNo = leftOversNo;
	}
	
}
