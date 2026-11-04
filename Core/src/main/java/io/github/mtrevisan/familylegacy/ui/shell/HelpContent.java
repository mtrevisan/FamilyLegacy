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
package io.github.mtrevisan.familylegacy.ui.shell;


/**
 * Static content used by the Help menu: URLs and version. The pages of the help are Markdown files
 * (see {@link io.github.mtrevisan.familylegacy.ui.dialogs.help.HelpTopics}).
 */
final class HelpContent{

	// TODO i18n
	/** Base URL of the online documentation. */
	static final String DOCUMENTATION_URL = "https://github.com/mtrevisan/family-legacy/wiki";
	/** URL of the issue tracker, with a prefilled "new issue" form. */
	static final String ISSUE_TRACKER_URL = "https://github.com/mtrevisan/family-legacy/issues/new";
	/** URL of the release page. */
	static final String RELEASES_URL = "https://github.com/mtrevisan/family-legacy/releases";
	/** Version shown in the About dialog and in "Check for Updates". */
	static final String APPLICATION_VERSION = "2.0.0 (development)";


	private HelpContent(){
	}

}
