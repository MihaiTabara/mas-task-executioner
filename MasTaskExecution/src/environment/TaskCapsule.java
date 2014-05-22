package environment;

import java.io.Serializable;

import agents.Constants;

/**
 * @author mtabara
 * The structure that encapsulates a task and a potential offer 
 * as they move between the agents
 */
public class TaskCapsule implements Serializable {

	private static final long serialVersionUID = 1L;
	
	/**
	 * The task is negotiating for
	 */
	private Task task;
	
	/**
	 * The offer that has been made for the current task.
	 * It defaults to INFINIT 
	 */
	int offer = Constants.INFINIT;
	
	public TaskCapsule(Task task) {
		this.task = task;
	}
	
	public Task getTask() {
		return task;
	}

	public int getOffer() {
		return offer;
	}

	public void setOffer(int offer) {
		this.offer = offer;
	}
	

}
