package environment;

public class Task {
	private static int counter = 0;
	private int taskId;
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
		return "Task [taskId=" + taskId + ", requiredCapability="
				+ requiredCapability + "]";
	}
}
