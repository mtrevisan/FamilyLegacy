/**
 * Copyright (c) 2026 Mauro Trevisan
 * <p>
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 * <p>
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;


/** How the bytes behind an address are read: the network in the application, a canned answer in the tests. */
@FunctionalInterface
public interface Transport{

	/**
	 * @param uri	The address to read.
	 * @return	The content; the caller closes it.
	 * @throws HttpStatusException	If the server answers with something other than success.
	 * @throws IOException	If the content cannot be read.
	 */
	InputStream open(URI uri) throws IOException;


	/** @return	The transport that reads from the network (HTTPS, following the redirects the GitHub downloads use). */
	static Transport http(){
		final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(15))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.build();
		return uri -> {
			final HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofSeconds(30))
				.header("Accept", "application/vnd.github+json, application/octet-stream")
				.header("User-Agent", "FamilyLegacy-Updater")
				.GET()
				.build();
			try{
				final HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
				if(response.statusCode() != 200){
					response.body().close();
					throw new HttpStatusException(response.statusCode(), uri);
				}
				return response.body();
			}
			catch(final InterruptedException e){
				Thread.currentThread().interrupt();
				throw new InterruptedIOException("Interrupted while reading " + uri);
			}
		};
	}


	/** The server answered, but not with the content. */
	final class HttpStatusException extends IOException{

		private static final long serialVersionUID = 1L;

		private final int status;


		public HttpStatusException(final int status, final URI uri){
			super("HTTP " + status + " from " + uri);

			this.status = status;
		}

		public int status(){
			return status;
		}

	}

}
