package io.github.mtrevisan.familylegacy.services.images;

import java.io.OutputStream;
import java.io.IOException;


/**
 * Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott. K Weiner 12/00
 *
 * @see <a href="https://github.com/rtyley/animated-gif-lib-for-java">Animated GIF library for Java</a>
 */
public class LZWEncoder{

	private static final int EOF = -1;

	// GIFCOMPR.C - GIF Image compression routines
	//
	// Lempel-Ziv compression based on 'compress'. GIF modifications by
	// David Rowley (mgardi@watdcsu.waterloo.edu)
	// General DEFINEs
	private static final int BITS = 12;

	private static final int HSIZE = 5003; // 80% occupancy


	private final int imgW;
	private final int imgH;
	private final byte[] pixAry;
	private final int initCodeSize;
	private int remaining;
	private int curPixel;

	// GIF Image compression - modified 'compress'
	//
	// Based on: compress.c - File compression ala IEEE Computer, June 1984.
	//
	// By Authors:  Spencer W. Thomas      (decvax!harpo!utah-cs!utah-gr!thomas)
	//              Jim McKie              (decvax!mcvax!jim)
	//              Steve Davies           (decvax!vax135!petsd!peora!srd)
	//              Ken Turkowski          (decvax!decwrl!turtlevax!ken)
	//              James A. Woods         (decvax!ihnp4!ames!jaw)
	//              Joe Orost              (decvax!vax135!petsd!joe)
	int numberOfBits; // number of bits/code
	int maxbits = BITS; // user settable max # bits/code
	int maxcode; // maximum code, given numberOfBits
	int maxmaxcode = 1 << BITS; // should NEVER generate this code

	int[] htab = new int[HSIZE];
	int[] codetab = new int[HSIZE];

	int hsize = HSIZE; // for dynamic table sizing

	int freeEntry = 0; // first unused entry

	// block compression parameters -- after all codes are used up,
	// and compression rate changes, start over.
	boolean clearFlag = false;

	// Algorithm: use open addressing double hashing (no chaining) on the
	// prefix code / next character combination. We do a variant of Knuth's
	// algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
	// secondary probe. Here, the modular division first probe is gives way
	// to a faster exclusive-or manipulation. Also do block compression with
	// an adaptive reset, whereby the code table is cleared when the compression
	// ratio decreases, but after the table fills. The variable-length output
	// codes are re-sized at this point, and a special CLEAR code is generated
	// for the decompressor. Late addition: construct the table according to
	// file size for noticeable speed improvement on small files. Please direct
	// questions about this implementation to ames!jaw.
	int g_init_bits;

	int clearCode;
	int eofCode;

	// output
	//
	// Output the given code.
	// Inputs:
	//      code:   A numberOfBits-bit integer.  If == -1, then EOF.  This assumes
	//              that numberOfBits =< wordsize - 1.
	// Outputs:
	//      Outputs code to the file.
	// Assumptions:
	//      Chars are 8 bits long.
	// Algorithm:
	//      Maintain a BITS character long buffer (so that 8 codes will
	// fit in it exactly).  Use the VAX insv instruction to insert each
	// code in turn.  When the buffer fills up empty it and start over.
	int cur_accum = 0;
	int cur_bits = 0;

	int masks[]
		= {
			0x0000,
			0x0001,
			0x0003,
			0x0007,
			0x000F,
			0x001F,
			0x003F,
			0x007F,
			0x00FF,
			0x01FF,
			0x03FF,
			0x07FF,
			0x0FFF,
			0x1FFF,
			0x3FFF,
			0x7FFF,
			0xFFFF};

	// Number of characters so far in this 'packet'
	int a_count;

	// Define the storage for the packet accumulator
	byte[] accum = new byte[256];

	//----------------------------------------------------------------------------
	LZWEncoder(int width, int height, byte[] pixels, int colorDepth){
		imgW = width;
		imgH = height;
		pixAry = pixels;
		initCodeSize = Math.max(2, colorDepth);
	}

	// Add a character to the end of the current packet, and if it is 254
	// characters, flush the packet to disk.
	void charOut(byte c, OutputStream outs) throws IOException{
		accum[a_count ++] = c;
		if(a_count >= 254)
			flushChar(outs);
	}

