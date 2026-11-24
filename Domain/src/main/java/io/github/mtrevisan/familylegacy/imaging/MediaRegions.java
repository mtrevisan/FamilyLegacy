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
package io.github.mtrevisan.familylegacy.imaging;

import io.github.mtrevisan.familylegacy.io.model.readers.CropReader;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.readers.MediaCitationReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.io.model.record.RecordLookup;
import org.apache.commons.lang3.StringUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The regions of a media: the parts of an image that show somebody, found where the individuals say it. There is nothing of their own in the
 * file: a region is the {@code crop} that an individual has on the media, in the {@code media} citation of the individual, or in its preferred
 * image. The individuals that have the same crop of the same page share a region (the couple of a wedding photograph, cropped by both), and
 * the regions that overlap (a face inside the group it belongs to) are told apart by their size.
 * <p>
 * The crops are in the pixels of the image, like all the crops of the file. A citation with a crop is a region of its individual whatever its
 * caption says: a crop of a photograph says which part of it concerns the individual.
 * </p>
 */
public final class MediaRegions{

	/**
	 * A region of a media.
	 *
	 * @param area        the rectangle, in pixels of the image
	 * @param page        the 1-based page ({@code null} for the media that has no pages)
	 * @param individuals the ids of the individuals shown, in the order of the file, without duplicates
	 */
	public record Region(Rectangle area, Integer page, List<String> individuals){

		public Region{
			if(area == null || area.isEmpty())
				throw new IllegalArgumentException("The area is empty");
			if(page != null && page < 1)
				throw new IllegalArgumentException("The page is 1-based: " + page);

			area = new Rectangle(area);
			individuals = (individuals != null? List.copyOf(new LinkedHashSet<>(individuals)): List.of());
		}


		/** @return the page the region is on: the first one for the media that has no pages */
		public int effectivePage(){
			return (page != null? page: 1);
		}

		/** @return the size of the area, to tell which of the regions that overlap is the smallest */
		public long areaSize(){
			return (long)area.width * area.height;
		}

		/** @return whether the point (in pixels of the image) is inside the region, the sides included */
		public boolean contains(final double x, final double y){
			return (x >= area.x && x <= area.x + area.width && y >= area.y && y <= area.y + area.height);
		}

		/** @return the same area and page, for other individuals */
		public Region withIndividuals(final List<String> individuals){
			return new Region(area, page, individuals);
		}

	}


	private MediaRegions(){}


