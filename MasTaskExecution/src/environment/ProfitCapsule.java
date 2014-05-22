/**
 * 
 */
package environment;

import java.io.Serializable;

/**
 * @author mtabara
 *
 */
public class ProfitCapsule implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private int profit = 0;
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
