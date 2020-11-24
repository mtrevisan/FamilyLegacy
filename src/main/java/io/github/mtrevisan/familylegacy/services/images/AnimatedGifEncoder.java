/**
 * Copyright (c) 2020 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.services.images;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;


/**
 * @see <a href="https://github.com/rtyley/animated-gif-lib-for-java">Animated GIF library for Java</a>
 */
public class AnimatedGifEncoder{

	private int width;
	private int height;

	private Color transparent;
	//transparent color will be found by looking for the closest color
	private boolean transparentExactMatch;
	//transparent color index in color table
	private int transparentIndex;

	/**
	 * Background color for the last added frame and any subsequent frames.
	 * <p>Since all colors are subject to modification in the quantization process, the color in the final palette for each frame closest
	 * to the given color becomes the background color for that frame.</p>
	 * <p>May be set to <code>null</code> to indicate no background color which will default to black.</p>
	 */
	private Color background;
	//no repeat
	private int repeatCount = -1;
	/** Delay [100 ms] between each frame, or changes it for subsequent frames (applies to last frame added). */
	private int delay;
	//ready to output frames
	private boolean started;
	private OutputStream out;
	//current frame
	private BufferedImage image;
	//BGR byte array from frame
	private byte[] pixels;
	//converted frame indexed to palette
	private byte[] indexedPixels;
	//number of bit planes
	private int colorDepth;
	//RGB palette
	private byte[] colorTab;
	//active palette entries
	private final boolean[] usedEntry = new boolean[256];
	//color table size (bits-1)
	private int palSize = 7;
	//disposal code (-1 = use default)
	private int disposalCode = -1;
	//close stream when finished
	private boolean closeStream;
	private boolean firstFrameAdded;
	//if false, get size from first frame
	private boolean sizeSet;
	//default sample interval for quantizer
	private int sample = 10;


	public void setBackground(final Color background){
		this.background = background;
	}

	public void setDelay(final int delay){
		this.delay = delay;
	}

	public void setStarted(final boolean started){
		this.started = started;
	}

	/**
	 * Sets the GIF frame disposal code for the last added frame and any subsequent frames.
	 * Default is <code>0</code> if no transparent color has been set, otherwise <code>2</code>.
	 *
	 * @param disposalCode	Disposal code.
	 */
	public void setDispose(final int disposalCode){
		if(disposalCode >= 0)
			this.disposalCode = disposalCode;
	}

	/**
	 * Sets the number of times the set of GIF frames should be played. Default is <code>1</code>; <code>0</code> means play
	 * indefinitely.
	 * Must be invoked before the first image is added.
	 *
	 * @param iterations	Number of iterations.
	 * @throws IllegalArgumentException	If a frame has already been added.
	 */
	public void setRepeat(final int iterations) throws IllegalArgumentException{
		if(firstFrameAdded)
			throw new IllegalArgumentException("Cannot set repeat count after the first frame has been added.");

		if(iterations >= 0)
			repeatCount = iterations;
	}

	/**
	 * Sets the transparent color for the last added frame and any subsequent frames.
	 * Since all colors are subject to modification in the quantization process, the color in the final
	 * palette for each frame closest to the given color becomes the transparent color for that frame.
	 * May be set to <code>null</code> to indicate no transparent color.
	 *
	 * @param color	Color to be treated as transparent on display.
	 */
	public void setTransparent(final Color color){
		setTransparent(color, false);
	}

	/**
	 * Sets the transparent color for the last added frame and any subsequent frames.
	 * Since all colors are subject to modification in the quantization process, the color in the final
	 * palette for each frame closest to the given color becomes the transparent color for that frame.
	 * If exactMatch is set to true, transparent color index is search with exact match, and not looking for the
	 * closest one.
	 * May be set to <code>null</code> to indicate no transparent color.
	 *
	 * @param color		Color to be treated as transparent on display.
	 * @param exactMatch	Whether an exact match should be applied.
	 */
	public void setTransparent(final Color color, final boolean exactMatch){
		transparent = color;
		transparentExactMatch = exactMatch;
	}

