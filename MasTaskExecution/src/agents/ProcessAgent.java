package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import environment.MasTaskEnvironment.AgentData;
import environment.Task;
import environment.TaskCapsule;
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
public class ProcessAgent extends Agent {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	protected static final String facilitatorName = "facilitator";
	protected Set<Task> willDo = new HashSet<>();
	protected List<Task> toDo = new ArrayList<>();
	protected Set<Task> leftOvers = new HashSet<>();
	private List<TaskCapsule> taskCapsules = new ArrayList<>();
	protected int myBudget = 0;
	protected AgentData myData;
	
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
															 MessageTemplate.MatchProtocol(Constants.STAGE0)));
				
				if (msg != null) {
					if (msg.getContent().equals("Greetings!")) {
						System.out.println("[" + myData.getName() + "]" + "Am primit greetings de la facilitator!");
						ACLMessage msgAnswer = msg.createReply();
						msgAnswer.setPerformative(ACLMessage.REQUEST);
						send(msgAnswer);
					}
					else {
						System.out.println("[" + myData.getName() + "]" + "Am primit taskuri de la facilitator!");
						List<Task> ret = new ArrayList<>();
						try {
							ret = (List<Task>) msg.getContentObject();
						} catch (UnreadableException e) {}
						
						if (ret != null) {
							for (Task t : ret) {
								toDo.add(t);
							}
						}
						myBudget  = myData.getBudget();
						try {
							evaluateTaskList();
						} catch (MasException e) {
							e.printStackTrace();
						}
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM), 
														     MessageTemplate.MatchProtocol(Constants.STAGE1)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit yellow-pages de la facilitator!");
					List<TaskCapsule> ret = new ArrayList<>();
					try {
						ret = (List<TaskCapsule>) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						taskCapsules = ret;
					}
					
					System.out.println("[" + myData.getName() + "]" + "YP: " + taskCapsules.toString());
				}
				else {
					block();
				}
			}
		});
	}

	protected void evaluateTaskList() throws MasException {
		System.out.println("[" + myData.getName() + "]" + "Todo inainte de filtering: " + toDo.toString());
		
		for (Task t : toDo) {
			if (myData.getCaps().get(t.getRequiredCapability()) == null ) {
				leftOvers.add(t);
			}
		}
		toDo.removeAll(leftOvers);

		Collections.sort(toDo, new Comparator<Task>() {
			public int compare(Task a, Task b) {
		    	Map<Integer, Integer> caps = myData.getCaps();
		    	int costA = caps.get(a.getRequiredCapability());
		    	int costB = caps.get(b.getRequiredCapability());
		    	return -Integer.valueOf(costA).compareTo(Integer.valueOf(costB));
			}
		});
		
		for (Task t : toDo) {
			int cost = myData.getCaps().get(t.getRequiredCapability());
			if (myBudget >= cost) {
				myBudget -= cost;
				willDo.add(t);
			}
			else {
				leftOvers.add(t);
			}
		}
		
		toDo.removeAll(willDo);
		toDo.removeAll(leftOvers);
		
		if (toDo.size() != 0) {
			throw new MasException("Something went wrong with splitting tasks.");
		}
		
		if (leftOvers.size() > 0)
			askForYellowPages();
		
		System.out.println("[" + myData.getName() + "]" + "Todo final: " + toDo.toString());
		System.out.println("[" + myData.getName() + "]" + "willDo: " + willDo.toString());
		System.out.println("[" + myData.getName() + "]" + "Leftovers: " + leftOvers.toString());
	}

	private void askForYellowPages() {
		taskCapsules.clear();
		
		for (Task t : leftOvers) {
			TaskCapsule task = new TaskCapsule(t);
			taskCapsules.add(task);
		}
		
		ACLMessage yellowMsg = new ACLMessage(ACLMessage.REQUEST);
		yellowMsg.setProtocol(Constants.STAGE1);
		yellowMsg.addReceiver(new AID(facilitatorName, AID.ISLOCALNAME));
		try {
			yellowMsg.setContentObject((Serializable) taskCapsules);
		} catch (IOException e) {}
		send(yellowMsg);
	}

}
