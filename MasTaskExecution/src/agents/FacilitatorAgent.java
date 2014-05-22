package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import environment.MasTaskEnvironment;
import environment.MasTaskEnvironment.AgentData;
import environment.MasTaskEnvironment.CycleData;
import environment.ProfitCapsule;
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
 * The class that describes the behaviour and the actions
 * of the Facilitator Agent
 */
public class FacilitatorAgent extends Agent {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Keeps all the info about the running environment; The info
	 * comes down from the Application right after platform and main 
	 * container are up. Holds information about both the 
	 * {@link AgentData} and the {@link CycleData}
	 */
	private MasTaskEnvironment env;
	
	/**
	 * A set containing the agents that have finished the two-phase 
	 * algorithm for the current cycle and are ready to send their
	 * results to the {@link FacilitatorAgent}
	 */
	private Set<String> agentsReadyToExecute = new HashSet<>();
	
	/**
	 * A set containing the agents that have finished processing 
	 * and sending over their results to the {@link FacilitatorAgent}
	 */
	private Set<String> agentsWithResults = new HashSet<>();
	
	/**
	 * Map used to associate tasks to a a given agent for the current cycle
	 */
	Map<Integer, List<Task>> taskAssigner = new HashMap<Integer, List<Task>>();
	
	/**
	 * Overall global profit obtained by achieving social welfare
	 */
	private float overAllProfit = 0;
	
	/**
	 * Local profit obtained after an iteration completes
	 */
	private float profitPerCycle = 0;
	
	/**
	 * Holds the current cycle iteration index
	 */
	private int currentCycle = 0;

	
	/* (non-Javadoc)
	 * @see jade.core.Agent#setup()
	 */
	/* (non-Javadoc)
	 * @see jade.core.Agent#setup()
	 */
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
		
		System.out.println("System start: " + env.getNumberOfAgents() + " agents");
		for (int i = 0; i < env.getNumberOfAgents(); i++) {
			System.out.println(env.getAgent(i).toString());
		}
		
		if (currentCycle < env.getNumberOfCycles()) {
			sendGreetings();
		}
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
														     MessageTemplate.MatchProtocol(Constants.STAGE0)));
				
				if (msg != null) {
					int requestingAgent = env.getAgentByName(msg.getSender().getLocalName()).getId();
					List<Task> tasksToAssign = taskAssigner.get(requestingAgent);
					
					if (tasksToAssign != null) {
						String toPrint = "AF -> AP " + (requestingAgent+1) + " ";
						for (Task t : tasksToAssign) {
							toPrint += t.toString() + " ";
						}
						System.out.println(toPrint);
					}
					
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
					List<YellowPageCapsule> ret = new ArrayList<>();
					try {
						ret = (List<YellowPageCapsule>) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						for (YellowPageCapsule yCapsule : ret) {
							for (AgentData agent : env.getAgents()) {
								if (agent.hasCapability(yCapsule.getTask().getRequiredCapability())) {
									yCapsule.addCandidate(agent.getName(), agent.getId());
								}
							}
						}
					}
					
					String toPrint = "AF responds to AP " + ((int)env.getAgentByName(msg.getSender().getLocalName()).getId()+1) + ":";
					for (YellowPageCapsule yCap : ret) {
						toPrint += "[";
						toPrint += yCap.toString();
						toPrint += "]";
					}
					System.out.println(toPrint);
					
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
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
														     MessageTemplate.MatchProtocol(Constants.STAGE3)));
				
				if (msg != null) {
					if (msg.getContent().equals("Ready for execution!")) {
						agentsReadyToExecute.add(new String(msg.getSender().getLocalName()));
						checkAllhaveDone();
					}
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE), 
														     MessageTemplate.MatchProtocol(Constants.STAGE3)));
				
				if (msg != null) {
					ProfitCapsule ret = null;
					try {
						ret = (ProfitCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						String agentName = msg.getSender().getLocalName();
						if (!agentsWithResults.contains(agentName)) {
							agentsWithResults.add(agentName);
							profitPerCycle += ret.getProfit();
							float penalties = -((float)ret.getLeftOversNo()) * env.getLeftoverPenalty();
							profitPerCycle += penalties;
						}
						checkAllResultsArrived();
					}
					
				}
				else {
					block();
				}
			}
		});
	}
	
	/**
	 * This method is the starter of all the process communication. Clears out 
	 * the parameters for the current iteration and sends the first message to 
	 * the agents. Within, it also shuffles the tasks to assign in the following
	 * iterations to the agents
	 */
	protected void sendGreetings() {
		System.out.println("\nCycle " + ((int)currentCycle+1) + ", phase 1:");
		profitPerCycle = 0;
		agentsReadyToExecute.clear();
		agentsWithResults.clear();
		
		taskAssigner = randomizeTasksForCycle(currentCycle);
		
		for (int i = 0; i < env.getNumberOfAgents(); i++) {
			
			ACLMessage greetingsMsg = new ACLMessage(ACLMessage.INFORM);
			greetingsMsg.setProtocol(Constants.STAGE0);
			greetingsMsg.addReceiver(new AID(env.getAgent(i).getName(), AID.ISLOCALNAME));
			greetingsMsg.setContent("Greetings!");
			send(greetingsMsg);
		}
	}
	
	
	/**
	 * Verify all the results from all the agents arrived. If so, compute the 
	 * overall profit, print the final results and, if possible, start the next
	 * iteration.
	 */
	protected void checkAllResultsArrived() {
		if (agentsWithResults.size() == env.getNumberOfAgents()) {
			overAllProfit += profitPerCycle;
			System.out.println("Total profit: " + overAllProfit + " (" + profitPerCycle + " this cycle);\n");
			currentCycle++;
			if (currentCycle < env.getNumberOfCycles()) {
				sendGreetings();
			}
		}
	}

	/**
	 * Verify that all the agents are done with phase 1 - negotiation.
	 * If so, allow them to start phase 2 - the execution.
	 */
	protected void checkAllhaveDone() {
		if (agentsReadyToExecute.size() == env.getNumberOfAgents()) {
			System.out.println("\nCycle " + ((int)currentCycle+1) + ", phase 2:");
			for (int i = 0; i < env.getNumberOfAgents(); i++) {
				ACLMessage greetingsMsg = new ACLMessage(ACLMessage.REQUEST);
				greetingsMsg.setProtocol(Constants.STAGE3);
				greetingsMsg.addReceiver(new AID(env.getAgent(i).getName(), AID.ISLOCALNAME));
				greetingsMsg.setContent("EXECUTE!");
				
				send(greetingsMsg);
			} 
		}
	}

	/**
	 * @param cycleId
	 * 		- the iteration index 
	 * @return
	 * 		- A mapping between the agent index and the tasks it is 
	 * 			supposed to execute at the indicated cycleId iteration
	 */
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
