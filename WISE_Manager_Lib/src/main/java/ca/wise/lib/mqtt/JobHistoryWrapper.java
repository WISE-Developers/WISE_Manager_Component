package ca.wise.lib.mqtt;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import ca.wise.lib.json.JobHistory;
import ca.wise.lib.json.JobHistory.HistoryStatus;

@JsonInclude(Include.NON_NULL)
public class JobHistoryWrapper {
    
    /**
     * The total number of running jobs.
     */
    @JsonProperty("total_count")
    public int totalCount;
    
    /**
     * The requested offset into the .
     */
    @JsonProperty("offset")
    public Integer offset;
    
    /**
     * The requested maximum number of results.
     */
    @JsonProperty("count")
    public Integer count;
    
    /**
     * An ID that was sent in the request to help
     * clients identify who requested the history.
     */
    @JsonProperty("response_id")
    public String responseId;

    /**
     * A filter that was applied to the returned histories.
     */
    @JsonProperty("filter")
    public HistoryStatus filter;

    /**
     * The list of job histories.
     */
    @JsonProperty("jobs")
    public List<JobHistory> jobs;
}
