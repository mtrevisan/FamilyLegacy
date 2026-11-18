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
package io.github.mtrevisan.familylegacy.imaging.opencv;

import org.bytedeco.javacpp.FloatPointer;
import org.bytedeco.javacpp.PointerScope;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.FaceDetectorYN;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.bytedeco.opencv.global.opencv_core.CV_8UC3;
import static org.bytedeco.opencv.global.opencv_dnn.DNN_BACKEND_OPENCV;
import static org.bytedeco.opencv.global.opencv_dnn.DNN_TARGET_CPU;


/**
 * Finds the faces with OpenCV and YuNet ({@code FaceDetectorYN}, an ONNX model of about 230 KB, much better than the Haar cascades on small,
 * turned faces and on old photographs).
 */
public class FaceDetector implements io.github.mtrevisan.familylegacy.imaging.FaceDetector{

	private static final String MODEL_RESOURCE = "/facedetector/face_detection_yunet_2023mar.onnx";
	/** A bigger picture is shrunk for the detection (the boxes are scaled back): the model is multi-scale, more pixels only cost time. */
	private static final int MAX_SIDE = 2048;
	private static final float SCORE_THRESHOLD = 0.6f;
	private static final float NMS_THRESHOLD = 0.3f;
	private static final int TOP_K = 5000;
	/** The columns of a row of the result: x, y, width, height, five landmarks (x, y), the score. */
	private static final int SCORE_COLUMN = 14;
	private static final int COLUMNS = 15;


	private Path modelFile;


	@Override
	public String id(){
		return "opencv";
	}

	@Override
	public void warmUp() throws IOException{
		//a small blank picture: it loads the native libraries, extracts the model and runs the network once
		detectFaces(new BufferedImage(64, 64, BufferedImage.TYPE_3BYTE_BGR));
	}

	@Override
	public synchronized List<Rectangle> detectFaces(final BufferedImage image) throws IOException{
		if(image == null || image.getWidth() <= 0 || image.getHeight() <= 0)
			return List.of();

		try{
			return detect(image);
		}
		catch(final UnsatisfiedLinkError | NoClassDefFoundError | RuntimeException e){
			//the native libraries of the platform are missing, or OpenCV refused the model or the picture
			throw new IOException("The face detector cannot work: " + e.getMessage(), e);
		}
	}

	private List<Rectangle> detect(final BufferedImage image) throws IOException{
		final Path model = modelFile();

		final double scale = Math.min(1., (double)MAX_SIDE / Math.max(image.getWidth(), image.getHeight()));
		final BufferedImage bgr = toBgr(image, scale);
		final int width = bgr.getWidth();
		final int height = bgr.getHeight();

		final List<float[]> found = new ArrayList<>();
		try(final PointerScope scope = new PointerScope()){
			final Mat mat = new Mat(height, width, CV_8UC3);
			mat.data().put(((DataBufferByte)bgr.getRaster().getDataBuffer()).getData());

			final FaceDetectorYN detector = FaceDetectorYN.create(model.toString(), "", new Size(width, height), SCORE_THRESHOLD,
				NMS_THRESHOLD, TOP_K, DNN_BACKEND_OPENCV, DNN_TARGET_CPU);
			final Mat faces = new Mat();
			detector.detect(mat, faces);

			final int count = faces.rows();
			if(count > 0){
				//(a new matrix is continuous: the rows follow one another)
				final FloatPointer values = new FloatPointer(faces.data());
				for(int row = 0; row < count; row ++){
					final long offset = (long)row * COLUMNS;
					found.add(new float[]{values.get(offset), values.get(offset + 1), values.get(offset + 2), values.get(offset + 3),
						values.get(offset + SCORE_COLUMN)});
				}
			}
		}

		found.sort(Comparator.comparingDouble((float[] face) -> face[4]).reversed());
		final Rectangle bounds = new Rectangle(0, 0, image.getWidth(), image.getHeight());
		final List<Rectangle> rectangles = new ArrayList<>(found.size());
		for(final float[] face : found){
			final Rectangle box = new Rectangle(
				(int)Math.round(face[0] / scale), (int)Math.round(face[1] / scale),
				(int)Math.round(face[2] / scale), (int)Math.round(face[3] / scale))
				.intersection(bounds);
			if(box.width > 0 && box.height > 0)
				rectangles.add(box);
		}
		return rectangles;
	}

	/** @return	The picture as 8-bit B, G, R (what OpenCV reads), shrunk by {@code scale}, with the transparent parts white. */
	private static BufferedImage toBgr(final BufferedImage image, final double scale){
		final int width = Math.max(1, (int)Math.round(image.getWidth() * scale));
		final int height = Math.max(1, (int)Math.round(image.getHeight() * scale));
		final BufferedImage bgr = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		final Graphics2D g2 = bgr.createGraphics();
		try{
			g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g2.setColor(Color.WHITE);
			g2.fillRect(0, 0, width, height);
			g2.drawImage(image, 0, 0, width, height, null);
		}
		finally{
			g2.dispose();
		}
		return bgr;
	}

	/** The model is a resource of the jar and OpenCV reads a file: it is copied once into a temporary file. */
	private Path modelFile() throws IOException{
		if(modelFile == null || !Files.isRegularFile(modelFile)){
			try(final InputStream in = FaceDetector.class.getResourceAsStream(MODEL_RESOURCE)){
				if(in == null)
					throw new IOException("The model of the face detector is missing");

				final Path file = Files.createTempFile("familylegacy-yunet", ".onnx");
				file.toFile().deleteOnExit();
				Files.copy(in, file, StandardCopyOption.REPLACE_EXISTING);
				modelFile = file;
			}
		}
		return modelFile;
	}

}
