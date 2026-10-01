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
package io.github.mtrevisan.familylegacy.v2;

import java.io.InputStream;
import java.util.Properties;


public final class ProjectInfo{

	private static final String APP_NAME;
	private static final String APP_VERSION;
	static{
		String name = "unknown";
		String version = "unknown";
		try(final InputStream is = ProjectInfo.class.getResourceAsStream("/version.properties")){
			if(is != null){
				final Properties props = new Properties();
				props.load(is);
				name = props.getProperty("app.name", name);
				version = props.getProperty("app.version", version);
			}
		}
		catch(final Exception ignored){}

		APP_NAME = name;
		APP_VERSION = version;
	}


	public static String getAppName(){
		return APP_NAME;
	}

	public static String getAppVersion(){
		return APP_VERSION;
	}

}
