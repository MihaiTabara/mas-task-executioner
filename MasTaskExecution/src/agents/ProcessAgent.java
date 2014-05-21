package agents;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import environment.MasTaskEnvironment.AgentData;
import environment.BidderTuple;
import environment.Task;
import environment.TaskCapsule;
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
public class ProcessAgent extends Agent {

	/**
	 * 
	 */
	protected static final long serialVersionUID = 1L;
	protected static final String facilitatorName = "facilitator";
	
	protected Set<Task> willDo = new HashSet<>();
	protected List<Task> toDo = new ArrayList<>();
	protected Set<Task> leftOvers = new HashSet<>();
	
	private List<YellowPageCapsule> yellowPagedCapsules = new ArrayList<>();
	private Map<Task, List<BidderTuple>> offers = new HashMap<Task, List<BidderTuple>>();
	private Map<Task, Integer> cfpDone = new HashMap<>();
	
	private int nrOfAwaitingBids = 0;
	
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
					List<YellowPageCapsule> ret = new ArrayList<>();
					try {
						ret = (List<YellowPageCapsule>) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						yellowPagedCapsules = ret;
					}
					
					System.out.println("[" + myData.getName() + "]" + "YP: " + yellowPagedCapsules.toString());
					callForProposals();
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
				// TODO aici cele doua conditii de oprire ?
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit CFP de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						int reqCapability = ret.getTask().getRequiredCapability();
						if (myData.getCapabilities().contains(reqCapability)) {
							int cost = myData.getCaps().get(reqCapability);
							if (cost <= myBudget) {
								ret.setOffer(cost);
								ACLMessage msgAnswer = msg.createReply();
								msgAnswer.setPerformative(ACLMessage.PROPOSE);
								try {
									msgAnswer.setContentObject((Serializable) ret);
								} catch (IOException e) {}
								send(msgAnswer);
								nrOfAwaitingBids++;
							}
							else {
								ACLMessage msgAnswer = msg.createReply();
								msgAnswer.setPerformative(ACLMessage.REFUSE);
								try {
									msgAnswer.setContentObject((Serializable) ret);
								} catch (IOException e) {}
								send(msgAnswer);
							}
						}
						else {
							try {
								throw new MasException("[" + myData.getName() + "]" + "cannot process this as cap is missing!");
							} catch (MasException e) {
								e.printStackTrace();
							}
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REFUSE), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit REFUSE de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						List<BidderTuple> bids = offers.get(biddedTask);
						if (bids == null) {
							bids = new ArrayList<>();
						}
						bids.add(new BidderTuple(msg.getSender().getLocalName(), ret.getOffer()));
						offers.put(biddedTask, bids);
						checkBiddedTaskComplete(biddedTask);
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
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit PROPOSE de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						List<BidderTuple> bids = offers.get(biddedTask);
						if (bids == null) {
							bids = new ArrayList<>();
						}
						bids.add(new BidderTuple(msg.getSender().getLocalName(), ret.getOffer()));
						offers.put(biddedTask, bids);
						checkBiddedTaskComplete(biddedTask);
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REJECT_PROPOSAL), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit REJECT de la " + msg.getSender().getLocalName());
					nrOfAwaitingBids--;
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit ACCEPT de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						int cost = myData.getCaps().get(biddedTask.getRequiredCapability());
						if (cost <= myBudget) {
							myBudget -= cost;
							willDo.add(biddedTask);
							
							ACLMessage handshakeMsg = msg.createReply();
							handshakeMsg.setPerformative(ACLMessage.CONFIRM);
							try {
								handshakeMsg.setContentObject((Serializable) ret);
							} catch (IOException e) {}
							send(handshakeMsg);
						}
						else{
							ACLMessage apologizeMsg = msg.createReply();
							apologizeMsg.setPerformative(ACLMessage.DISCONFIRM);
							try {
								apologizeMsg.setContentObject((Serializable) ret);
							} catch (IOException e) {}
							send(apologizeMsg);
						}
					}
					nrOfAwaitingBids--;
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CONFIRM), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit CONFIRM de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						leftOvers.remove(biddedTask);
						
						if (!cfpDone.entrySet().contains(biddedTask))
							cfpDone.put(biddedTask, new Integer(1));
						
						checkAllCFPsDone();
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
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.DISCONFIRM), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
					System.out.println("[" + myData.getName() + "]" + "Am primit DISCONFIRM de la " + msg.getSender().getLocalName());
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						// should already be there, just to make sure we add id anyway
						leftOvers.add(biddedTask);
						
						if (!cfpDone.entrySet().contains(biddedTask))
							cfpDone.put(biddedTask, new Integer(1));
						
						checkAllCFPsDone();
					}
				}
				else {
					block();
				}
			}
		});
	}

	protected void checkBiddedTaskComplete(Task biddedTask) {
		for (YellowPageCapsule yCap : yellowPagedCapsules) {
			if (yCap.getTask().getTaskId() == biddedTask.getTaskId()) {
				// all answers for this task arrived - it can be processed
				if (offers.get(biddedTask).size() == yCap.getCandidates().size()) {
					
					List<BidderTuple> bids = offers.get(biddedTask);
					BidderTuple winner = Collections.min(bids, new Comparator<BidderTuple>() {
						public int compare(BidderTuple a, BidderTuple b) {
					    	return Integer.valueOf(a.getBid()).compareTo(Integer.valueOf(b.getBid()));
						}
					});
					
					if (winner.getBid() >= 0 ) {
						// send winner an ACCEPT_PROPOSAL
						ACLMessage winnerMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
						winnerMsg.setProtocol(Constants.STAGE2);
						winnerMsg.addReceiver(new AID(winner.getAgentName(), AID.ISLOCALNAME));
						try {
							winnerMsg.setContentObject((Serializable) new TaskCapsule(biddedTask));
						} catch (IOException e) {}
						send(winnerMsg);
						
						// send losers a REJECT_PROPOSAL
						bids.remove(winner);
						for (BidderTuple loser : bids) {
							if (loser.getBid() >= 0) {
								// if the agent actually send a PROPOSE message
								ACLMessage loserMsg = new ACLMessage(ACLMessage.REJECT_PROPOSAL);
								loserMsg.setProtocol(Constants.STAGE2);
								loserMsg.addReceiver(new AID(loser.getAgentName(), AID.ISLOCALNAME));
								try {
									loserMsg.setContentObject((Serializable) new TaskCapsule(biddedTask));
								} catch (IOException e) {}
								send(loserMsg);
							}	
						}
					} 
					else {
						if (!cfpDone.entrySet().contains(biddedTask))
							cfpDone.put(biddedTask, new Integer(1));
						
						checkAllCFPsDone();
					}
					
				}
			}
		}
		
	}

	private boolean checkAllCFPsDone() {
		for (YellowPageCapsule yCapsule : yellowPagedCapsules) {
			Task cfpedTask = yCapsule.getTask();
			if (!cfpDone.entrySet().contains(cfpedTask)) {
				return false;
			}
		}
		// TODO ar trebui sa trimit ceva aici - mesaj la server + cealalta conditie de oprire ( poate verifiace in behaviour-ul de CFP)
		return true;
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
		yellowPagedCapsules.clear();
		
		for (Task t : leftOvers) {
			YellowPageCapsule yCapsule = new YellowPageCapsule(t);
			yellowPagedCapsules.add(yCapsule);
		}
		
		ACLMessage yellowMsg = new ACLMessage(ACLMessage.REQUEST);
		yellowMsg.setProtocol(Constants.STAGE1);
		yellowMsg.addReceiver(new AID(facilitatorName, AID.ISLOCALNAME));
		try {
			yellowMsg.setContentObject((Serializable) yellowPagedCapsules);
		} catch (IOException e) {}
		send(yellowMsg);
	}
	
	protected void callForProposals() {
		for (YellowPageCapsule yCapsule : yellowPagedCapsules ) {
			for (String agentName : yCapsule.getCandidates()) {
				TaskCapsule taskCapsule = new TaskCapsule(yCapsule.getTask());
				
				ACLMessage taskCapsuleMsg = new ACLMessage(ACLMessage.CFP);
				taskCapsuleMsg.setProtocol(Constants.STAGE2);
				taskCapsuleMsg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
				try {
					taskCapsuleMsg.setContentObject((Serializable) taskCapsule);
				} catch (IOException e) {}
				send(taskCapsuleMsg);
			}			
		}
	}

}