	/**
	 * Sets frame rate in frames per second. Equivalent to <code>setDelay(1000 / fps)</code>.
	 *
	 * @param fps	frame rate [frame/s]
	 */
	public void setFrameRate(final float fps){
		if(fps != 0.f)
			delay = Math.round(1000.f / fps);
	}

	/**
	 * Sets quality of color quantization (conversion of images to the maximum <code>256</code> colors allowed by the GIF specification).
	 * Lower values (minimum = <code>1</code>) produce better colors, but slow processing significantly. <code>10</code> is the default,
	 * and produces good color mapping at reasonable speeds. Values greater than <code>20</code> do not yield significant improvements in speed.
	 *
	 * @param quality	Quality (greater than <code>0</code>).
	 */
	public void setQuality(final int quality){
		sample = Math.max(quality, 1);
	}

	/**
	 * Sets the GIF frame size.
	 * The default size is the size of the first frame added if this method is not invoked.
	 *
	 * @param width	Frame width.
	 * @param height	Frame height.
	 * @throws IllegalArgumentException	If with or height are non-positive.
	 */
	public void setSize(final int width, final int height) throws IllegalArgumentException{
		if(width < 1 || height < 1)
			throw new IllegalArgumentException("Width (" + width + ") and height (" + height + ") must be non-negative");

		if(!started || !firstFrameAdded){
			this.width = width;
			this.height = height;
			sizeSet = true;
		}
	}


	/**
	 * Initiates writing of a GIF file with the specified name.
	 *
	 * @param file	String containing output file name.
	 * @return	<code>false</code> if open or initial write failed.
	 */
	public boolean start(final String file){
		boolean response = false;
		try{
			out = new BufferedOutputStream(new FileOutputStream(file));
			response = start(out);
			closeStream = true;
		}
		catch(final IOException e){
			//FIXME
			e.printStackTrace();
		}
		return response;
	}

	/**
	 * Initiates GIF file creation on the given stream.
	 * The stream is not closed automatically.
	 *
	 * @param os	OutputStream on which GIF images are written.
	 * @return	<code>false</code> if initial write failed.
	 */
	public boolean start(final OutputStream os){
		boolean response = false;
		if(os != null){
			closeStream = false;
			out = os;
			try{
				//write GIF header
				writeString("GIF89a");

				response = true;
			}
			catch(final IOException e){
				//FIXME
				e.printStackTrace();
			}
			started = response;
		}
		return response;
	}

	/**
	 * Adds next GIF frame. The frame is not written immediately, but is actually deferred until the next frame is received so that timing
	 * data can be inserted. Invoking <code>finish()</code> flushes all frames. If <code>setSize</code> was not invoked, the size of the
	 * first image is used for all subsequent frames.
	 *
	 * @param im	BufferedImage containing frame to write.
	 * @return	<code>true</code> if successful.
	 */
	public boolean addFrame(final BufferedImage im){
		boolean response = false;
		if(im != null && started){
			try{
				if(!sizeSet)
					//use first frame's size
					setSize(im.getWidth(), im.getHeight());
				image = im;
				//convert to correct format if necessary
				getImagePixels();
				//build color table & map pixels
				analyzePixels();
				if(!firstFrameAdded){
					//logical screen descriptor
					writeLogicalScreenDescriptor();
					//global color table
					writePalette();
					if(repeatCount >= 0)
						//use NS app extension to indicate reps
						writeNetscapeExt();
				}
				//write graphic control extension
				writeGraphicCtrlExt();
				//image descriptor
				writeImageDesc();
				if(firstFrameAdded)
					//local color table
					writePalette();
				//encode and write pixel data
				writePixels();

				firstFrameAdded = true;
				response = true;
			}
			catch(final IOException e){
				//FIXME
				e.printStackTrace();
			}
		}
		return response;
	}

