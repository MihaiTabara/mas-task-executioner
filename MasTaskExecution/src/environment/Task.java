package environment;

import java.io.Serializable;

/**
 * @author mtabara
 * The structure to define a task in the system.
 * Each task receives an unique number and has a corresponding 
 * required capability to solve the task
 */
public class Task implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Internal value used to generate the tasks id
	 */
	private static int counter = 0;
	
	/**
	 * The id of a task, unique across the system
	 */
	private int taskId;
	
	/**
	 * The required capability the task needs to be solved 
	 */
	private int requiredCapability;
	
	public Task(int cap) {
		this.setRequiredCapability(cap);
		this.setTaskId(++counter);
	}

	public int getRequiredCapability() {
		return requiredCapability;
	}

	public void setRequiredCapability(int requiredCapability) {
		this.requiredCapability = requiredCapability;
	}

	public int getTaskId() {
		return taskId;
	}

	public void setTaskId(int taskId) {
		this.taskId = taskId;
	}

	@Override
	public String toString() {
		return "task " + taskId + " (cap " + requiredCapability + ");";
	}
}