	/**
	 * @param model   the records
	 * @param mediaId the id of the media
	 * @return the regions of the media (on every page), in the order the individuals say them; none if the media is not cited with a crop
	 */
	public static List<Region> of(final RecordLookup model, final String mediaId){
		final Map<Key, Set<String>> found = new LinkedHashMap<>();
		if(mediaId == null)
			return List.of();

		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE)){
			for(final FLEFRecord citation : FLEFRecordHelper.findChildren(individual, MediaCitationReader.TAG)){
				if(!mediaId.equals(StringUtils.trimToNull(FLEFRecordHelper.getChildValue(citation, MediaCitationReader.TAG_MEDIA))))
					continue;

				add(found, individual, CropReader.extractPreferredImageCrop(FLEFRecordHelper.findChild(citation, MediaCitationReader.TAG_CROP)),
					parsePage(FLEFRecordHelper.getChildValue(citation, MediaCitationReader.TAG_PAGE)));
			}
			if(mediaId.equals(IndividualReader.extractPreferredImageMediaId(individual)))
				add(found, individual, IndividualReader.extractPreferredImageCrop(individual), null);
		}

		final List<Region> regions = new ArrayList<>(found.size());
		for(final Map.Entry<Key, Set<String>> entry : found.entrySet())
			regions.add(new Region(entry.getKey().area(), entry.getKey().page(), List.copyOf(entry.getValue())));
		return regions;
	}

	/**
	 * The media in which an individual is tagged, or that it cites, with what it has on each.
	 *
	 * @param mediaId  the id of the media
	 * @param citation the first citation of the media by the individual ({@code null} if the media is only its preferred image)
	 * @param regions  the regions of the individual on the media: its crops, each with the individual as its only person
	 * @param portrait whether the media is the preferred image of the individual
	 */
	public record IndividualMedia(String mediaId, FLEFRecord citation, List<Region> regions, boolean portrait){

		/** @return whether the individual is tagged in the media, that is, has at least one crop on it */
		public boolean isTagged(){
			return !regions.isEmpty();
		}

	}


	/**
	 * @param individual the individual record (may be {@code null})
	 * @return the media that the individual cites in its {@code media} citations or has as preferred image, one entry for each media, in the order
	 * 	of the file (the preferred image last, if it is not cited); the ones that have regions are the media in which the individual is tagged
	 */
	public static List<IndividualMedia> mediaOf(final FLEFRecord individual){
		final Map<String, FLEFRecord> citations = new LinkedHashMap<>();
		final Map<String, Set<Key>> crops = new LinkedHashMap<>();
		if(individual == null)
			return List.of();

		for(final FLEFRecord citation : FLEFRecordHelper.findChildren(individual, MediaCitationReader.TAG)){
			final String mediaId = StringUtils.trimToNull(FLEFRecordHelper.getChildValue(citation, MediaCitationReader.TAG_MEDIA));
			if(mediaId == null)
				continue;

			citations.putIfAbsent(mediaId, citation);
			final Set<Key> keys = crops.computeIfAbsent(mediaId, id -> new LinkedHashSet<>());
			final Rectangle crop = CropReader.extractPreferredImageCrop(FLEFRecordHelper.findChild(citation, MediaCitationReader.TAG_CROP));
			if(crop != null)
				keys.add(new Key(crop, parsePage(FLEFRecordHelper.getChildValue(citation, MediaCitationReader.TAG_PAGE))));
		}
		final String preferred = IndividualReader.extractPreferredImageMediaId(individual);
		if(preferred != null){
			final Set<Key> keys = crops.computeIfAbsent(preferred, id -> new LinkedHashSet<>());
			final Rectangle crop = IndividualReader.extractPreferredImageCrop(individual);
			if(crop != null)
				keys.add(new Key(crop, null));
		}

		final List<String> people = (individual.getId() != null? List.of(individual.getId()): List.of());
		final List<IndividualMedia> media = new ArrayList<>(crops.size());
		for(final Map.Entry<String, Set<Key>> entry : crops.entrySet()){
			final List<Region> regions = new ArrayList<>(entry.getValue().size());
			for(final Key key : entry.getValue())
				regions.add(new Region(key.area(), key.page(), people));
			media.add(new IndividualMedia(entry.getKey(), citations.get(entry.getKey()), regions, entry.getKey().equals(preferred)));
		}
		return media;
	}

	/**
	 * @param model the records
	 * @return how many individuals are tagged in each media (the ones that have a crop on it), by the id of the media; the media in which nobody is
	 * 	tagged are not there
	 */
	public static Map<String, Integer> taggedPeople(final RecordLookup model){
		final Map<String, Integer> counts = new LinkedHashMap<>();
		for(final FLEFRecord individual : model.getRecordsByType(IndividualReader.TYPE))
			for(final IndividualMedia media : mediaOf(individual))
				if(media.isTagged())
					counts.merge(media.mediaId(), 1, Integer::sum);
		return counts;
	}

	/**
	 * @param regions the regions of a media
	 * @param page    the 1-based page shown ({@code null} for the media that has no pages)
	 * @return the regions on the page, in order
	 */
	public static List<Region> onPage(final List<Region> regions, final Integer page){
		final int shown = (page != null? page: 1);
		final List<Region> result = new ArrayList<>(regions.size());
		for(final Region region : regions)
			if(region.effectivePage() == shown)
				result.add(region);
		return result;
	}

	/**
	 * Which regions are under a point: where a click or the mouse is. Where regions overlap (a face inside the group it belongs to) the smallest
	 * comes first, as the most specific one; the ones of the same size keep their order.
	 *
	 * @param regions the regions of the media
	 * @param x       the horizontal position, in pixels of the image
	 * @param y       the vertical position, in pixels of the image
	 * @param page    the 1-based page shown ({@code null} for the media that has no pages)
	 * @return the regions that contain the point on the page, smallest first
	 */
	public static List<Region> at(final List<Region> regions, final double x, final double y, final Integer page){
		final int shown = (page != null? page: 1);
		final List<Region> result = new ArrayList<>(0);
		for(final Region region : regions)
			if(region.effectivePage() == shown && region.contains(x, y))
				result.add(region);
		//(a stable sort)
		result.sort(Comparator.comparingLong(Region::areaSize));
		return result;
	}

	/**
	 * Whether the region is the portrait of the individual: its preferred image is this media, cropped to this region.
	 *
	 * @param individual the individual record ({@code null} is nobody's portrait)
	 * @param mediaId    the id of the media the region belongs to
	 * @param region     the region
	 * @return whether the region is the preferred image of the individual
	 */
	public static boolean isPortraitOf(final FLEFRecord individual, final String mediaId, final Region region){
		return (individual != null && mediaId != null && mediaId.equals(IndividualReader.extractPreferredImageMediaId(individual))
			&& region.area().equals(IndividualReader.extractPreferredImageCrop(individual)));
	}


	private static void add(final Map<Key, Set<String>> found, final FLEFRecord individual, final Rectangle crop, final Integer page){
		if(crop == null || individual.getId() == null)
			return;

		found.computeIfAbsent(new Key(crop, page), key -> new LinkedHashSet<>())
			.add(individual.getId());
	}

	private static Integer parsePage(final String value){
		try{
			final int page = (value != null? Integer.parseInt(value.trim()): 0);
			return (page >= 1? page: null);
		}
		catch(final NumberFormatException ignored){
			return null;
		}
	}


	/** What makes two crops the same region (a missing page is the first one). */
	private record Key(Rectangle area, Integer page){

		private Key{
			page = (page != null && page != 1? page: null);
		}

	}

}