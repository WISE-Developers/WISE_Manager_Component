package ca.wise.lib.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class Manage {

	@JsonProperty("request")
	public String request;
	
	@JsonProperty("target")
	public String target;
	
	@JsonProperty("delete_old")
	public Boolean deleteOld;
}
