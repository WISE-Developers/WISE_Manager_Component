package ca.wise.lib.json;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(Include.NON_NULL)
public class Checkin {

	@JsonProperty("node_id")
	public String nodeId;
	
	@JsonProperty("version")
	public String version;
	
	@JsonProperty("status")
	public Status status;
	
	@JsonProperty("node_type")
	public Type type;
	
	@JsonProperty("topic_string")
	public String topic;
	
	@JsonProperty("manager_details")
	public ManagerDetails managerDetails;
	
	@JsonProperty("computer_details")
	public ComputerDetails computerDetails;
	
	public enum Status {
		Unknown(-1),
		Running(0),
		StartingUp(1),
		ShuttingDown(2),
		ConnectionLoss(3)
		;
		
		private final int value;
		
		Status(int value) {
			this.value = value;
		}
		
		@JsonCreator
		public static Status forValue(final int value) {
			return Arrays.stream(values())
					.filter(x -> x.value == value)
					.findFirst()
					.orElse(Status.Unknown);
		}
		
		@JsonValue
		public int toValue() {
			return value;
		}
	}
	
	public enum Type {
		Unknown(-1),
		Builder(0),
		Manager(1)
		;
		
		private final int value;
		
		Type(int value) {
			this.value = value;
		}
		
		@JsonCreator
		public static Type forValue(final int value) {
			return Arrays.stream(values())
					.filter(x -> x.value == value)
					.findFirst()
					.orElse(Type.Unknown);
		}
		
		@JsonValue
		public int toValue() {
			return value;
		}
	}
	
	@JsonInclude(Include.NON_NULL)
	public static class ManagerDetails {
	    
	    /**
	     * A list of the names of currently running jobs (not queued or complete).
	     */
	    @JsonProperty("running_jobs")
	    public List<String> runningJobs;
	    
	    /**
	     * The current memory usage of PSaaS Manager (in bytes).
	     */
	    @JsonProperty("memory_usage")
	    public Long memoryUsage;
	    
	    /**
	     * The current CPU usage of PSaaS Manager (in percent).
	     */
	    @JsonProperty("cpu_usage")
	    public Double cpuUsage;
	    
	    /**
	     * Is the job queue paused.
	     * 0 -> Paused,
	     * 1 -> Unpaused,
	     * 2 -> Offline
	     */
	    @JsonProperty("is_paused")
	    public int isPaused = 1;
	}
	
	@JsonInclude(Include.NON_NULL)
	public static class ComputerDetails {
	    
	    @JsonProperty("physical_cores")
	    public Integer physicalCores;
	    
	    @JsonProperty("logical_cores")
	    public Integer logicalCores;
	    
	    @JsonProperty("total_memory")
	    public Long totalMemory;
	    
	    @JsonProperty("total_space")
	    public Long totalSpace;
	    
	    @JsonProperty("used_space")
	    public Long usedSpace;
	    
	    @JsonProperty("start_time")
	    public String startTime;
	    
	    @JsonProperty("internal_ip_address")
	    public String internalIpAddress;
	    
	    @JsonProperty("external_ip_address")
	    public String externalIpAddress;
	    
	    @JsonProperty("operating_system")
	    public String operatingSystem;
        
        @JsonProperty("machine_name")
        public String machineName;
        
        @JsonProperty("job_folder")
        public String jobFolder;
	}
}
