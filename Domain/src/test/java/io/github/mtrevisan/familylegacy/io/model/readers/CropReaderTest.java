package io.github.mtrevisan.familylegacy.io.model.readers;

import io.github.mtrevisan.familylegacy.io.model.record.FLEFRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;


class CropReaderTest{

	private static FLEFRecord crop(final String width, final String height){
		return FLEFRecord.createChildWithTag(CropReader.TAG_CROP)
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_X, "1"))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_Y, "2"))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_WIDTH, width))
			.addChild(FLEFRecord.createChildWithTagAndValue(CropReader.TAG_HEIGHT, height));
	}

	@Test
	void validCrop(){
		Assertions.assertEquals(new Rectangle(1, 2, 3, 4), CropReader.extractPreferredImageCrop(crop("3", "4")));
	}

	@Test
	void emptyCropIsRejected(){
		Assertions.assertNull(CropReader.extractPreferredImageCrop(crop("0", "4")));
		Assertions.assertNull(CropReader.extractPreferredImageCrop(crop("3", "0")));
	}

}
