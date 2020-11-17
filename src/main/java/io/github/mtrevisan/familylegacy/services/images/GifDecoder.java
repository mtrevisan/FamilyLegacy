package io.github.mtrevisan.familylegacy.services.images;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 *
 * @see <a href="https://github.com/rtyley/animated-gif-lib-for-java">Animated GIF library for Java</a>
 */
public class GifDecoder{

	public static final int FILE_READ_STATUS_OK = 0;
	/** Error decoding file (may be partially decoded) */
	public static final int FILE_READ_STATUS_FORMAT_ERROR = 1;
	/** Unable to open source */
	public static final int FILE_READ_STATUS_OPEN_ERROR = 2;

	private static final int MAX_DECODER_PIXEL_STACK_SIZE = 4096;


	private BufferedInputStream in;
	private int status;

	//full image width
	private int width;
	//full image height
	private int height;
	private boolean globalColorTableFlag;
	private int globalColorTableSize;
	//iterations (0 = repeat forever)
	private int loopCount = 1;

	private int[] globalColorTable;
	private int[] localColorTable;
	private int[] activeColorTable;

	private int backgroundColorIndex;
	private int backgroundColor;
	private int lastBackgroundColor;
	private int pixelAspectRatio;

	private boolean localColorTableFlag;
	private int localColorTableSize;
	private boolean interlaceFlag;

	private int ix, iy, iw, ih; // current image rectangle
	private Rectangle lastFrameRect;
	private BufferedImage currentFrame;
	private BufferedImage lastFrame;

	//current data block
	private final byte[] block = new byte[256];
	//block size
	private int blockSize;

	//last graphic control extension info
	private int dispose;
	//0 = no action; 1 = leave in place; 2 = restore to background; 3 = restore to prev
	private int lastDispose;
	//use transparent color
	private boolean transparency;
	//[ms]
	private int delay;
	private int transparentColorIndex;

	//LZW decoder working arrays
	private short[] prefix;
	private byte[] suffix;
	private byte[] pixelStack;
	private byte[] pixels;

	//frames read from current file
	private List frames;
	private int frameCount;


	private static class GifFrame{

		private final BufferedImage image;
		private final int delay;

		public GifFrame(BufferedImage im, int delay){
			image = im;
			this.delay = delay;
		}

	}

	/**
	 * Gets display duration for specified frame
	 *
	 * @param n	index of frame
	 * @return	delay [ms]
	 */
	public int getDelay(int n){
		//
		delay = -1;
		if((n >= 0) && (n < frameCount)){
			delay = ((GifFrame)frames.get(n)).delay;
		}
		return delay;
	}

	/**
	 * Gets the number of frames read from file
	 *
	 * @return	frame count
	 */
	public int getFrameCount(){
		return frameCount;
	}

	/**
	 * Gets the first (or only) image read.
	 *
	 * @return	BufferedImage containing first frame, or <code>null</code> if none.
	 */
	public BufferedImage getImage(){
		return getFrame(0);
	}

	/**
	 * Gets the "Netscape" iteration count, if any.
	 * A count of <code>0</code> means repeat indefinitiely.
	 *
	 * @return	iteration count if one was specified, else <code>1</code>.
	 */
	public int getLoopCount(){
		return loopCount;
	}

	/** Creates new frame image from current data (and previous frames as specified by their disposition codes) */
	private void setPixels(){
		//expose destination image's pixels as int array
		int[] dest = ((DataBufferInt)currentFrame.getRaster().getDataBuffer()).getData();

		//fill in starting image contents based on last image's dispose code
		if(lastDispose > 0){
			if(lastDispose == 3){
				//use image before last
				int n = frameCount - 2;
				if(n > 0)
					lastFrame = getFrame(n - 1);
				else
					lastFrame = null;
			}

			if(lastFrame != null){
				int[] prev = ((DataBufferInt)lastFrame.getRaster().getDataBuffer()).getData();
				System.arraycopy(prev, 0, dest, 0, width * height);

				if(lastDispose == 2){
					Color c;
					if(transparency)
						//assume background is transparent
						c = new Color(0, 0, 0, 0);
					else
						//use given background color
						c = new Color(lastBackgroundColor);

					//fill last image rect area with background color
					Graphics2D g = currentFrame.createGraphics();
					g.setColor(c);
					//replace area
					g.setComposite(AlphaComposite.Src);
					g.fill(lastFrameRect);
					g.dispose();
				}
			}
		}

		//copy each source line to the appropriate place in the destination
		int pass = 1;
		int inc = 8;
		int iline = 0;
		for(int i = 0; i < ih; i ++){
			int line = i;
			if(interlaceFlag){
				if(iline >= ih){
					pass ++;
					switch(pass){
						case 2:
							iline = 4;
							break;

						case 3:
							iline = 2;
							inc = 4;
							break;

						case 4:
							iline = 1;
							inc = 2;
					}
				}
				line = iline;
				iline += inc;
			}
			line += iy;
			if(line < height){
				int k = line * width;
				//start of line in dest
				int dx = k + ix;
				//end of dest line
				int dlim = dx + iw;
				if((k + width) < dlim)
					//past dest edge
					dlim = k + width;
				//start of line in source
				int sx = i * iw;
				while(dx < dlim){
					//map color and insert in destination
					int index = ((int)pixels[sx ++]) & 0xff;
					int c = activeColorTable[index];
					if(c != 0)
						dest[dx] = c;
					dx ++;
				}
			}
		}
	}

