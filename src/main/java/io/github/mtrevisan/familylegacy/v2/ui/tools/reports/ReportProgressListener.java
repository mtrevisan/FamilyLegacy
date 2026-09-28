package io.github.mtrevisan.familylegacy.v2.ui.tools.reports;

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
