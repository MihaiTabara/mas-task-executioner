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
import environment.ProfitCapsule;
import environment.Task;
import environment.TaskCapsule;
import environment.YellowPageCapsule;
import exceptions.MasException;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
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
	private Map<Integer, List<BidderTuple>> offers = new HashMap<Integer, List<BidderTuple>>();
	private Map<Integer, Integer> cfpDone = new HashMap<>();
	
	private int nrOfAwaitingBids = -1;
	
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
						willDo.clear();
						toDo.clear();
						offers.clear();
						cfpDone.clear();
						nrOfAwaitingBids = 0;
						
						ACLMessage msgAnswer = msg.createReply();
						msgAnswer.setPerformative(ACLMessage.REQUEST);
						send(msgAnswer);
					}
					else {
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
					List<YellowPageCapsule> ret = new ArrayList<>();
					try {
						ret = (List<YellowPageCapsule>) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						for (YellowPageCapsule yCap : ret) {
							yellowPagedCapsules.add(yCap);
						}
						callForProposals();
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

				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP), 
														     MessageTemplate.MatchProtocol(Constants.STAGE2)));
				
				if (msg != null) {
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
								System.out.println("AP " + ((int)myData.getId()+1) + " bids cost " + cost + " for " + ret.getTask().toString());
								nrOfAwaitingBids++;
								send(msgAnswer);
							}
							else {
								ACLMessage msgAnswer = msg.createReply();
								msgAnswer.setPerformative(ACLMessage.REFUSE);
								try {
									msgAnswer.setContentObject((Serializable) ret);
								} catch (IOException e) {}
								System.out.println("AP " + ((int)myData.getId()+1) + " refuses " + ret.getTask().toString());
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
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						Integer taskId = Integer.valueOf(biddedTask.getTaskId());
						List<BidderTuple> bids = offers.get(taskId);
						if (bids == null) {
							bids = new ArrayList<>();
						}
						bids.add(new BidderTuple(msg.getSender().getLocalName(), ret.getOffer()));
						offers.put(taskId, bids);
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
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}

					if (ret != null) {
						Task biddedTask =  ret.getTask();
						
						Integer taskId = Integer.valueOf(biddedTask.getTaskId());
						List<BidderTuple> bids = offers.get(taskId);
						if (bids == null) {
							bids = new ArrayList<>();
						}
						bids.add(new BidderTuple(msg.getSender().getLocalName(), ret.getOffer()));
						offers.put(taskId, bids);
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
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						System.out.println(biddedTask.toString() + " assigned to AP " + ((int)myData.getId()+1));
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
							System.out.println(biddedTask.toString() + " is dropped (due to budget conflict) by AP " + ((int)myData.getId()+1));
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
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						Integer biddedTaskId = Integer.valueOf(biddedTask.getTaskId());
						 
						Task toRemove = null;
						for (Task t : leftOvers) {
							if (t.getTaskId() == biddedTask.getTaskId()) {
								toRemove = t;
								break;
							}
						}
						leftOvers.remove(toRemove);
						
						if (!cfpDone.entrySet().contains(biddedTaskId)) {
							cfpDone.put(biddedTaskId, new Integer(1));
						}
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
					TaskCapsule ret = null;
					try {
						ret = (TaskCapsule) msg.getContentObject();
					} catch (UnreadableException e) {}
					
					if (ret != null) {
						Task biddedTask =  ret.getTask();
						Integer biddedTaskId = Integer.valueOf(biddedTask.getTaskId());
						
						System.out.println("AP " + ((int)myData.getId()+1) + " postpones " + biddedTask.toString() + " as was dropped.");
						if (!cfpDone.entrySet().contains(biddedTaskId)) {
							cfpDone.put(biddedTaskId, new Integer(1));
						}
						checkAllCFPsDone();						
					}
				}
				else {
					block();
				}
			}
		});
		
		addBehaviour(new TickerBehaviour(this, 5000) {

            /**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
            protected void onTick() {
                if (nrOfAwaitingBids == 0 && checkAllCFPsDone()) {
                	nrOfAwaitingBids = -1;
                	
                	ACLMessage donePhase1Msg = new ACLMessage(ACLMessage.INFORM);
                	donePhase1Msg.setProtocol(Constants.STAGE3);
                	donePhase1Msg.addReceiver(new AID(facilitatorName, AID.ISLOCALNAME));
                	donePhase1Msg.setContent("Ready for execution!");
            		send(donePhase1Msg);
                }
                
            }
        });
		
		addBehaviour(new CyclicBehaviour(this) {
			private static final long serialVersionUID = 1L;

			@Override
			public void action() {
				ACLMessage msg = receive(MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.REQUEST), 
														     MessageTemplate.MatchProtocol(Constants.STAGE3)));
				if (msg != null) {
					if (msg.getContent().equals("EXECUTE!")) {
						String toPrint = "";
						if (willDo.size() > 0) {
							toPrint += "AP " + ((int)myData.getId()+1) + " executes: ";
							for (Task t : willDo) {
								toPrint += t.toString() + " ";
							}
						}
						if (leftOvers.size() > 0) {
							toPrint += "AP " + ((int)myData.getId()+1) + " leftovers: ";
							for (Task t : leftOvers) {
								toPrint += t.toString() + " ";
							}
						}
						if (toPrint != "") {
							System.out.println(toPrint);
						}
						
						sendExecutionResults();
					}
				}
				else {
					block();
				}
			}
		});
	}

	protected void sendExecutionResults() {
		ACLMessage resultsMsg = new ACLMessage(ACLMessage.PROPOSE);
		resultsMsg.setProtocol(Constants.STAGE3);
		resultsMsg.addReceiver(new AID(facilitatorName, AID.ISLOCALNAME));
		ProfitCapsule profitCapsule = new ProfitCapsule(0, leftOvers.size());
		try {
			resultsMsg.setContentObject((Serializable) profitCapsule);
		} catch (IOException e) {}
		send(resultsMsg);
	}

	protected void checkBiddedTaskComplete(Task biddedTask) {
		int expectedOffersNo = 0;
		for (YellowPageCapsule yCap : yellowPagedCapsules) {
			if (biddedTask.getRequiredCapability() == yCap.getTask().getRequiredCapability()) {
				expectedOffersNo = yCap.getCandidates().size();
			}
		}
		Integer biddedTaskId = Integer.valueOf(biddedTask.getTaskId());
		if (offers.containsKey(biddedTaskId) && offers.get(biddedTaskId).size() == expectedOffersNo) {
			List<BidderTuple> bids = offers.get(biddedTaskId);
			BidderTuple winner = Collections.min(bids, new Comparator<BidderTuple>() {
				public int compare(BidderTuple a, BidderTuple b) {
			    	return Integer.valueOf(a.getBid()).compareTo(Integer.valueOf(b.getBid()));
				}
			});
			
			if (winner.getBid() < Constants.INFINIT ) {
				ACLMessage winnerMsg = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				winnerMsg.setProtocol(Constants.STAGE2);
				winnerMsg.addReceiver(new AID(winner.getAgentName(), AID.ISLOCALNAME));
				
				try {
					winnerMsg.setContentObject((Serializable) new TaskCapsule(biddedTask));
				} catch (IOException e) {}
				
				send(winnerMsg);
				
				bids.remove(winner);
				for (BidderTuple loser : bids) {
					if (loser.getBid() < Constants.INFINIT) {
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
				System.out.println("AP " + ((int)myData.getId()+1) + " postpones " + biddedTask.toString());
				if (!cfpDone.entrySet().contains(biddedTaskId)) {
					cfpDone.put(biddedTaskId, new Integer(1));
				}
				checkAllCFPsDone();
			}
			
		}
		
	}

	private boolean checkAllCFPsDone() {
		for (Task cfpedTask : leftOvers) {
			Integer taskId = Integer.valueOf(cfpedTask.getTaskId());
			if (cfpDone.get(taskId) == null) {
				return false;
			}
		}
		
		return true;
	}

	protected void evaluateTaskList() throws MasException {
		
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
			beforeCallForProposals();

	}

	private void askForYellowPages(List<YellowPageCapsule> pagesToCallFor) {
		
		String toPrint = "AP " + ((int)myData.getId()+1) + " asks AF about: ";
		for (YellowPageCapsule yCap : pagesToCallFor) {
			toPrint += yCap.getTask().toString() + " "; 
		}
		System.out.println(toPrint);
		
		ACLMessage yellowMsg = new ACLMessage(ACLMessage.REQUEST);
		yellowMsg.setProtocol(Constants.STAGE1);
		yellowMsg.addReceiver(new AID(facilitatorName, AID.ISLOCALNAME));
		try {
			yellowMsg.setContentObject((Serializable) pagesToCallFor);
		} catch (IOException e) {}
		
		send(yellowMsg);
	}
	
	protected void beforeCallForProposals() {
		List<YellowPageCapsule> pagesToCallFor = new ArrayList<>();
		
		for (Task t : leftOvers) {
			boolean taskToQueryFor = true;
			for (YellowPageCapsule yCap : yellowPagedCapsules) {
				if (t.getRequiredCapability() == yCap.getTask().getRequiredCapability()) {
					taskToQueryFor = false;
					break;
				}
			}
			for (YellowPageCapsule yCap : pagesToCallFor) {
				if (t.getRequiredCapability() == yCap.getTask().getRequiredCapability()) {
					taskToQueryFor = false;
					break;
				}
			}
			if (taskToQueryFor) {
				pagesToCallFor.add(new YellowPageCapsule(t));
			}
		}
		
		if (pagesToCallFor.size() > 0) {
			askForYellowPages(pagesToCallFor);
		}
		else {
			callForProposals();
		}
	}
	
	protected void callForProposals() {		

		for (Task t : leftOvers) {
			for (YellowPageCapsule yCapsule : yellowPagedCapsules) {
				if (t.getRequiredCapability() == yCapsule.getTask().getRequiredCapability()) {
					for (String agentName : yCapsule.getCandidates()) {
						TaskCapsule taskCapsule = new TaskCapsule(t);
						
						ACLMessage taskCapsuleMsg = new ACLMessage(ACLMessage.CFP);
						taskCapsuleMsg.setProtocol(Constants.STAGE2);
						taskCapsuleMsg.addReceiver(new AID(agentName, AID.ISLOCALNAME));
						try {
							taskCapsuleMsg.setContentObject((Serializable) taskCapsule);
						} catch (IOException e) {}
						
						send(taskCapsuleMsg);
					}
					String toPrint = "AP " + ((int)myData.getId()+1) + ": cfp for " + t.toString();
					System.out.println(toPrint);
					break;
				}
			}
		}
	}

}
