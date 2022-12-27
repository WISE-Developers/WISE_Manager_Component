package ca.wise.geoserver;

import lombok.Getter;

public class HttpException extends Exception {
	private static final long serialVersionUID = 1L;
	@Getter protected int status;
	@Getter protected String text;
	
	protected HttpException(int status, String text) {
	    super(text);
		this.status = status;
		this.text = text;
	}
}