	/**
	 * Gets the image contents of frame <code>n</code>.
	 *
	 * @param n	Frame index
	 * @return	BufferedImage representation of frame, or <code>null</code> if <code>n</code> is invalid.
	 */
	public BufferedImage getFrame(int n){
		BufferedImage im = null;
		if(n >= 0 && n < frameCount)
			im = ((GifFrame)frames.get(n)).image;
		return im;
	}

	/**
	 * Gets image size.
	 *
	 * @return	GIF image dimensions
	 */
	public Dimension getFrameSize(){
		return new Dimension(width, height);
	}

	/**
	 * Reads GIF image from stream
	 *
	 * @param is	InputStream containing GIF file.
	 * @return	read status code (<code>0</code> = no errors)
	 */
	public int read(InputStream is){
		if(is != null){
			status = FILE_READ_STATUS_OK;
			in = (is instanceof BufferedInputStream? (BufferedInputStream)is: new BufferedInputStream(is));

			init();

			readHeader();

			if(!hasErrors()){
				readContents();

				if(frameCount < 0)
					status = FILE_READ_STATUS_FORMAT_ERROR;
			}

			try{
				is.close();
			}
			catch(IOException e){}
		}
		else
			status = FILE_READ_STATUS_OPEN_ERROR;

		return status;
	}

	/**
	 * Reads GIF file from specified file/URL source.
	 * (URL assumed if name contains <code>":/"</code> or <code>"file:"</code>)
	 *
	 * @param name	String containing source
	 * @return	read status code (<code>0</code> = no errors)
	 */
	public int read(String name){
		try{
			status = FILE_READ_STATUS_OK;
			name = name.trim().toLowerCase();
			if(name.startsWith("file:") || name.contains(":/")){
				URL url = new URL(name);
				in = new BufferedInputStream(url.openStream());
			}
			else
				in = new BufferedInputStream(new FileInputStream(name));
			status = read(in);
		}
		catch(IOException e){
			status = FILE_READ_STATUS_OPEN_ERROR;
		}
		return status;
	}

