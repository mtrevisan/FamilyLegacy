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

import java.io.IOException;
import java.io.OutputStream;


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

	// for dynamic table sizing
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
	static final int MAX_BITS = BITS; // user settable max # bits/code
	int maxcode; // maximum code, given numberOfBits
	static final int MAX_MAX_CODE = 1 << BITS; // should NEVER generate this code

	final int[] htab = new int[HSIZE];
	final int[] codetab = new int[HSIZE];

	int freeEntry; // first unused entry

	// block compression parameters -- after all codes are used up,
	// and compression rate changes, start over.
	boolean clearFlag;

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
	int cur_accum;
	int cur_bits;

	final int[] masks = {
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
	final byte[] accum = new byte[256];

	//----------------------------------------------------------------------------
	LZWEncoder(final int width, final int height, final byte[] pixels, final int colorDepth){
		imgW = width;
		imgH = height;
		pixAry = pixels;
		initCodeSize = Math.max(2, colorDepth);
	}

	// Add a character to the end of the current packet, and if it is 254
	// characters, flush the packet to disk.
	void charOut(final byte c, final OutputStream outs) throws IOException{
		accum[a_count ++] = c;
		if(a_count >= 254)
			flushChar(outs);
	}

	// Clear out the hash table
	// table clear for block compress
	void clearBlock(final OutputStream outs) throws IOException{
		clearHash(HSIZE);
		freeEntry = clearCode + 2;
		clearFlag = true;

		output(clearCode, outs);
	}

	// reset code table
	void clearHash(final int hsize){
		for(int i = 0; i < hsize; i ++)
			htab[i] = -1;
	}

	void compress(final int initBits, final OutputStream outs) throws IOException{
		int fcode;
		int i /* = 0 */;
		int c;
		int ent;
		int disp;
		final int hsize_reg;
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
		for(fcode = HSIZE; fcode < 65536; fcode *= 2)
			 ++ hshift;
		hshift = 8 - hshift; // set hash code range bound

		hsize_reg = HSIZE;
		clearHash(hsize_reg); // clear hash table

		output(clearCode, outs);

		outer_loop:
		while((c = nextPixel()) != EOF){
			fcode = (c << MAX_BITS) + ent;
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
			if(freeEntry < MAX_MAX_CODE){
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
	void encode(final OutputStream os) throws IOException{
		os.write(initCodeSize); // write "initial code size" byte

		remaining = imgW * imgH; // reset navigation variables
		curPixel = 0;

		compress(initCodeSize + 1, os); // compress and write the pixel data

		os.write(0); // write block terminator
	}

	// Flush the packet to disk, and reset the accumulator
	void flushChar(final OutputStream outs) throws IOException{
		if(a_count > 0){
			outs.write(a_count);
			outs.write(accum, 0, a_count);
			a_count = 0;
		}
	}

	final int MAXCODE(final int numberOfBits){
		return (1 << numberOfBits) - 1;
	}

	//----------------------------------------------------------------------------
	// Return the next pixel from the image
	//----------------------------------------------------------------------------
	private int nextPixel(){
		if(remaining == 0)
			return EOF;

		 -- remaining;

		final byte pix = pixAry[curPixel ++];

		return pix & 0xff;
	}

	void output(final int code, final OutputStream outs) throws IOException{
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
				if(numberOfBits == MAX_BITS)
					maxcode = MAX_MAX_CODE;
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
