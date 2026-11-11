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
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;


/**
 * Knows which installers of a release run on this system, and starts one.
 * <p>
 * Only Windows is supported (the releases carry the {@code .exe} built by jpackage, or an {@code .msi}); elsewhere the application
 * sends the user to the page of the release.
 * </p>
 */
public final class InstallerLauncher{

	private InstallerLauncher(){}


	/** @return	Whether an installer can be run by the application on this system. */
	public static boolean isSupportedPlatform(){
		return isWindows(System.getProperty("os.name", ""));
	}

	static boolean isWindows(final String osName){
		return osName.toLowerCase(Locale.ROOT).startsWith("windows");
	}

	/**
	 * @param fileName	The name of an asset of a release.
	 * @return	How much the asset is preferred as the installer for this system (0 is the best), or -1 if it is not one.
	 */
	public static int installerRank(final String fileName){
		return installerRank(fileName, isSupportedPlatform());
	}

	static int installerRank(final String fileName, final boolean windows){
		if(!windows)
			return -1;

		final String lower = fileName.toLowerCase(Locale.ROOT);
		if(lower.endsWith(".exe"))
			return 0;
		if(lower.endsWith(".msi"))
			return 1;
		return -1;
	}

	/**
	 * @param installer	The downloaded installer.
	 * @return	The command line that runs it.
	 */
	static List<String> command(final Path installer){
		final String path = installer.toAbsolutePath().toString();
		if(path.toLowerCase(Locale.ROOT).endsWith(".msi"))
			return List.of("msiexec", "/i", path);
		//(an installer that asks for administrator rights cannot be started by CreateProcess, error 740: the shell can, with the UAC prompt)
		return List.of("rundll32", "url.dll,FileProtocolHandler", path);
	}

	/**
	 * Starts the installer; it goes on after the application ends.
	 *
	 * @param installer	The downloaded installer.
	 * @throws IOException	If the installer cannot be started.
	 */
	public static void launch(final Path installer) throws IOException{
		new ProcessBuilder(command(installer))
			.redirectErrorStream(true)
			.redirectOutput(ProcessBuilder.Redirect.DISCARD)
			.start();
	}

}
