package environment;

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

	/**
	 * @author mtabara
	 * Class describing the state of a Cycle as it comes from the input
	 */
	public static class CycleData {
		/**
		 * The cycle id
		 */
		protected int cycleId;
		
		/**
		 * The list of tasks to be executed in the current cycle
		 */
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
	
	/**
	 * @author mtabara
	 * The class represeting the information about an agent
	 */
	public static class AgentData {
		/**
		 * The agent id - unique across environment
		 */
		protected int id;
		
		/**
		 * The name of the agent
		 */
		protected String name;
		
		/**
		 * The budget of the agent as it is being read from the input
		 */
		protected int budget;
		
		/**
		 * The mapping of pairs (capability, cost) read from the input
		 */
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
		
		public boolean hasCapability(Integer capToTest) {
			if (caps.get(capToTest) == null)
				return false;
			return true;
		}

		public Map<Integer, Integer> getCaps() {
			return caps;
		}
		
		public Set<Integer> getCapabilities() {
			return caps.keySet();
		}

		@Override
		public String toString() {
			String ret = "";
			ret += "AP " + ((int)getId()+1) + ": budget " + getBudget() + "; capabilities ";
			for (Integer cap : caps.keySet()) {
				ret += cap + " (cost " + caps.get(cap) + "); ";
			}
			return ret;
		}
		
		
	}
	
	/**
	 * The cycles of the system as they are being read from the input
	 */
	protected List<CycleData> cycles = new ArrayList<>();
	
	/**
	 * All the agents in the environment as they're being read from the input
	 */
	protected List<AgentData> agents = new ArrayList<>();
	
	/**
	 * Number of agents in the environment
	 */
	protected int numberOfAgents;
	
	/**
	 * Number of cycles in the environment
	 */
	protected int numberOfCycles;
	
	/**
	 * The leftover penalty to take into account when computing the overall
	 * profit in the environment
	 */
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

	public List<AgentData> getAgents() {
		return agents;
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
