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
package io.github.mtrevisan.familylegacy.ui.components.projections.chronomap;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingWorker;
import java.util.List;

/**
 * Background geocoding of the places that are not yet resolved, reporting the progress on an embedded strip.
 */
final class GeocodingWorker{

	private static final Logger LOGGER = LoggerFactory.getLogger(GeocodingWorker.class);

	/** Minimum interval between two refreshes of the map while the geocoding is running. */
	private static final long INCREMENTAL_REFRESH_MS = 3_000L;

	private record ProgressData(int current, int total, String placeName){}

	private GeocodingWorker(){}

	/**
	 * Starts the background geocoding worker.
	 * <p>
	 * The worker is started only when there is actual network work to do.
	 * The list of places to geocode is computed by
	 * {@link PlaceCoordinateResolver#extractMissingPlaces()}, which
	 * checks both the in-memory cache and the on-disk cache. When every
	 * place is already resolved (a typical second run on the same file),
	 * the method returns without showing the progress strip or
	 * scheduling any background task.
	 */
	static SwingWorker<Integer, ?> start(final GeocodingProgressPanel progressPanel, final PlaceCoordinateResolver placeResolver,
			final Runnable onResolved){
		final List<FLEFRecord> placesToGeocode = placeResolver.extractMissingPlaces();
		if(placesToGeocode.isEmpty()){
			LOGGER.debug("Geocoding skipped: all places are already cached");

			return null;
		}

		LOGGER.debug("Geocoding {} places not present in the cache", placesToGeocode.size());

		final SwingWorker<Integer, ProgressData> worker = new SwingWorker<>(){
			private int lastShownCount = placeResolver.cachedCount();
			private long lastShownMs = System.currentTimeMillis();


			@Override
			protected Integer doInBackground(){
				return placeResolver.geocodePlaces(
					placesToGeocode,
					(current, total, placeName) -> publish(new ProgressData(current, total, placeName)),
					this::isCancelled
				);
			}

			@Override
			protected void process(final List<ProgressData> chunks){
				if(isCancelled())
					return;

				final ProgressData latest = chunks.getLast();
				progressPanel.updateProgress(latest.current(), latest.total(), latest.placeName());

				//show the places already resolved without waiting for the end (the index rebuild is throttled)
				final long now = System.currentTimeMillis();
				final int resolvedNow = placeResolver.cachedCount();
				if(resolvedNow != lastShownCount && now - lastShownMs >= INCREMENTAL_REFRESH_MS){
					lastShownCount = resolvedNow;
					lastShownMs = now;

					onResolved.run();
				}
			}

			@Override
			protected void done(){
				progressPanel.end();

				if(isCancelled()){
					//the places resolved before the cancellation are already cached: show them
					LOGGER.debug("Geocoding worker cancelled");
					onResolved.run();

					return;
				}

				try{
					final int resolved = get();
					if(resolved > 0)
						onResolved.run();
				}
				catch(final Exception ignored){
					LOGGER.debug("Geocoding worker cancelled or interrupted");
				}
			}
		};

		progressPanel.begin(() -> worker.cancel(true));
		worker.execute();
		return worker;
	}

}
