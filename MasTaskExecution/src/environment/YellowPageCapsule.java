/**
 * 
 */
package environment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import agents.FacilitatorAgent;

/**
 * @author mtabara
 * The structure that defines the capsule that is exchanged by the agent 
 * with the {@link FacilitatorAgent} in the attempt of getting more info
 * to achieve a specific task.
 */
public class YellowPageCapsule implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The indicated task for whom capacity is asking for
	 */
	private Task task;
	
	/**
	 * A set of strings containing the names of the agents 
	 * that can be issued out with a CFP to solve this task
	 */
	Set<String> candidates = new HashSet<>();
	
	/**
	 * A set of integers containing the names of the agents 
	 * that can be issued out with a CFP to solve this task
	 */
	Set<Integer> intCandidates = new HashSet<>();
	
	public YellowPageCapsule(Task t) {
		this.task = t;
	}

	public Task getTask() {
		return task;
	}
	
	public void addCandidate(String candidate, Integer intCandidate) {
		candidates.add(candidate);
		intCandidates.add(intCandidate);
	}

	public Set<String> getCandidates() {
		return candidates;
	}

	@Override
	public String toString() {
		String ret = "";
		ret += task.toString();
		ret += " can be done by agents: ";
		for (Integer candidate : intCandidates)
			ret += ((int)candidate+1) + ",";
		
		return ret;
	}

}
