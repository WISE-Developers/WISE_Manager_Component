package ca.wise.lib.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Shutdown {

	@JsonProperty("priority")
	public int priority;
	
	@JsonProperty("timeout")
	public Integer timeout;

    @JsonProperty("job_name")
	public String jobId;
}
