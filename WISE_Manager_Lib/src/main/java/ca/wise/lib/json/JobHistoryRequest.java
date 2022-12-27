package ca.wise.lib.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import ca.wise.lib.json.JobHistory.HistoryStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(Include.NON_NULL)
public class JobHistoryRequest {

    /**
     * A filter to apply to returned job histories.
     */
	@JsonProperty("filter")
	public HistoryStatus filter;
	
	/**
	 * Skip some jobs.
	 */
	@JsonProperty("offset")
	public int offset = -1;
	
	/**
	 * Only return a specific number of jobs.
	 */
	@JsonProperty("count")
	public int count = -1;
	
	/**
	 * An ID to return with the response to help
	 * clients identify who requested the history.
	 */
	@JsonProperty("response_id")
	public String responseId;
}
