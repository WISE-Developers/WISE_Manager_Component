package ca.wise.geoserver;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.google.common.base.Strings;
import com.google.protobuf.BoolValue;

import ca.wise.geoserver.proto.CoverageStoreDetails;
import ca.wise.geoserver.proto.CoverageStoreDetails.CoverageStore;
import ca.wise.geoserver.proto.CoverageWrapper;
import ca.wise.geoserver.proto.ListCoverageStore;
import ca.wise.geoserver.proto.ListCoverageStore.CoverageStoreList;
import ca.wise.geoserver.proto.NewCoverageStore;
import kong.unirest.GetRequest;
import kong.unirest.HttpRequestWithBody;
import kong.unirest.HttpResponse;
import kong.unirest.RequestBodyEntity;
import kong.unirest.Unirest;
import lombok.Builder;
import lombok.Getter;

public class CoverageStores extends GeoServerBase {

	@Getter private String workspaceName;
	
	@Builder	
	public CoverageStores(String baseUrl, String username, String password, String workspaceName) {
		super(baseUrl, username, password);
		this.workspaceName = workspaceName;
	}
	
	/**
	 * Get a list of coverage stores within a workspace.
	 * @return The list of coverage stores.
	 * @throws HttpException If an error occurred while getting the coverage stores.
	 */
	public CoverageStoreList getCoverageStores() throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.build();
		GetRequest request = Unirest.get(url)
				.accept("application/json");
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<ListCoverageStore> response = request
				.asObject(ListCoverageStore.class);
		
		if (response.getStatus() == 200) {
			ListCoverageStore store = response.getBody();
			//the coverageStores object is a string when there are no coverages so it can't be parsed
			if (store != null && store.hasCoverageStores())
				return store.getCoverageStores();
			else
				return CoverageStoreList.newBuilder()
						.build();
		}
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Create a new coverage store within the current workspace.
	 * @param name The name of the new coverage store.
	 * @return True if a new coverage store was created, false if it already exists.
	 * @throws HttpException If an error occurred while creating the new coverage store.
	 * Whether the coverage store was created or not is unknown.
	 */
	public boolean createCoverageStore(String name) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.build();
		
		NewCoverageStore.Builder body = NewCoverageStore.newBuilder();
		body.getCoverageStoreBuilder()
			.setName(name)
			.setWorkspace(workspaceName);
		
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
	 * Get a coverage store from the current workspace.
	 * @param name The name of the coverage store to get.
	 * @return The coverage store if it exists, null if it doesn't.
	 * @throws HttpException If an error occurred while getting the coverage store.
	 */
	public CoverageStore getCoverageStore(String name) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.append(name)
				.build();
		
		GetRequest request = Unirest.get(url)
				.accept("application/json")
				.queryString("quietOnNotFound", true);
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<CoverageStoreDetails> response = request
				.asObject(CoverageStoreDetails.class);
		
		if (response.getStatus() == 200)
			return response.getBody().getCoverageStore();
		else if (response.getStatus() == 404)
			return null;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
	
	/**
	 * Delete a coverage store from the current workspace.
	 * @param name The name of the coverage store to delete.
	 * @return True if the coverage store was deleted, false if it wasn't.
	 * @throws HttpException If an error occurred while deleting the coverage store.
	 * Whether the coverage store was deleted or not is unknown.
	 */
	public boolean deleteCoverageStore(String name) throws HttpException {
		return deleteCoverageStore(name, false);
	}

	/**
	 * Delete a coverage store from the current workspace.
	 * @param name The name of the coverage store to delete.
	 * @param recurse True to delete all data within the coverage store, false to fail
	 * if there is data within the coverage store.
	 * @return True if the coverage store was deleted, false if it wasn't.
	 * @throws HttpException If an error occurred while deleting the coverage store.
	 * Whether the coverage store was deleted or not is unknown.
	 */
	public boolean deleteCoverageStore(String name, boolean recurse) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.append(name)
				.build();
		
