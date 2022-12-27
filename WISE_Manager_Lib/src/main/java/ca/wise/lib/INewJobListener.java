package ca.wise.lib;

@FunctionalInterface
public interface INewJobListener {
	
	void onNewJob(JobStartDetails details);
}
