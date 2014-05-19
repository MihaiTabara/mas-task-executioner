package agents;

import java.io.Serializable;
import java.util.List;

import environment.MasTaskEnvironment.AgentData;
import environment.Task;
import exceptions.MasException;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

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
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));
				
				if (msg != null) {
					if (msg.getProtocol().equals(Constants.STAGE1)) {
						if (msg.getContent().equals("Greetings!")) {
							System.out.println("[" + myData.getName() + "]" + "Am primit greetings de la facilitator!");
							ACLMessage msgAnswer = msg.createReply();
							msgAnswer.setPerformative(ACLMessage.REQUEST);
							send(msgAnswer);
						}
						else {
							System.out.println("[" + myData.getName() + "]" + "Am primit taskuri de la facilitator!");
							try {
								List<Task> ret = (List<Task>) msg.getContentObject();
								// pay attention to null objects!
								System.out.println(ret.toString());
							} catch (UnreadableException e) {}
						}
					}
				}
				else {
					block();
				}
			}
		});
		
	}

}