	/**
	 * Decodes LZW image data into pixel array.
	 * Adapted from John Cristy's ImageMagick.
	 */
	private void decodeImageData(){
		int nullCode = -1;
		int npix = iw * ih;

		if((pixels == null) || (pixels.length < npix))
			pixels = new byte[npix]; // allocate new pixel array
		if(prefix == null)
			prefix = new short[MAX_DECODER_PIXEL_STACK_SIZE];
		if(suffix == null)
			suffix = new byte[MAX_DECODER_PIXEL_STACK_SIZE];
		if(pixelStack == null)
			pixelStack = new byte[MAX_DECODER_PIXEL_STACK_SIZE + 1];

		//initialize GIF data stream decoder:
		int dataSize = read();
		int clear = 1 << dataSize;
		int endOfInformation = clear + 1;
		int available = clear + 2;
		int oldCode = nullCode;
		int codeSize = dataSize + 1;
		int codeMask = (1 << codeSize) - 1;
		for(int code = 0; code < clear; code ++){
			prefix[code] = 0;
			suffix[code] = (byte)code;
		}

		//decode GIF pixel stream
		int datum = 0;
		int bits = 0;
		int count = 0;
		int first = 0;
		int top = 0;
		int pi = 0;
		int bi = 0;
		for(int i = 0; i < npix; ){
			if(top == 0){
				if(bits < codeSize){
					//load bytes until there are enough bits for a code
					if(count == 0){
						// Read a new data block.
						count = readBlock();
						if(count <= 0)
							break;

						bi = 0;
					}
					datum += (((int)block[bi]) & 0xff) << bits;
					bits += 8;
					bi ++;
					count --;
					continue;
				}

				//get the next code:
				int code = datum & codeMask;
				datum >>= codeSize;
				bits -= codeSize;

				//interpret the code
				if(code > available || code == endOfInformation)
					break;

				if(code == clear){
					//reset decoder
					codeSize = dataSize + 1;
					codeMask = (1 << codeSize) - 1;
					available = clear + 2;
					oldCode = nullCode;
					continue;
				}
				if(oldCode == nullCode){
					pixelStack[top ++] = suffix[code];
					oldCode = code;
					first = code;
					continue;
				}
				int inCode = code;
				if(code == available){
					pixelStack[top ++] = (byte)first;
					code = oldCode;
				}
				while(code > clear){
					pixelStack[top ++] = suffix[code];
					code = prefix[code];
				}
				first = ((int)suffix[code]) & 0xff;

				//add a new string to the string table,
				if(available >= MAX_DECODER_PIXEL_STACK_SIZE){
					pixelStack[top ++] = (byte)first;
					continue;
				}
				pixelStack[top ++] = (byte)first;
				prefix[available] = (short)oldCode;
				suffix[available] = (byte)first;
				available ++;
				if((available & codeMask) == 0 && available < MAX_DECODER_PIXEL_STACK_SIZE){
					codeSize ++;
					codeMask += available;
				}
				oldCode = inCode;
			}

			//pop a pixel off the pixel stack:
			top --;
			pixels[pi ++] = pixelStack[top];
			i ++;
		}

		//clear missing pixels
		for(int i = pi; i < npix; i ++)
			pixels[i] = 0;
	}

	/** Returns <code>true</code> if an error was encountered during reading/decoding */
	private boolean hasErrors(){
		return (status != FILE_READ_STATUS_OK);
	}

	/** Initializes or re-initializes reader */
	private void init(){
		status = FILE_READ_STATUS_OK;
		frameCount = 0;
		frames = new ArrayList();
		globalColorTable = null;
		localColorTable = null;
	}

	/** Reads a single byte from the input stream */
	private int read(){
		int currentByte = 0;
		try{
			currentByte = in.read();
		}
		catch(IOException e){
			status = FILE_READ_STATUS_FORMAT_ERROR;
		}
		return currentByte;
	}

	/**
	 * Reads next variable length block from input.
	 *
	 * @return	number of bytes stored in "buffer"
	 */
	private int readBlock(){
		blockSize = read();
		int n = 0;
		if(blockSize > 0){
			try{
				while(n < blockSize){
					int count = in.read(block, n, blockSize - n);
					if(count == -1)
						break;

					n += count;
				}
			}
			catch(IOException e){}

			if(n < blockSize)
				status = FILE_READ_STATUS_FORMAT_ERROR;
		}
		return n;
	}

	/**
	 * Reads color table as 256 RGB integer values
	 *
	 * @param numberOfColors	number of colors to read
	 * @return	int array containing 256 colors (packed ARGB with full alpha)
	 */
	private int[] readColorTable(int numberOfColors){
		int nbytes = 3 * numberOfColors;
		byte[] c = new byte[nbytes];
		int n = 0;
		try{
			n = in.read(c);
		}
		catch(IOException e){}

		int[] tab = null;
		if(n < nbytes)
			status = FILE_READ_STATUS_FORMAT_ERROR;
		else{
			//max size to avoid bounds checks
			tab = new int[256];
			int i = 0;
			int j = 0;
			while(i < numberOfColors){
				int r = ((int)c[j ++]) & 0xff;
				int g = ((int)c[j ++]) & 0xff;
				int b = ((int)c[j ++]) & 0xff;
				tab[i ++] = 0xff000000 | (r << 16) | (g << 8) | b;
			}
		}
		return tab;
	}

	/**
	 * Main file parser.
	 * Reads GIF content blocks.
	 */
	private void readContents(){
		//read GIF file content blocks
		boolean done = false;
		while(!done && !hasErrors()){
			int code = read();
			switch(code){
				//image separator
				case 0x2C:
					readNextFrame();
					break;

					//extension
				case 0x21:
					code = read();
					switch(code){
						//graphics control extension
						case 0xF9:
							readGraphicControlExtension();
							break;

						//application extension
						case 0xFF:
							readBlock();
							String app = "";
							for(int i = 0; i < 11; i ++)
								app += (char)block[i];
							if(app.equals("NETSCAPE2.0"))
								readNetscapeExtension();
							else
								//don't care
								skip();
							break;

						default:
							//uninteresting extension
							skip();
					}
					break;

				//terminator
				case 0x3B:
					done = true;
					break;

				//bad byte, keep going and see what happens
				case 0x00:
					break;

				default:
					status = FILE_READ_STATUS_FORMAT_ERROR;
			}
		}
	}

