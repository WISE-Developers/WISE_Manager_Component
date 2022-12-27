package ca.wise.lib.json;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Describes the files that have been written by a specified job.
 */
@JsonInclude(Include.NON_NULL)
public class FileList {

    /**
     * The name of the job that output the files.
     */
    @JsonProperty("job_name")
    public String jobName;
    
    /**
     * The names of the files that have been written.
     */
    @JsonProperty("files")
    public List<String> files = new ArrayList<>();
    
    public FileList(String jobName) {
        this.jobName = jobName;
    }
}
