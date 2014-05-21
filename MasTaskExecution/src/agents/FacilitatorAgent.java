package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import environment.MasTaskEnvironment;
import environment.MasTaskEnvironment.AgentData;
import environment.MasTaskEnvironment.CycleData;
import environment.Task;
import environment.YellowPageCapsule;
import exceptions.MasException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;

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
		
		for (int i = 0; i < env.getNumberOfAgents(); i++) {
			ACLMessage greetingsMsg = new ACLMessage(ACLMessage.INFORM);
			greetingsMsg.setProtocol(Constants.STAGE0);
			greetingsMsg.addReceiver(new AID(env.getAgent(i).getName(), AID.ISLOCALNAME));
			greetingsMsg.setContent("Greetings!");
			send(greetingsMsg);
		}
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;
			Map<Integer, List<Task>> taskAssigner = randomizeTasksForCycle(0);

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
														     MessageTemplate.MatchProtocol(Constants.STAGE0)));
				
				if (msg != null) {
					System.out.println("[facilitator] Am primit REQUEST de la " + msg.getSender().getLocalName());
					int requestingAgent = env.getAgentByName(msg.getSender().getLocalName()).getId();
					List<Task> tasksToAssign = taskAssigner.get(requestingAgent);
					
					ACLMessage msgToSend = msg.createReply();
					msgToSend.setPerformative(ACLMessage.INFORM);
					try {
						msgToSend.setContentObject((Serializable) tasksToAssign);
					} catch (IOException e) {}
					send(msgToSend);
				}
				else {
					block();
				}
			}
		});
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
														     MessageTemplate.MatchProtocol(Constants.STAGE1)));
				
				if (msg != null) {
					System.out.println("[facilitator] Am primit REQUEST-YELLOW-PAGES de la " + msg.getSender().getLocalName());
					List<YellowPageCapsule> ret = new ArrayList<>();
					try {
						ret = (List<YellowPageCapsule>) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						for (YellowPageCapsule yCapsule : ret) {
							for (AgentData agent : env.getAgents()) {
								if (agent.hasCapability(yCapsule.getTask().getRequiredCapability())) {
									yCapsule.addCandidate(agent.getName());
								}
							}
						}
					}
					
					ACLMessage msgToSend = msg.createReply();
					msgToSend.setPerformative(ACLMessage.INFORM);
					try {
						msgToSend.setContentObject((Serializable) ret);
					} catch (IOException e) {}
					send(msgToSend);
				}
				else {
					block();
				}
			}
		});
	}
	
	private Map<Integer, List<Task>> randomizeTasksForCycle(int cycleId) {
		Map<Integer, List<Task>> taskAssigner = new TreeMap<>();
		CycleData currentCycleData = env.getCycle(cycleId);
	
		int jAgent = 0;
		
		for (Task t : currentCycleData.getTasks()) {
			if (jAgent == env.getNumberOfAgents()) {
				jAgent = 0;
			}
			
			List<Task> ret = taskAssigner.get(jAgent);
			if (ret == null) {
				taskAssigner.put(jAgent, new ArrayList<Task>());
			}
			taskAssigner.get(jAgent).add(t);
			jAgent++;
		}
		
		return taskAssigner;
	}

}