	/** Reads Graphics Control Extension values */
	private void readGraphicControlExtension(){
		//block size
		read();

		//packed fields
		int packed = read();
		//disposal method
		dispose = (packed & 0x1c) >> 2;
		if(dispose == 0)
			//elect to keep old image if discretionary
			dispose = 1;
		transparency = (packed & 1) != 0;
		//delay [ms]
		delay = readShort() * 10;
		//transparent color index
		transparentColorIndex = read();

		//block terminator
		read();
	}

	/** Reads GIF file header information */
	private void readHeader(){
		String id = "";
		for(int i = 0; i < 6; i ++)
			id += (char)read();
		if(!id.startsWith("GIF")){
			status = FILE_READ_STATUS_FORMAT_ERROR;
			return;
		}

		readLogicalScreenDescriptor();
		if(globalColorTableFlag && !hasErrors()){
			globalColorTable = readColorTable(globalColorTableSize);
			backgroundColor = globalColorTable[backgroundColorIndex];
		}
	}

	private void readNextFrame(){
		//(sub)image position & size
		ix = readShort();
		iy = readShort();
		iw = readShort();
		ih = readShort();

		int packed = read();
		//1 - local color table flag
		localColorTableFlag = ((packed & 0x80) != 0);
		//2 - interlace flag
		interlaceFlag = ((packed & 0x40) != 0);
		//3 - sort flag
		//4-5 - reserved
		//6-8 - local color table size
		localColorTableSize = 2 << (packed & 7);

		if(localColorTableFlag){
			//read table
			localColorTable = readColorTable(localColorTableSize);
			//make local table active
			activeColorTable = localColorTable;
		}
		else{
			//make global table active
			activeColorTable = globalColorTable;
			if(backgroundColorIndex == transparentColorIndex)
				backgroundColor = 0;
		}
		int save = 0;
		if(transparency){
			save = activeColorTable[transparentColorIndex];
			//set transparent color if specified
			activeColorTable[transparentColorIndex] = 0;
		}

		if(activeColorTable == null)
			//no color table defined
			status = FILE_READ_STATUS_FORMAT_ERROR;

		if(hasErrors())
			return;

		//decode pixel data
		decodeImageData();

		skip();

		if(hasErrors())
			return;

		frameCount ++;

		//create new image to receive frame data
		currentFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

		//transfer pixel data to image
		setPixels();

		//add image to frame list
		frames.add(new GifFrame(currentFrame, delay));

		if(transparency)
			activeColorTable[transparentColorIndex] = save;
		resetFrame();
	}

	private void readLogicalScreenDescriptor(){
		//logical screen size
		width = readShort();
		height = readShort();

		//packed fields
		int packed = read();
		//1	: global color table flag
		globalColorTableFlag = ((packed & 0x80) != 0);
		//2-4	: color resolution
		//5	: gct sort flag
		//6-8	: gct size
		globalColorTableSize = 2 << (packed & 7);

		//background color index
		backgroundColorIndex = read();
		//pixel aspect ratio
		pixelAspectRatio = read();
	}

	/** Reads Netscape extension to obtain iteration count */
	private void readNetscapeExtension(){
		do{
			readBlock();
			if(block[0] == 1){
				//loop count sub-block
				int b1 = ((int)block[1]) & 0xff;
				int b2 = ((int)block[2]) & 0xff;
				loopCount = (b2 << 8) | b1;
			}
		}while(blockSize > 0 && !hasErrors());
	}

	/** Reads next 16-bit value, LSB first */
	private int readShort(){
		//read 16-bit value, LSB first
		return (read() | (read() << 8));
	}

	/** Resets frame state for reading next image */
	private void resetFrame(){
		lastDispose = dispose;
		lastFrameRect = new Rectangle(ix, iy, iw, ih);
		lastFrame = currentFrame;
		lastBackgroundColor = backgroundColor;
		dispose = 0;
		transparency = false;
		delay = 0;
		localColorTable = null;
	}

	/** Skips variable length blocks up to and including next zero length block */
	private void skip(){
		do{
			readBlock();
		}while(blockSize > 0 && !hasErrors());
	}

}
