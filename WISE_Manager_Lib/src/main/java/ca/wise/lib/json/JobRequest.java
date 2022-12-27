package ca.wise.lib.json;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import ca.wise.rpc.RPCClient;

@JsonInclude(Include.NON_NULL)
public class JobRequest {
    
    @JsonIgnore
    public static final int VALIDATE_NONE = 0;
    
    @JsonIgnore
    public static final int VALIDATE_CURRENT = 1;
    
    @JsonIgnore
    public static final int VALIDATE_COMPLETE = 2;

	@JsonProperty("job_name")
	public String jobName;
	
	@JsonProperty("use_cores")
	public int cores;
	
	@JsonProperty("file_extension")
	public String extension;
	
	@JsonProperty("file_size")
	public int fileSize;
	
	@JsonProperty("job_priority")
	public int priority = 0;
	
	@JsonProperty("validation_state")
	public int validationState = VALIDATE_NONE;
	
	@JsonIgnore
	public OffsetDateTime recieveTime;
	
	@JsonIgnore
	public Integer requestOffset;
	
	@JsonIgnore
	public ZonedDateTime creationTime;
	
	@JsonIgnore
	public String from;
	
	@JsonIgnore
	public RPCClient client;
	
	@JsonIgnore
	public Path jobFile;
	
	public JobRequest() { }
	
	public JobRequest(JobRequest copy) {
	    this.jobName = copy.jobName;
	    this.cores = copy.cores;
	    this.extension = copy.extension;
	    this.fileSize = copy.fileSize;
	    this.priority = copy.priority;
	    this.recieveTime = copy.recieveTime;
	    this.requestOffset = copy.requestOffset;
	    this.creationTime = copy.creationTime;
	    this.validationState = copy.validationState;
	}
}