	/**
	 * Flushes any pending data and closes output file.
	 * If writing to an OutputStream, the stream is not closed.
	 *
	 * @return	Whether a closing of a previous stream was completed successfully.
	 */
	public boolean finish(){
		if(!started)
			return false;

		boolean response = false;
		started = false;
		try{
			//GIF trailer
			out.write(0x3b);

			out.flush();
			if(closeStream)
				out.close();

			response = true;
		}
		catch(final IOException e){
			//FIXME
			e.printStackTrace();
		}

		//reset for subsequent use
		transparentIndex = 0;
		out = null;
		image = null;
		pixels = null;
		indexedPixels = null;
		colorTab = null;
		closeStream = false;
		firstFrameAdded = false;

		return response;
	}


	/**
	 * Analyzes image colors and creates color map.
	 */
	private void analyzePixels(){
		final int len = pixels.length;
		final int nPix = len / 3;
		indexedPixels = new byte[nPix];
		final NeuralNetQuantizationAlgorithm nq = new NeuralNetQuantizationAlgorithm(pixels, len, sample);
		// initialize quantizer
		colorTab = nq.process(); // create reduced palette
		// convert map from BGR to RGB
		for(int i = 0; i < colorTab.length; i += 3){
			final byte temp = colorTab[i];
			colorTab[i] = colorTab[i + 2];
			colorTab[i + 2] = temp;
			usedEntry[i / 3] = false;
		}
		// map image pixels to new palette
		int k = 0;
		for(int i = 0; i < nPix; i ++){
			final int index = nq.map(pixels[k ++] & 0xff, pixels[k ++] & 0xff, pixels[k ++] & 0xff);
			usedEntry[index] = true;
			indexedPixels[i] = (byte)index;
		}
		pixels = null;
		colorDepth = 8;
		palSize = 7;
		// get closest match to transparent color if specified
		if(transparent != null)
			transparentIndex = (transparentExactMatch? findExact(transparent): findClosest(transparent));
	}

	/**
	 * Returns index of palette color closest to c
	 *
	 */
	private int findClosest(final Color c){
		if(colorTab == null)
			return -1;

		final int r = c.getRed();
		final int g = c.getGreen();
		final int b = c.getBlue();
		int minpos = 0;
		int dmin = 256 * 256 * 256;
		final int len = colorTab.length;
		for(int i = 0; i < len;){
			final int dr = r - (colorTab[i ++] & 0xff);
			final int dg = g - (colorTab[i ++] & 0xff);
			final int db = b - (colorTab[i] & 0xff);
			final int d = dr * dr + dg * dg + db * db;
			final int index = i / 3;
			if(usedEntry[index] && (d < dmin)){
				dmin = d;
				minpos = index;
			}
			i ++;
		}
		return minpos;
	}

	/*
	 * Returns true if the exact matching color is existing, and used in the color palette, otherwise, return false. This method has to be called before
	 * finishing the image, because after finished the palette is destroyed and it will always return false.
	 */
	boolean isColorUsed(final Color c){
		return findExact(c) != -1;
	}

	/**
	 * Returns index of palette exactly matching to color c or -1 if there is no exact matching.
	 *
	 */
	private int findExact(final Color c){
		if(colorTab == null)
			return -1;

		final int r = c.getRed();
		final int g = c.getGreen();
		final int b = c.getBlue();
		final int len = colorTab.length / 3;
		for(int index = 0; index < len; index ++){
			final int i = index * 3;
			// If the entry is used in colorTab, then check if it is the same exact color we're looking for
			if(usedEntry[index] && r == (colorTab[i] & 0xff) && g == (colorTab[i + 1] & 0xff) && b == (colorTab[i + 2] & 0xff))
				return index;
		}
		return -1;
	}

