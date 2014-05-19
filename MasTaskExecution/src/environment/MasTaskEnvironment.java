package environment;

import jade.core.Agent;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;


/**
 * @author mtabara
 * Mas Task Environment to gather data from input files
 */
public class MasTaskEnvironment {

	public static class CycleData {
		protected int cycleId;
		protected List<Task> tasks = new ArrayList<>();
		
		public CycleData(int cycleId) {
			this.cycleId = cycleId;
		}

		public List<Task> getTasks() {
			return tasks;
		}
		
		public Task getTask(int index) {
			return tasks.get(index);
		}

		@Override
		public String toString() {
			String ret = "";
			ret += "CycleData [cycleId=" + cycleId + ", tasks=";
			ret += "\n";
			for (Task t : tasks) {
				ret += t.toString() + "\n";
			}
			return ret;
		}
	}
	
	public static class AgentData {
		protected int id;
		protected String name;
		protected int budget;
		protected Map<Integer, Integer> caps = new TreeMap<>();
		
		public AgentData(int id) {
			this.id = id;
			setName(new String("agent" + id));
		}

		public Integer getBudget() {
			return budget;
		}

		public void setBudget(int budget) {
			this.budget = budget;
		}

		public Integer getId() {
			return id;
		}
		
		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
		
		public boolean equals(String name) {
			return this.name.equals(name);
		}

		public void addCapability(Integer capability, Integer cost) {
			caps.put(capability, cost);
		}

		public Map<Integer, Integer> getCaps() {
			return caps;
		}
		
		public Set<Integer> getCapabilities() {
			return caps.keySet();
		}

		@Override
		public String toString() {
			return "AgentData [id=" + id + ", budget=" + budget + ", caps="
					+ caps + "]";
		}
		
		
	}
	
	protected List<CycleData> cycles = new ArrayList<>();
	protected List<AgentData> agents = new ArrayList<>();
	protected int numberOfAgents;
	protected int numberOfCycles;
	protected float LeftoverPenalty;
	
	public MasTaskEnvironment(InputStream input) throws IOException {
		try (Scanner scan = new Scanner(input))
		{
			// read first line
			Scanner currLineScanner = new Scanner(scan.nextLine());
			numberOfAgents = currLineScanner.nextInt();
			numberOfCycles = currLineScanner.nextInt();
			LeftoverPenalty = currLineScanner.nextFloat();
			currLineScanner.close();
			
			// consume empty line
			String dummyLine = scan.nextLine().trim();
			if (dummyLine.length() != 0 )
				throw new IOException("Something went wrong in reading the input file! Was expecting empty line ...");
			
			// consume a line for each agents's input data
			for (int i = 0; i < numberOfAgents; i++) {
				currLineScanner = new Scanner(scan.nextLine());
				int budget = currLineScanner.nextInt();
				
				AgentData newAgent = new AgentData(i);
				newAgent.setBudget(budget);
				
				while (currLineScanner.hasNextInt()) {
					int cap = currLineScanner.nextInt();
					if (!currLineScanner.hasNextInt()) {
						currLineScanner.close();
						throw new IOException("Wrong input Could not find cost for current capability.");
					}
					int cost = currLineScanner.nextInt();
					newAgent.addCapability(new Integer(cap), new Integer(cost));
				}
				
				this.addAgent(newAgent);
				currLineScanner.close();
			}
			
			// consume another empty line
			dummyLine = scan.nextLine().trim();
			if (dummyLine.length() != 0 )
				throw new IOException("Something went wrong in reading the input file! Was expecting empty line ...");
			
			// consume a line for each cycle's input data
			for (int i = 0; i < numberOfCycles; i++) {
				currLineScanner = new Scanner(scan.nextLine());
				
				CycleData newCycle = new CycleData(i);
				while (currLineScanner.hasNextInt()) {
					int cap = currLineScanner.nextInt();
					newCycle.tasks.add(new Task(cap));
				}
				
				this.addCycle(newCycle);
				currLineScanner.close();
			}

		}
	}

	public int getNumberOfAgents() {
		return numberOfAgents;
	}
	
	public AgentData getAgentByName(String nameToSearch) {
		for (AgentData agent : agents) {
			if (agent.equals(nameToSearch)) {
				return agent;
			}
		}
		return null;
	}

	public void setNumberOfAgents(int numberOfAgents) {
		this.numberOfAgents = numberOfAgents;
	}

	public int getNumberOfCycles() {
		return numberOfCycles;
	}

	public void setNumberOfCycles(int numberOfCycles) {
		this.numberOfCycles = numberOfCycles;
	}

	public float getLeftoverPenalty() {
		return LeftoverPenalty;
	}

	public AgentData getAgent(int agentIndex) {
		return agents.get(agentIndex);
	}
	
	public void addAgent(AgentData agent) {
		this.agents.add(agent);
	}
	
	public CycleData getCycle(int cycleIndex) {
		return cycles.get(cycleIndex);
	}
	
	public void addCycle(CycleData cycle) {
		
		this.cycles.add(cycle);
	}
	
	@Override
	public String toString() {
		String ret = "MasTaskEnvironment looks like:\n";
		ret += "Number of agents " + numberOfAgents + "\n";
		ret += "Number of cycles " + numberOfCycles + "\n";
		ret += "Leftover penalty is " + LeftoverPenalty + "\n";
		
		for (int i = 0; i < numberOfAgents; i++) {
			ret += agents.get(i).toString() + "\n";
		}
		
		for (int i = 0; i < numberOfCycles; i++) {
			ret += cycles.get(i).toString() + "\n";
		}
	
		return ret;
	}

}
