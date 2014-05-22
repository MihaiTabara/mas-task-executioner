/**
 * 
 */
package environment;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mtabara
 *
 */
public class YellowPageCapsule implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Task task;
	Set<String> candidates = new HashSet<>();
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