	/**
	 * Extracts image pixels into byte array "pixels"
	 */
	private void getImagePixels(){
		if(image.getWidth() != width || image.getHeight() != height || image.getType() != BufferedImage.TYPE_3BYTE_BGR){
			//create new image with right size/format
			final BufferedImage tmp = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
			final Graphics2D g = tmp.createGraphics();
			g.setColor(background);
			g.fillRect(0, 0, width, height);
			g.drawImage(image, 0, 0, null);
			image = tmp;
		}
		pixels = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
	}

	/**
	 * Writes Graphic Control Extension
	 */
	private void writeGraphicCtrlExt() throws IOException{
		out.write(0x21); // extension introducer
		out.write(0xf9); // GCE label
		out.write(4); // data block size
		final int transp;
		int disp;
		if(transparent == null){
			transp = 0;
			disp = 0; // dispose = no action
		}
		else{
			transp = 1;
			disp = 2; // force clear if using transparent color
		}
		if(disposalCode >= 0)
			disp = disposalCode & 7; // user override
		disp <<= 2;

		// packed fields
		out.write(0
			| // 1:3 reserved
			disp
			| // 4:6 disposal
			0
			| // 7   user input - 0 = none
			transp); // 8   transparency flag

		writeShort(Math.round(delay / 10.f)); // delay x 1/100 sec
		out.write(transparentIndex); // transparent color index
		out.write(0); // block terminator
	}

	/** Writes Image Descriptor */
	private void writeImageDesc() throws IOException{
		out.write(0x2c); // image separator
		writeShort(0); // image position x,y = 0,0
		writeShort(0);
		writeShort(width); // image size
		writeShort(height);
		//packed fields
		if(!firstFrameAdded)
			//no LCT - GCT is used for first (or only) frame
			out.write(0);
		else
			// specify normal LCT
			out.write(0x80
				| // 1 local color table  1=yes
				0
				| // 2 interlace - 0=no
				0
				| // 3 sorted - 0=no
				0
				| // 4-5 reserved
				palSize); // 6-8 size of color table
	}

	private void writeLogicalScreenDescriptor() throws IOException{
		//logical screen size
		writeShort(width);
		writeShort(height);
		// packed fields
		out.write(
			//1	: global color table flag = 1 (gct used)
			(0x80
			//2-4	: color resolution = 7
			| 0x70
			//5	: gct sort flag = 0
			| 0x00
			//6-8 : gct size
			| palSize));

		//background color index
		out.write(0);
		//pixel aspect ratio - assume 1:1
		out.write(0);
	}

	/**
	 * Writes Netscape application extension to define
	 * repeat count.
	 */
	private void writeNetscapeExt() throws IOException{
		out.write(0x21); // extension introducer
		out.write(0xff); // app extension label
		out.write(11); // block size
		writeString("NETSCAPE" + "2.0"); // app id + auth code
		out.write(3); // sub-block size
		out.write(1); // loop sub-block id
		writeShort(repeatCount); // loop count (extra iterations, 0=repeat forever)
		out.write(0); // block terminator
	}

	/**
	 * Writes color table
	 */
	private void writePalette() throws IOException{
		out.write(colorTab, 0, colorTab.length);
		final int n = (3 * 256) - colorTab.length;
		for(int i = 0; i < n; i ++)
			out.write(0);
	}

	/**
	 * Encodes and writes pixel data
	 */
	private void writePixels() throws IOException{
		final LZWEncoder encoder = new LZWEncoder(width, height, indexedPixels, colorDepth);
		encoder.encode(out);
	}

	/**
	 * Write 16-bit value to output stream, LSB first
	 */
	private void writeShort(final int value) throws IOException{
		out.write(value & 0xff);
		out.write((value >> 8) & 0xff);
	}

	/**
	 * Writes string to output stream
	 */
	private void writeString(final String s) throws IOException{
		for(int i = 0; i < s.length(); i ++)
			out.write((byte)s.charAt(i));
	}

}
