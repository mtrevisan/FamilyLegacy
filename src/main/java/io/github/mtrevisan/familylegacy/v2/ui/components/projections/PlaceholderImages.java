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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections;

import io.github.mtrevisan.familylegacy.v2.ui.dialogs.records.SexType;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;

import javax.swing.ImageIcon;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;


/**
 * Shared cache of the placeholder images used when an individual or a
 * group has no preferred image, plus the sizing helpers that both
 * {@code IndividualData} and {@code GroupData} use for their real and
 * placeholder photos.
 * <p>
 * The three source images (unknown / male / female) never change, and
 * there are only six combinations (three sexes × two box types), so the
 * resized variants are computed once at class load and shared by every
 * consumer. Building them lazily per instance would resize the same
 * source images once per individual, which is wasteful on large models.
 * <p>
 * The class is intentionally stateless from the caller's point of view:
 * the only mutable state is the pre-computed cache, which is initialized
 * during class loading and then treated as read-only.
 */
public final class PlaceholderImages{

	/** Base width of the placeholder, in pixels. */
	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	/** Width-to-height ratio used for the placeholder and for the photos. */
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;

	private static final ImageIcon PLACEHOLDER_UNKNOWN = ResourceHelper.getImageFromResource("/images/preferred_image_placeholder.svg");
	private static final ImageIcon PLACEHOLDER_MALE = ResourceHelper.getImageFromResource("/images/preferred_image_placeholder.male.svg");
	private static final ImageIcon PLACEHOLDER_FEMALE = ResourceHelper.getImageFromResource("/images/preferred_image_placeholder.female.svg");

	private static final Map<SexType, Map<BoxPanelType, ImageIcon>> CACHE = buildCache();


	private PlaceholderImages(){}


	/* ======================================================================
	 *                          Public API
	 * ====================================================================== */

	/**
	 * Returns the placeholder icon for the given sex and box type, or the
	 * unknown-sex placeholder when {@code sex} is {@code null}.
	 *
	 * @param sex     the individual's sex; may be {@code null}
	 * @param boxType the box type (PRIMARY or SECONDARY)
	 * @return the shared placeholder icon, never {@code null}
	 */
	public static ImageIcon placeholder(final SexType sex, final BoxPanelType boxType){
		final SexType effective = (sex != null? sex: SexType.UNKNOWN);
		return CACHE.get(effective).get(boxType);
	}

	/**
	 * Convenience overload for callers that have no sex information, such
	 * as the group data extractor.
	 *
	 * @param boxType the box type (PRIMARY or SECONDARY)
	 * @return the shared placeholder icon, never {@code null}
	 */
	public static ImageIcon placeholder(final BoxPanelType boxType){
		return placeholder(SexType.UNKNOWN, boxType);
	}

	/**
	 * Convenience overload that resolves the sex from its raw string
	 * representation (the value stored in the {@code sex} tag). Falls
	 * back to {@link SexType#UNKNOWN} when the value is missing or is
	 * not a valid enum name.
	 *
	 * @param rawSex  the raw value from the model; may be {@code null}
	 * @param boxType the box type (PRIMARY or SECONDARY)
	 * @return the shared placeholder icon, never {@code null}
	 */
	public static ImageIcon placeholderFor(final String rawSex, final BoxPanelType boxType){
		return placeholder(resolveSex(rawSex), boxType);
	}

	/**
	 * Resizes the given image to fit a box of the given type, preserving
	 * the aspect ratio. The result is the same size as the placeholder
	 * for the same box type, so real photos and placeholders line up.
	 *
	 * @param image   the image to resize; may be {@code null}
	 * @param boxType the box type (PRIMARY or SECONDARY)
	 * @return the resized image, or {@code null} when the input is {@code null}
	 */
	public static ImageIcon resize(final ImageIcon image, final BoxPanelType boxType){
		if(image == null)
			return null;

		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int width = (int)Math.ceil(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		final int height = (int)Math.ceil(PREFERRED_IMAGE_WIDTH * IMAGE_ASPECT_RATIO / shrinkFactor);
		return ResourceHelper.resize(image, width, height);
	}


	/* ======================================================================
	 *                          Internal
	 * ====================================================================== */

	private static Map<SexType, Map<BoxPanelType, ImageIcon>> buildCache(){
		final Map<SexType, Map<BoxPanelType, ImageIcon>> cache = new EnumMap<>(SexType.class);
		for(final SexType sex : SexType.values()){
			final ImageIcon source = switch(sex){
				case MALE -> PLACEHOLDER_MALE;
				case FEMALE -> PLACEHOLDER_FEMALE;
				case UNKNOWN -> PLACEHOLDER_UNKNOWN;
			};
			final Map<BoxPanelType, ImageIcon> byBoxType = new EnumMap<>(BoxPanelType.class);
			for(final BoxPanelType boxType : BoxPanelType.values())
				byBoxType.put(boxType, resize(source, boxType));
			cache.put(sex, byBoxType);
		}
		return cache;
	}

	private static SexType resolveSex(final String rawSex){
		if(rawSex == null)
			return SexType.UNKNOWN;
		try{
			return SexType.valueOf(rawSex.toUpperCase(Locale.ROOT));
		}
		catch(final IllegalArgumentException ignored){
			return SexType.UNKNOWN;
		}
	}

}
