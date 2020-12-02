/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.familylegacy.ui.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;


public final class FileHelper{

	private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);

	private static final String URL_PROTOCOL_HTTP = "http://";
	private static final String TEST_CONNECTIVITY_URL = "https://www.google.com/";


	private FileHelper(){}

	public static boolean browseURL(final String url){
		return executeDesktopCommand(Desktop.Action.BROWSE, url);
	}

	private static boolean executeDesktopCommand(final Desktop.Action action, final Object parameter){
		boolean done = false;
		final Desktop desktop = getDesktopFor(action);
		try{
			switch(action){
				case OPEN:
					desktop.open((File)parameter);
					done = true;
					break;

				case BROWSE:
					if(hasInternetConnectivity()){
						desktop.browse(new URI((String)parameter));
						done = true;
					}
					break;

				case MAIL:
					if(hasInternetConnectivity()){
						desktop.mail(new URI((String)parameter));
						done = true;
					}
			}
		}
		catch(final Exception e){
			LOGGER.error("Cannot execute {} command", action, e);
		}
		return done;
	}

	public static boolean hasInternetConnectivity(){
		return testURL(TEST_CONNECTIVITY_URL);
	}

	public static boolean testURL(final String url){
		try{
			HttpURLConnection connection = (HttpURLConnection)new URL(url.startsWith(URL_PROTOCOL_HTTP)? url: URL_PROTOCOL_HTTP + url)
				.openConnection();
			connection.setRequestMethod("HEAD");
			int responseCode = connection.getResponseCode();

			if((responseCode != HttpURLConnection.HTTP_OK)){
				//try with a GET, as some legacy server would not handle a HEAD
				connection = (HttpURLConnection)new URL(url).openConnection();
				connection.setRequestMethod("GET");
				responseCode = connection.getResponseCode();
			}

			return (responseCode == HttpURLConnection.HTTP_OK);
		}
		catch(final Exception ignored){
			return false;
		}
	}

	private static Desktop getDesktopFor(final Desktop.Action action){
		return (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)? Desktop.getDesktop(): null);
	}

}
