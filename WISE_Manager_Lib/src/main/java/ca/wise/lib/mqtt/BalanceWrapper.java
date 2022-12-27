package ca.wise.lib.mqtt;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import ca.wise.lib.json.JobRequest;

/**
 * Wrapper around information used to manage
 * transferring a job file between instances
 * of W.I.S.E. Manager for load balancing.
 */
public class BalanceWrapper {

    public final JobRequest request;
    
    public final List<JobRequest> clients = new ArrayList<>();
    
    /**
     * Create a new balance wrapper.
     * @param request The job request that needs to be balanced. Stores a copy of the request.
     */
    public BalanceWrapper(JobRequest request) {
        this.request = new JobRequest(request);
        this.request.creationTime = ZonedDateTime.now();
    }
}
