package io.github.mtrevisan.familylegacy.v2.io;

@FunctionalInterface
public interface ProgressListener{

	ProgressListener NOOP = (percent, message) -> {};


	/**
	 * @param percent 0-100 for a determinate step, or a negative value
	 *                to switch back to an indeterminate animation
	 * @param message short description of the current step
	 */
	void onProgress(int percent, String message);

}
