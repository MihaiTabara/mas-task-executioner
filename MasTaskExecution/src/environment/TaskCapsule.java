/**
 * 
 */
package environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author mtabara
 *
 */
public class TaskCapsule implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Task task;
	Set<String> candidates = new HashSet<>();
	
	public TaskCapsule(Task t) {
		this.task = t;
	}

	public Task getTask() {
		return task;
	}
	
	public void addCandidate(String candidate) {
		candidates.add(candidate);
	}

	@Override
	public String toString() {
		String ret = "";
		ret += task.toString();
		ret += " can be done by: ";
		for (String candidate : candidates)
			ret += candidate;
		
		return ret;
	}
}
