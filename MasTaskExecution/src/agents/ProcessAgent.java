package agents;

import environment.MasTaskEnvironment.AgentData;
import exceptions.MasException;
import jade.core.Agent;

/**
 * @author mtabara
 *
 */
public class ProcessAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private AgentData myData;
	@Override
	protected void setup() {
		System.out.println("Hello from " + this.getLocalName());
		
		Object[] args = getArguments();
		if (args != null) {
			if (args.length > 1) {
				try {
					throw new MasException("Too many arguments sent to agent " + this.getLocalName() + "upon start() calling!");
				} catch (MasException e) {
					e.printStackTrace();
				}
			}
			else {
				myData = (AgentData)args[0];
			}
		}
		
		System.out.println(myData.toString());
	}

}
