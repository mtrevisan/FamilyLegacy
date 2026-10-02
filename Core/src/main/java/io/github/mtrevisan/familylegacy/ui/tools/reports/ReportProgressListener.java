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
package io.github.mtrevisan.familylegacy.ui.tools.reports;


/**
 * Receives progress notifications during report generation.
 *
 * <p>The percentage is a hint, not a contract: the caller may jump from
 * 0 to 100 without intermediate values, and some phases (e.g. building a
 * large section) may take significantly longer than others. The message
 * is a short human-readable description of the current step, already
 * localized by the caller when appropriate.</p>
 *
 * <p>Implementations must be thread-safe: the listener is invoked from a
 * background thread (typically the one that runs the {@link javax.swing.SwingWorker}),
 * not from the Swing Event Dispatch Thread.</p>
 */
@FunctionalInterface
public interface ReportProgressListener{

	/** No-op listener, used when the caller is not interested in progress. */
	ReportProgressListener NOOP = (percent, message) -> {};


	/**
	 * Notifies the listener that the generation has reached the given
	 * completion percentage.
	 *
	 * @param percent an integer between 0 and 100
	 * @param message a short description of the current step; may be
	 *                {@code null} when the caller has nothing to add
	 */
	void onProgress(int percent, String message);

}
