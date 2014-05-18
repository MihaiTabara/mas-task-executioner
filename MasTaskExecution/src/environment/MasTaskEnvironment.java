package environment;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
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
		protected int budget;
		protected Map<Integer, Integer> caps = new TreeMap<>();
		
		public AgentData(int id) {
			this.id = id;
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
					if (!currLineScanner.hasNextInt())
						throw new IOException("Wrong input Could not find cost for current capability.");
					int cost = currLineScanner.nextInt();
					newAgent.addCapability(new Integer(cap), new Integer(cost));
				}
				
				agents.add(newAgent);
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
				
				cycles.add(newCycle);
			}

		}
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
