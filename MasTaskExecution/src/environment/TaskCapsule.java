package environment;

import java.io.Serializable;

public class TaskCapsule implements Serializable {
/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Task task;
	int offer = -1;
	
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
