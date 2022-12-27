package ca.wise.geoserver;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Strings;

public class UrlBuilder {

	private String baseUrl;
	private List<String> path = new ArrayList<>();
	
	public UrlBuilder(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	
	public UrlBuilder append(String path) {
		this.path.add(path);
		return this;
	}
	
	public UrlBuilder scheme(String scheme) {
		int index = baseUrl.indexOf("://");
		if (index >= 0)
			baseUrl = baseUrl.substring(index + 3);
		if (scheme.endsWith("://"))
			baseUrl = scheme + baseUrl;
		else
			baseUrl = scheme + "://" + baseUrl;
		return this;
	}
	
	public UrlBuilder defaultScheme(String scheme) {
		int index = baseUrl.indexOf("://");
		if (index < 0) {
			if (scheme.endsWith("://"))
				baseUrl = scheme + baseUrl;
			else
				baseUrl = scheme + "://" + baseUrl;
		}
		return this;
	}
	
	public String build() {
		StringBuilder builder = new StringBuilder();
		builder.append(baseUrl);
		for (String p : path) {
			if (!Strings.isNullOrEmpty(p)) {
				if (builder.charAt(builder.length() - 1) != '/')
					builder.append("/");
				builder.append(p);
			}
		}
		return builder.toString();
	}
	
	@Override
	public String toString() {
		return build();
	}
}