	// Clear out the hash table
	// table clear for block compress
	void clearBlock(OutputStream outs) throws IOException{
		clearHash(hsize);
		freeEntry = clearCode + 2;
		clearFlag = true;

		output(clearCode, outs);
	}

	// reset code table
	void clearHash(int hsize){
		for(int i = 0; i < hsize; i ++)
			htab[i] = -1;
	}

	void compress(int initBits, OutputStream outs) throws IOException{
		int fcode;
		int i /* = 0 */;
		int c;
		int ent;
		int disp;
		int hsize_reg;
		int hshift;

		// Set up the globals: g_init_bits - initial number of bits
		g_init_bits = initBits;

		// Set up the necessary values
		clearFlag = false;
		numberOfBits = g_init_bits;
		maxcode = MAXCODE(numberOfBits);

		clearCode = 1 << (initBits - 1);
		eofCode = clearCode + 1;
		freeEntry = clearCode + 2;

		a_count = 0; // clear packet

		ent = nextPixel();

		hshift = 0;
		for(fcode = hsize; fcode < 65536; fcode *= 2)
			 ++ hshift;
		hshift = 8 - hshift; // set hash code range bound

		hsize_reg = hsize;
		clearHash(hsize_reg); // clear hash table

		output(clearCode, outs);

		outer_loop:
		while((c = nextPixel()) != EOF){
			fcode = (c << maxbits) + ent;
			i = (c << hshift) ^ ent; // xor hashing

			if(htab[i] == fcode){
				ent = codetab[i];
				continue;
			}
			else if(htab[i] >= 0){ // non-empty slot
				disp = hsize_reg - i; // secondary hash (after G. Knott)
				if(i == 0)
					disp = 1;
				do{
					if((i -= disp) < 0)
						i += hsize_reg;

					if(htab[i] == fcode){
						ent = codetab[i];
						continue outer_loop;
					}
				}
				while(htab[i] >= 0);
			}
			output(ent, outs);
			ent = c;
			if(freeEntry < maxmaxcode){
				codetab[i] = freeEntry ++; // code -> hashtable
				htab[i] = fcode;
			}
			else
				clearBlock(outs);
		}
		// Put out the final code.
		output(ent, outs);
		output(eofCode, outs);
	}

	//----------------------------------------------------------------------------
	void encode(OutputStream os) throws IOException{
		os.write(initCodeSize); // write "initial code size" byte

		remaining = imgW * imgH; // reset navigation variables
		curPixel = 0;

		compress(initCodeSize + 1, os); // compress and write the pixel data

		os.write(0); // write block terminator
	}

	// Flush the packet to disk, and reset the accumulator
	void flushChar(OutputStream outs) throws IOException{
		if(a_count > 0){
			outs.write(a_count);
			outs.write(accum, 0, a_count);
			a_count = 0;
		}
	}

	final int MAXCODE(int numberOfBits){
		return (1 << numberOfBits) - 1;
	}

	//----------------------------------------------------------------------------
	// Return the next pixel from the image
	//----------------------------------------------------------------------------
	private int nextPixel(){
		if(remaining == 0)
			return EOF;

		 -- remaining;

		byte pix = pixAry[curPixel ++];

		return pix & 0xff;
	}

	void output(int code, OutputStream outs) throws IOException{
		cur_accum &= masks[cur_bits];

		if(cur_bits > 0)
			cur_accum |= (code << cur_bits);
		else
			cur_accum = code;

		cur_bits += numberOfBits;

		while(cur_bits >= 8){
			charOut((byte)(cur_accum & 0xff), outs);
			cur_accum >>= 8;
			cur_bits -= 8;
		}

		// If the next entry is going to be too big for the code size,
		// then increase it, if possible.
		if(freeEntry > maxcode || clearFlag){
			if(clearFlag){
				maxcode = MAXCODE(numberOfBits = g_init_bits);
				clearFlag = false;
			}
			else{
				 ++ numberOfBits;
				if(numberOfBits == maxbits)
					maxcode = maxmaxcode;
				else
					maxcode = MAXCODE(numberOfBits);
			}
		}

		if(code == eofCode){
			// At EOF, write the rest of the buffer.
			while(cur_bits > 0){
				charOut((byte)(cur_accum & 0xff), outs);
				cur_accum >>= 8;
				cur_bits -= 8;
			}

			flushChar(outs);
		}
	}

}