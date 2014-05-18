package agents;

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

	@Override
	protected void setup() {
		System.out.println("Hello from " + this.getLocalName());
		Object[] args = getArguments();
		if (args != null) {
			System.out.println("My arguments are:");
			for (int i = 0; i < args.length; ++i) {
				System.out.println(args[0].toString());
			}
		}
	}

}
