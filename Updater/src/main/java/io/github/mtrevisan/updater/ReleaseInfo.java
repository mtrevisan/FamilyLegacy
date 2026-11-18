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

import java.net.URI;


/**
 * A release published on GitHub, with the installer to download.
 *
 * @param version	The version of the release (from its tag).
 * @param tag	The tag of the release, as published.
 * @param name	The title of the release.
 * @param notes	The release notes (Markdown); empty if none.
 * @param pageUrl	The page of the release, to open in a browser.
 * @param installerName	The file name of the installer; {@code null} if the release has none for this system.
 * @param installerUrl	Where to download the installer from; {@code null} if the release has none for this system.
 * @param installerSize	The size of the installer in bytes, or -1 if unknown.
 * @param sha256	The SHA-256 of the installer, in hexadecimal lower case; {@code null} if the release does not publish it.
 */
public record ReleaseInfo(Version version, String tag, String name, String notes, URI pageUrl, String installerName, URI installerUrl,
		long installerSize, String sha256){

	/** @return	Whether the installer can be downloaded and checked: it has a published SHA-256. */
	public boolean isInstallable(){
		return (hasInstaller() && sha256 != null);
	}

	/** @return	Whether the release carries an installer that can be downloaded. */
	public boolean hasInstaller(){
		return (installerUrl != null);
	}

}
