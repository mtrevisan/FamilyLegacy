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
import java.util.Optional;


/** Compares the running version with the latest release. */
public final class UpdateChecker{

	private final GitHubReleases releases;


	public UpdateChecker(final GitHubReleases releases){
		this.releases = releases;
	}

	/**
	 * @param currentVersion	The version of the running application (the text of the pom).
	 * @return	What was found.
	 * @throws IOException	If GitHub cannot be reached or its answer cannot be understood.
	 */
	public UpdateCheck check(final String currentVersion) throws IOException{
		final Optional<ReleaseInfo> latest = releases.latest();
		if(latest.isEmpty())
			return new UpdateCheck(UpdateCheck.Status.NO_RELEASE, null);

		final ReleaseInfo release = latest.get();
		final Optional<Version> current = Version.parse(currentVersion);
		if(current.isEmpty())
			return new UpdateCheck(UpdateCheck.Status.UNKNOWN_CURRENT_VERSION, release);

		return new UpdateCheck((release.version().compareTo(current.get()) > 0? UpdateCheck.Status.UPDATE_AVAILABLE:
			UpdateCheck.Status.UP_TO_DATE), release);
	}

}
