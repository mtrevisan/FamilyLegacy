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
package io.github.mtrevisan.familylegacy.ui.components.projections.individual;

import io.github.mtrevisan.familylegacy.individual.IndividualSummary;
import io.github.mtrevisan.familylegacy.io.model.FLEFModel;
import io.github.mtrevisan.familylegacy.io.model.readers.IndividualReader;
import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.BoxPanelType;
import io.github.mtrevisan.familylegacy.ui.components.projections.common.PlaceholderImages;
import io.github.mtrevisan.familylegacy.ui.helpers.AsyncResourceLoader;
import io.github.mtrevisan.familylegacy.ui.helpers.ResourceHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Rectangle;
import java.util.function.BiConsumer;


/**
 * The image of an individual (see {@link IndividualSummary} for the rest): the placeholder for its sex, replaced by the preferred image
 * when this has been loaded asynchronously.
 */
public final class IndividualImage{

	private static final Logger LOGGER = LoggerFactory.getLogger(IndividualImage.class);


	private static final int IMAGE_CACHE_SIZE = 300;
	private static final AsyncResourceLoader<ImageIcon> IMAGE_LOADER = new AsyncResourceLoader<>(IMAGE_CACHE_SIZE);

	private static final String DOT = ".";
	private static final String TAG_PIPE = "|";


	private String preferredImageKey;
	private String preferredImageUri;
	private Rectangle preferredImageCropRect;
	private ImageIcon imagePrimary;
	private ImageIcon imageSecondary;


	public static IndividualImage of(final IndividualSummary summary, final FLEFModel model){
		return new IndividualImage(summary, model);
	}


	private IndividualImage(final IndividualSummary summary, final FLEFModel model){
		//if the data of the individual are hidden, placeholders only: the preferred image is not loaded
		extractPreferredImage((summary.isHidden()? null: summary.getIndividual()), model);
	}


	public String getPreferredImageKey(){
		return preferredImageKey;
	}

	public ImageIcon getImagePrimary(){
		return imagePrimary;
	}

	public ImageIcon getImageSecondary(){
		return imageSecondary;
	}

	private void extractPreferredImage(final FLEFRecord record, final FLEFModel model){
		if(record == null){
			preferredImageUri = null;
			preferredImageCropRect = null;
			imagePrimary = PlaceholderImages.placeholder(BoxPanelType.PRIMARY);
			imageSecondary = PlaceholderImages.placeholder(BoxPanelType.SECONDARY);
			preferredImageKey = StringUtils.EMPTY;

			return;
		}

		preferredImageUri = IndividualReader.extractPreferredImageUri(record, model);
		preferredImageCropRect = IndividualReader.extractPreferredImageCrop(record);

		final String rawSex = IndividualReader.extractRawSex(record);
		// Set the default image immediately
		imagePrimary = PlaceholderImages.placeholderFor(rawSex, BoxPanelType.PRIMARY);
		imageSecondary = PlaceholderImages.placeholderFor(rawSex, BoxPanelType.SECONDARY);
		preferredImageKey = composePreferredImageKey(preferredImageUri, preferredImageCropRect);
	}

	public void loadPreferredImageAsync(final BiConsumer<String, ImageIcon[]> imageConsumer){
		if(StringUtils.isEmpty(preferredImageUri))
			return;

		IMAGE_LOADER.load(
			preferredImageKey,
			() -> {
				final ImageIcon croppedImage = ResourceHelper.getCroppedImage(preferredImageUri, preferredImageCropRect);
				if(croppedImage != null){
					final ImageIcon imagePrimary = PlaceholderImages.resize(croppedImage, BoxPanelType.PRIMARY);
					final ImageIcon imageSecondary = PlaceholderImages.resize(croppedImage, BoxPanelType.SECONDARY);
					return new ImageIcon[]{imagePrimary, imageSecondary};
				}
				else{
					LOGGER.error("Non-existent individual image for {}", preferredImageUri);

					return null;
				}
			},
			images -> {
				if(images != null){
					imagePrimary = images[0];
					imageSecondary = images[1];
				}

				imageConsumer.accept(preferredImageKey, images);
			}
		);
	}

	private static String composePreferredImageKey(final String preferredImage, final Rectangle preferredImageCropRect){
		return (StringUtils.isNotEmpty(preferredImage)? preferredImage: StringUtils.EMPTY)
			+ (preferredImageCropRect != null? TAG_PIPE + (int)preferredImageCropRect.getX() + DOT
			+ (int)preferredImageCropRect.getY() + DOT + (int)preferredImageCropRect.getWidth() + DOT
			+ (int)preferredImageCropRect.getHeight(): StringUtils.EMPTY);
	}


}
