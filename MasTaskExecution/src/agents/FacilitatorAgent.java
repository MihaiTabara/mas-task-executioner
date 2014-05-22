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
 *
 */
public class FacilitatorAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private MasTaskEnvironment env;
	private Set<String> agentsReadyToExecute = new HashSet<>();
	private Set<String> agentsWithResults = new HashSet<>();
	Map<Integer, List<Task>> taskAssigner = new HashMap<Integer, List<Task>>();
	private float overAllProfit = 0;
	private float profitPerCycle = 0;
	private int currentCycle = 0;

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
