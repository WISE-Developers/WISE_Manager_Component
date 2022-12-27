package ca.wise.geoserver;

import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;

import ca.wise.geoserver.proto.ListWorkspace;
import ca.wise.geoserver.proto.ListWorkspace.WorkspaceList;
import ca.wise.geoserver.proto.NewWorkspace;
import ca.wise.geoserver.proto.WorkspaceDetails;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import lombok.Builder;

public class Workspaces extends GeoServerBase {

	@Builder	
	public Workspaces(String baseUrl, String username, String password) {
		super(baseUrl, username, password);
	}
	
	/**
	 * List all available workspaces on the geoserver.
	 * @return A list of workspaces.
	 * @throws HttpException If an error occurred while attempting
	 * to get the list of workspaces.
	 */
	public WorkspaceList getWorkspaces() throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.build();
		GetRequest request = Unirest.get(url)
			.accept("application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<ListWorkspace> response = request
				.asObject(ListWorkspace.class);
		
		if (response.getStatus() == 200)
			return response.getBody().getWorkspaces();
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Get the details of a workspace.
	 * @param workspace The name of the workspace to get.
	 * @return The workspace details if the workspace existed, null if it doesn't exist.
	 * @throws HttpException If an error occurred retrieving the workspace.
	 */
	public WorkspaceDetails.Workspace getWorkspace(String workspace) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspace)
				.build();
		GetRequest request = Unirest.get(url)
				.accept("application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<WorkspaceDetails> response = request
				.asObject(WorkspaceDetails.class);
		
		if (response.getStatus() == 200)
			return response.getBody().getWorkspace();
		else if (response.getStatus() == 404)
			return null;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Create a new workspace.
	 * @param workspace The name of the workspace to create.
	 * @return True if a new workspace was created, false if the workspace alredy exists.
	 * @throws HttpException If an error occurred while attempting to add the workspace,
	 * whether the workspace was created or not is unknown.
	 */
	public boolean createWorkspace(String workspace) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.build();
		
		NewWorkspace.Builder body = NewWorkspace.newBuilder();
		body.getWorkspaceBuilder()
				.setName(workspace);
		
		HttpRequestWithBody request = Unirest.post(url)
				.accept("application/json")
				.header("Content-Type", "application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<String> response = request
				.body(body)
				.asString();
		
		if (response.getStatus() == 200 || response.getStatus() == 201)
			return true;
		else if (response.getStatus() == 401 && response.getStatusText().contains("already exists"))
			return false;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Delete a workspace. Leaves the contents of the workspace intact.
	 * @param workspace The name of the workspace to delete.
	 * @return True if the workspace was deleted, false if the workspace
	 * wasn't able to be deleted (ex. wasn't empty, doesn't exist, is default).
	 * @throws HttpException If an error occurred while trying to delete
	 * the workspace, whether the workspace was deleted or not is unknown.
	 */
	public boolean deleteWorkspace(String workspace) throws HttpException {
		return deleteWorkspace(workspace, false);
	}

	/**
	 * Delete a workspace. Leaves the contents of the workspace intact.
	 * @param workspace The name of the workspace to delete.
	 * @param recurse Delete the contents of the workspace as well.
	 * @return True if the workspace was deleted, false if the workspace
	 * wasn't able to be deleted (ex. wasn't empty, doesn't exist, is default).
	 * @throws HttpException If an error occurred while trying to delete
	 * the workspace, whether the workspace was deleted or not is unknown.
	 */
	public boolean deleteWorkspace(String workspace, boolean recurse) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspace)
				.build();
		
		HttpRequestWithBody request = Unirest.delete(url)
				.queryString("recurse", recurse)
				.accept("application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<String> response = request.asString();
		
		if (response.getStatus() == 200)
			return true;
		else if (response.getStatus() == 403 || response.getStatus() == 404 || response.getStatus() == 405)
			return false;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Update an existing workspace.
	 * @param workspace The name of the workspace to update.
	 * @param enabled Is the workspace enabled or not.
	 * @return True if a new workspace was update.
	 * @throws HttpException If an error occurred while attempting to update the workspace,
	 * whether the workspace was updated or not is unknown.
	 */
	public boolean updateWorkspace(String workspace, boolean enabled) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspace)
				.build();
		
		NewWorkspace.Builder body = NewWorkspace.newBuilder();
		body.getWorkspaceBuilder()
				.setName(workspace)
				.setEnabled(BoolValue.newBuilder().setValue(enabled));
		
		HttpRequestWithBody request = Unirest.put(url)
				.accept("application/json")
				.header("Content-Type", "application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<String> response = request
				.body(body)
				.asString();
		
		if (response.getStatus() == 200)
			return true;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Get a client object for querying the coverage stores within the
	 * requested workspace.
	 * @param workspace The workspace to get the coverage stores client for.
	 * @return A coverage stores client for the requested workspace.
	 */
	public CoverageStores getCoverageStoresClient(String workspace) {
		return CoverageStores.builder()
				.baseUrl(baseUrl)
				.username(username)
				.password(password)
				.workspaceName(workspace)
				.build();
	}
}
