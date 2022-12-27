package ca.wise.lib;

public class JobStartDetails {
    public final String jobName;
    public final int cores;
    public final int priority;
    public final int validationState;
    
    public JobStartDetails(String jobName, int cores, int priority, int validationState) {
        this.jobName = jobName;
        this.cores = cores;
        this.priority = priority;
        this.validationState = validationState;
    }
}
