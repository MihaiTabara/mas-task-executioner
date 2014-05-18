package agents;

import environment.MasTaskEnvironment;
import exceptions.MasException;
import jade.core.Agent;

/**
 * @author mtabara
 *
 */
public class FacilitatorAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MasTaskEnvironment env;

	@Override
	protected void setup() {
		System.out.println("Hello from " + this.getLocalName());
		
		Object[] args = getArguments();
		if (args != null) {
			if (args.length > 1) {
				try {
					throw new MasException("Too many arguments sent to facilitator agent upon start() calling!");
				} catch (MasException e) {
					e.printStackTrace();
				}
			}
			else {
				env = (MasTaskEnvironment)args[0];
			}
		}
		
		System.out.println(env.toString());
	}

}
