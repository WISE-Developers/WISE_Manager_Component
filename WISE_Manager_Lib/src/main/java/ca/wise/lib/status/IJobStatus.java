package ca.wise.lib.status;

public interface IJobStatus {
	
	public void forceReload();

	public void updateStatus(Status newStatus);
	
	public void updateStatus(Status newStatus, String data);
	
	public void writeMessage(String message);
	
	public Status getCurrentStatus();
	
	public JobCompletionStatus getJobStatus();
	
	public static enum JobCompletionStatus {
		NONE,
		SUCCEEDED,
		FAILED
	}
}