		HttpRequestWithBody request = Unirest.delete(url)
				.accept("application/json");
		if (recurse) {
			request
				.queryString("recurse", true)
				.queryString("purge", "metadata");
		}
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
	 * Upload a grid file to a coverage store.
	 * @param store The name of the coverage store in the current workspace.
	 * @param file The file to upload.
	 * @return The name of the new coverage if one was created.
	 * @throws HttpException An error that occurred while uploading the file.
	 * Whether the file was uploaded is unknown.
	 * @throws IOException If an error occurred while reading the file to upload.
	 */
	public String uploadFileToCoverageStore(String store, Path file) throws HttpException, IOException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.append(store)
				.append("file.geotiff")
				.build();

		String filename = com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString());
		RequestBodyEntity request = Unirest.put(url)
				.accept("application/json")
				.queryString("filename", file.getFileName().toString())
				.queryString("coverageName", filename)
				.body(Files.readAllBytes(file))
				.header("Content-Type", "image/tiff");
		
		if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
			request.basicAuth(username, password);
		HttpResponse<String> response = request.asString();
		
		if (response.getStatus() == 200 || response.getStatus() == 201)
			return filename;
		else
			throw new HttpException(response.getStatus(), response.getStatusText());
	}
    
    /**
     * Upload a vector file to a data store.
     * @param store The name of the coverage store in the current workspace.
     * @param file The file to upload.
     * @return The name of the new coverage if one was created.
     * @throws HttpException An error that occurred while uploading the file.
     * Whether the file was uploaded is unknown.
     * @throws IOException If an error occurred while reading the file to upload.
     */
    public String uploadFileToDataStore(String store, Path file) throws HttpException, IOException {
        String url = new UrlBuilder(baseUrl)
                .defaultScheme("http")
                .append("rest")
                .append("workspaces")
                .append(workspaceName)
                .append("datastores")
                .append(store)
                .append("file.shp")
                .build();
        
        Path parent = file.getParent();
        String filename = com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString());
        List<Path> shapefiles;
        //get a list of all secondary parts of the shapefile
        try (Stream<Path> stream = Files.walk(parent)) {
            shapefiles = stream
                    .filter(x -> com.google.common.io.Files.getNameWithoutExtension(x.getFileName().toString()).equals(filename))
                    .collect(Collectors.toList());
        }
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        for (Path path : shapefiles) {
            try (FileInputStream fis = new FileInputStream(path.toFile())) {
                ZipEntry entry = new ZipEntry(path.getFileName().toString());
                zos.putNextEntry(entry);
                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }
            }
        }
        zos.close();
        
        RequestBodyEntity request = Unirest.put(url)
                .accept("application/json")
                .queryString("filename", filename + ".zip")
                .queryString("configure", "all")
                .body(baos.toByteArray())
                .header("Content-Type", "application/zip");
        
        if (!Strings.isNullOrEmpty(username) && !Strings.isNullOrEmpty(password))
            request.basicAuth(username, password);
        HttpResponse<String> response = request.asString();
        
        if (response.getStatus() == 200 || response.getStatus() == 201) {
            return filename;
        }
        else {
            throw new HttpException(response.getStatus(), response.getStatusText());
        }
    }
	
	/**
	 * Get a coverage for the current workspace.
	 * @param coverage The name of the coverage to get.
	 * @return A coverage, or null if the coverage was not available.
	 * @throws HttpException If an error was thrown while getting the coverage.
	 */
	public boolean updateCoverage(String store, String coverage, boolean enabled, String srs) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.append(store)
				.append("coverages")
				.append(coverage)
				.build();
		
		CoverageWrapper.Builder body = CoverageWrapper.newBuilder();
		body.getCoverageBuilder()
			.setName(coverage)
			.setSrs(srs)
			.setEnabled(enabled);
		
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
	 * Update a coverage store in the current workspace.
	 * @param store The name of the coverage store to update.
	 * @param enabled Is the coverage store enabled.
	 * @return True if the coverage store was successfully updated.
	 * @throws HttpException If an error occurred while udpating the coverage store.
	 * Whether the coverage store was updated or not is unknown.
	 */
	public boolean updateCoverageStore(String store, boolean enabled) throws HttpException {
		String url = new UrlBuilder(baseUrl)
				.defaultScheme("http")
				.append("rest")
				.append("workspaces")
				.append(workspaceName)
				.append("coveragestores")
				.append(store + ".json")
				.build();
		
		NewCoverageStore.Builder body = NewCoverageStore.newBuilder();
		body.getCoverageStoreBuilder()
			.setName(store)
			.setWorkspace(workspaceName)
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
}
