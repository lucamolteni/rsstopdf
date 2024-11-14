package io.triode.rsstopdf;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.tinylog.Logger;


public class HttpFetch {
	HttpClient httpClient = HttpClient.newHttpClient();

	public Optional<HttpResponse<String>> getURLFollowingRedirects(String url) {
		return getURLFollowingRedirect( url, HttpResponse.BodyHandlers.ofString() );
	}

	public Optional<HttpResponse<byte[]>> getURLFollowingRedirectsByte(String url) {
		return getURLFollowingRedirect( url, HttpResponse.BodyHandlers.ofByteArray() );
	}

	public <T> Optional<HttpResponse<T>> getURLFollowingRedirect(
			String url,
			HttpResponse.BodyHandler<T> bodyHandler) {
		try {
			HttpRequest request = HttpRequest.newBuilder()
					.uri( URI.create( url ) )
					.GET()
					.build();

			HttpResponse<T> response = httpClient.send( request, bodyHandler );
			if ( response.statusCode() == 301 ) {
				String redirectUrl = response.headers().firstValue( "Location" ).orElseThrow();
				request = HttpRequest.newBuilder()
						.uri( URI.create( redirectUrl ) )
						.GET()
						.build();
				response = httpClient.send( request, bodyHandler );
			}

			return Optional.of( response );
		}
		catch (IOException | InterruptedException | IllegalArgumentException e) {
			Logger.info( "Failed to GET URL: " + url );
			return Optional.empty();
		}
	}
}
