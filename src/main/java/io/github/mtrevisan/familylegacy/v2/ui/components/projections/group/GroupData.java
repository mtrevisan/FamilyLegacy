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
package io.github.mtrevisan.familylegacy.v2.ui.components.projections.group;

import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecord;
import io.github.mtrevisan.familylegacy.v2.io.model.FLEFRecordHelper;
import io.github.mtrevisan.familylegacy.v2.ui.components.projections.BoxPanelType;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.AsyncResourceLoader;
import io.github.mtrevisan.familylegacy.v2.ui.helpers.ResourceHelper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;


/**
 * Extracts display information for a group from a FLEFModel.
 */
public final class GroupData{

	private static final Logger LOGGER = LoggerFactory.getLogger(GroupData.class);


	private static final AsyncResourceLoader<ImageIcon> IMAGE_LOADER = new AsyncResourceLoader<>();


	private static final String DOT = ".";
	private static final String TAG_PIPE = "|";

	private static final String TAG_GROUP = "group";
	private static final String TAG_NAME = "name";
	private static final String TAG_TYPE = "type";
	private static final String TAG_VALUE = "value";
	private static final String TAG_PREFERRED_IMAGE = "preferred_image";
	private static final String TAG_URI = "uri";
	private static final String TAG_CROP = "crop";
	private static final String TAG_X = "x";
	private static final String TAG_Y = "y";
	private static final String TAG_WIDTH = "width";
	private static final String TAG_HEIGHT = "height";

	private static final String TAG_NAME_VALUE = TAG_NAME + DOT + TAG_VALUE;
	private static final String TAG_PREFERRED_IMAGE_URI = TAG_PREFERRED_IMAGE + DOT + TAG_URI;
	private static final String TAG_PREFERRED_IMAGE_CROP = TAG_PREFERRED_IMAGE + DOT + TAG_CROP;

	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BR = "<br>";

	private static final String NO_DATA = "?";

	private static final ImageIcon ADD_PHOTO = ResourceHelper.getImageFromResource("/images/preferred_image_placeholder.jpg");

	private static final double PREFERRED_IMAGE_WIDTH = 48.;
	private static final double IMAGE_ASPECT_RATIO = 4. / 3.;


	private final FLEFRecord group;
	private final String id;
	private final String nameText;
	private String nameTooltip;
	private final String yype;

	private String preferredImageKey;
	private String preferredImageUri;
	private Rectangle preferredImageCropRect;
	private ImageIcon imagePrimary;
	private ImageIcon imageSecondary;


	public static GroupData create(final FLEFRecord group){
		return (group != null
			? new GroupData(group)
			: null);
	}


	private GroupData(final FLEFRecord group){
		this.group = group;
		id = group.getId();

		final List<String> names = extractGroupNames(group);
		if(!names.isEmpty()){
			nameText = names.getFirst();
			nameTooltip = TAG_HTML_OPEN + StringUtils.join(names, TAG_BR) + TAG_HTML_CLOSE;
		}
		else{
			nameText = NO_DATA;
			nameTooltip = null;
		}

		final String rawType = FLEFRecordHelper.getChildValue(group, TAG_TYPE);
		yype = (rawType != null? rawType.replace('_', ' '): StringUtils.EMPTY);

		extractPreferredImage(group);
	}


	public FLEFRecord getGroup(){
		return group;
	}

	public String getId(){
		return id;
	}

	public String getNameText(){
		return nameText;
	}

	public String getNameTooltip(){
		return nameTooltip;
	}

	public String getYype(){
		return yype;
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

	public boolean isEmpty(){
		return (id == null);
	}

	private List<String> extractGroupNames(final FLEFRecord group){
		final List<String> names = new ArrayList<>();
		if(group == null || !TAG_GROUP.equals(group.getTag()))
			return names;

		for(final FLEFRecord nameStruct : FLEFRecordHelper.findChildren(group, TAG_NAME)){
			final String val = FLEFRecordHelper.getChildValue(nameStruct, TAG_VALUE);
			if(StringUtils.isNotEmpty(val))
				names.add(val.trim());
			else if(StringUtils.isNotEmpty(nameStruct.getValue()))
				names.add(nameStruct.getValue().trim());
		}
		return names;
	}


	private void extractPreferredImage(final FLEFRecord record){
		if(record == null){
			preferredImageUri = null;
			preferredImageCropRect = null;
			imagePrimary = resize(ADD_PHOTO, BoxPanelType.PRIMARY);
			imageSecondary = resize(ADD_PHOTO, BoxPanelType.SECONDARY);
			preferredImageKey = StringUtils.EMPTY;

			return;
		}

		preferredImageUri = FLEFRecordHelper.getChildValue(record, TAG_PREFERRED_IMAGE_URI);
// TODO to be removed
		if(preferredImageUri != null)
			preferredImageUri = "C:\\mauro\\heritage\\My Genealogy Projects\\Trevisan (Dorato)-Gallinaro-Masutti (Manfrin)-Zaros (Basso)" + preferredImageUri;
		preferredImageCropRect = null;
		try{
			final FLEFRecord crop = FLEFRecordHelper.findChild(record, TAG_PREFERRED_IMAGE_CROP);
			final int cropX = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_X));
			final int cropY = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_Y));
			final int cropWidth = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_WIDTH));
			final int cropHeight = Integer.parseInt(FLEFRecordHelper.getChildValue(crop, TAG_HEIGHT));
			if(cropX >= 0 && cropY >= 0 && cropWidth >= 0 && cropHeight >= 0)
				preferredImageCropRect = new Rectangle(cropX, cropY, cropWidth, cropHeight);
		}
		catch(final Exception ignored){}

		// Set the default image immediately
		imagePrimary = resize(ADD_PHOTO, BoxPanelType.PRIMARY);
		imageSecondary = resize(ADD_PHOTO, BoxPanelType.SECONDARY);
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
					final ImageIcon imagePrimary = resize(croppedImage, BoxPanelType.PRIMARY);
					final ImageIcon imageSecondary = resize(croppedImage, BoxPanelType.SECONDARY);
					return new ImageIcon[]{imagePrimary, imageSecondary};
				}
				else{
					LOGGER.error("Non-existent image for {}", preferredImageUri);

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

	private ImageIcon resize(final ImageIcon image, final BoxPanelType boxType){
		final double shrinkFactor = (boxType == BoxPanelType.PRIMARY? 1.: 2.);
		final int preferredImageWidth = (int)Math.ceil(PREFERRED_IMAGE_WIDTH / shrinkFactor);
		final int preferredImageHeight = (int)Math.ceil(PREFERRED_IMAGE_WIDTH * IMAGE_ASPECT_RATIO / shrinkFactor);
		return ResourceHelper.resize(image, preferredImageWidth, preferredImageHeight);
	}

	private static String composePreferredImageKey(final String preferredImage, final Rectangle preferredImageCropRect){
		return (StringUtils.isNotEmpty(preferredImage)? preferredImage: StringUtils.EMPTY)
			+ (preferredImageCropRect != null? TAG_PIPE + (int)preferredImageCropRect.getX() + DOT
			+ (int)preferredImageCropRect.getY() + DOT + (int)preferredImageCropRect.getWidth() + DOT
			+ (int)preferredImageCropRect.getHeight(): StringUtils.EMPTY);
	}


	@Override
	public String toString(){
		return nameText + " [" + id + "]";
	}

}
