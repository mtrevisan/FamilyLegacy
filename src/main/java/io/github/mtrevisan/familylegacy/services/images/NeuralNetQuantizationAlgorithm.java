package io.github.mtrevisan.familylegacy.services.images;


/**
 * Neural-Net Quantization Algorithm
 *
 * Copyright (c) 1994 Anthony Dekker
 *
 * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994.
 * See "Kohonen neural networks for optimal colour quantization"
 * in "Network: Computation in Neural Systems" Vol. 5 (1994) pp 351-367.
 * for a discussion of the algorithm.
 *
 * Any party obtaining a copy of these files from the author, directly or
 * indirectly, is granted, free of charge, a full and unrestricted irrevocable,
 * world-wide, paid up, royalty-free, nonexclusive right and license to deal
 * in this software and documentation files (the "Software"), including without
 * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons who receive
 * copies from any such party to do so, with the only requirement being
 * that this copyright notice remain intact.
 *
 * @see <a href="https://github.com/rtyley/animated-gif-lib-for-java">Animated GIF library for Java</a>
 */
public class NeuralNetQuantizationAlgorithm{

	//number of colors used
	private static final int NET_SIZE = 256;

	//four primes near 500 - assume no image has a length so large that it is divisible by all four primes
	private static final int PRIME_1 = 499;
	private static final int PRIME_2 = 491;
	private static final int PRIME_3 = 487;
	private static final int PRIME_4 = 503;

	//minimum size for input image [B]
	private static final int MIN_PICTURE_SIZE = 3 * PRIME_4;

	//network definitions:
	private static final int MAX_NET_POS = NET_SIZE - 1;
	//bias for color values
	private static final int NET_BIAS_SHIFT = 4;
	//number of learning cycles
	private static final int LEARNING_CYCLES = 100;

	//definitions for frequency and bias:
	//bias for fractions
	private static final int int_bias_shift = 16;
	private static final int int_bias = 1 << int_bias_shift;
	//gamma = 1024
	private static final int GAMMA_EXPONENT = 10;
	private static final int GAMMA = (1 << GAMMA_EXPONENT);
	//beta = 1/1024
	private static final int BETA_EXPONENT = 10;
	private static final int BETA = (int_bias >> BETA_EXPONENT);
	private static final int BETA_GAMMA = (int_bias << (GAMMA_EXPONENT - BETA_EXPONENT));

	//definitions for decreasing radius factor:
	/* for 256 cols, radius starts */
	private static final int INIT_RAD = (NET_SIZE >> 3);
	/* at 32. biased by 6 bits */
	private static final int RADIUS_BIAS_EXPONENT = 6;
	private static final int RADIUS_BIAS = (1 << RADIUS_BIAS_EXPONENT);
	/* and decreases by a */
	private static final int INITIAL_RADIUS = (INIT_RAD * RADIUS_BIAS);
	//factor of 1/30 each cycle
	private static final int RADIUS_DECRESE_FACTOR = 30;

	//definitions for decreasing alpha factor:
	//alpha starts at 1
	private static final int ALPHA_BIAS_SHIFT = 10;
	private static final int INITIAL_ALPHA = (1 << ALPHA_BIAS_SHIFT);

	//radbias and alpharadbias used for radpower calculation
	private static final int RAD_BIAS_SHIFT = 8;
	private static final int RAD_BIAS = (1 << RAD_BIAS_SHIFT);
	private static final int ALPHA_RAD_B_SHIFT = (ALPHA_BIAS_SHIFT + RAD_BIAS_SHIFT);
	private static final int ALPHA_RAD_BIAS = (1 << ALPHA_RAD_B_SHIFT);


	/* Types and Global Variables
	-------------------------- */
	/* the input image itself */
	private byte[] imageData;
	/* lengthCount = H*W*3 */
	private int length_count;

	//biased by 10 bits
	private int alphaDecreaseFactor;

	//[1, 30]
	private int samplingFactor;

	//typedef int pixel[4]; /* BGRc */
	private int[][] network = new int[NET_SIZE][];

	/* for network lookup - really 256 */
	private int[] net_index = new int[256];

	private int[] bias = new int[NET_SIZE];
	private int[] frequency = new int[NET_SIZE];
	private int[] rad_power = new int[INIT_RAD];

	/* radpower for precomputation */


	/* Initialise network in range (0,0,0) to (255,255,255) and set parameters */
	public NeuralNetQuantizationAlgorithm(final byte[] imageData, final int len, final int samplingFactor){
		this.imageData = imageData;
		length_count = len;
		this.samplingFactor = samplingFactor;

		for(int i = 0; i < NET_SIZE; i ++){
			int p = (i << (NET_BIAS_SHIFT + 8)) / NET_SIZE;
			network[i] = new int[]{p, p, p, 0};
			//1 / net_size
			frequency[i] = int_bias / NET_SIZE;
		}
	}

	public byte[] colorMap(){
		final byte[] map = new byte[3 * NET_SIZE];
		final int[] index = new int[NET_SIZE];
		for(int i = 0; i < NET_SIZE; i ++)
			index[network[i][3]] = i;
		int k = 0;
		for(int i = 0; i < NET_SIZE; i ++){
			final int j = index[i];
			map[k ++] = (byte)(network[j][0]);
			map[k ++] = (byte)(network[j][1]);
			map[k ++] = (byte)(network[j][2]);
		}
		return map;
	}

	/** Insertion sort of network and building of netindex[0..255] (to do after unbias) */
	public void inxbuild(){
		int previouscol = 0;
		int startpos = 0;
		for(int i = 0; i < NET_SIZE; i ++){
			final int[] p = network[i];
			int smallpos = i;
			int smallval = p[1];
			/* index on g */
 			/* find smallest in i..netsize-1 */
			int[] q;
			for(int j = i + 1; j < NET_SIZE; j ++){
				q = network[j];
				if(q[1] < smallval){
					/* index on g */
					smallpos = j;
					smallval = q[1];
					/* index on g */
				}
			}
			q = network[smallpos];
			/* swap p (i) and q (smallpos) entries */
			if(i != smallpos){
				int j = q[0];
				q[0] = p[0];
				p[0] = j;
				j = q[1];
				q[1] = p[1];
				p[1] = j;
				j = q[2];
				q[2] = p[2];
				p[2] = j;
				j = q[3];
				q[3] = p[3];
				p[3] = j;
			}
			/* smallval entry is now in position i */
			if(smallval != previouscol){
				net_index[previouscol] = (startpos + i) >> 1;
				for(int j = previouscol + 1; j < smallval; j ++)
					net_index[j] = i;
				previouscol = smallval;
				startpos = i;
			}
		}
		net_index[previouscol] = (startpos + MAX_NET_POS) >> 1;
		for(int j = previouscol + 1; j < 256; j ++){
			net_index[j] = MAX_NET_POS;
			/* really 256 */
		}
	}

	/** Main Learning Loop */
	public void learn(){
		if(length_count < MIN_PICTURE_SIZE)
			samplingFactor = 1;
		alphaDecreaseFactor = 30 + ((samplingFactor - 1) / 3);
		final byte[] p = imageData;
		int pix = 0;
		final int lim = length_count;
		int samplepixels = length_count / (3 * samplingFactor);
		int delta = samplepixels / LEARNING_CYCLES;
		int alpha = INITIAL_ALPHA;
		int radius = INITIAL_RADIUS;

		int rad = radius >> RADIUS_BIAS_EXPONENT;
		if(rad <= 1)
			rad = 0;
		for(int i = 0; i < rad; i ++)
			rad_power[i] = alpha * (((rad * rad - i * i) * RAD_BIAS) / (rad * rad));

		//fprintf(stderr,"beginning 1D learning: initial radius=%d\n", rad);
		int step;
		if(length_count < MIN_PICTURE_SIZE)
			step = 1;
		else if((length_count % PRIME_1) != 0)
			step = PRIME_1;
		else if((length_count % PRIME_2) != 0)
			step = PRIME_2;
		else if((length_count % PRIME_3) != 0)
			step = PRIME_3;
		else
			step = PRIME_4;
		step *= 3;

		int i = 0;
		while(i < samplepixels){
			final int b = (p[pix + 0] & 0xff) << NET_BIAS_SHIFT;
			final int g = (p[pix + 1] & 0xff) << NET_BIAS_SHIFT;
			final int r = (p[pix + 2] & 0xff) << NET_BIAS_SHIFT;
			int j = contest(b, g, r);

			altersingle(alpha, j, b, g, r);
			if(rad != 0)
				alterNeighbors(rad, j, b, g, r);

			pix += step;
			if(pix >= lim)
				pix -= length_count;

			i ++;
			if(delta == 0)
				delta = 1;
			if(i % delta == 0){
				alpha -= alpha / alphaDecreaseFactor;
				radius -= radius / RADIUS_DECRESE_FACTOR;
				rad = radius >> RADIUS_BIAS_EXPONENT;
				if(rad <= 1)
					rad = 0;
				for(j = 0; j < rad; j ++)
					rad_power[j] = alpha * (((rad * rad - j * j) * RAD_BIAS) / (rad * rad));
			}
		}
		//fprintf(stderr,"finished 1D learning: final alpha=%f !\n",((float)alpha)/initalpha);
	}

	/* Search for BGR values 0..255 (after net is unbiased) and return colour index */
	public int map(final int b, final int g, final int r){
		int bestd = 1000;
		/* biggest possible dist is 256*3 */
		int best = -1;
		int i = net_index[g];
		/* index on g */
		int j = i - 1;
		/* start at netindex[g] and work outwards */

		while(i < NET_SIZE || j >= 0){
			if(i < NET_SIZE){
				final int[] p = network[i];
				int dist = p[1] - g;
				/* inx key */
				if(dist >= bestd)
					/* stop iter */
					i = NET_SIZE;

				else{
					i ++;
					if(dist < 0)
						dist = -dist;
					int a = p[0] - b;
					if(a < 0)
						a = -a;
					dist += a;
					if(dist < bestd){
						a = p[2] - r;
						if(a < 0)
							a = -a;
						dist += a;
						if(dist < bestd){
							bestd = dist;
							best = p[3];
						}
					}
				}
			}
			if(j >= 0){
				final int[] p = network[j];
				int dist = g - p[1];
				/* inx key - reverse dif */
				if(dist >= bestd)
					/* stop iter */
					j = -1;
				else{
					j --;
					if(dist < 0)
						dist = -dist;
					int a = p[0] - b;
					if(a < 0)
						a = -a;
					dist += a;
					if(dist < bestd){
						a = p[2] - r;
						if(a < 0)
							a = -a;
						dist += a;
						if(dist < bestd){
							bestd = dist;
							best = p[3];
						}
					}
				}
			}
		}
		return (best);
	}

	public byte[] process(){
		learn();
		unbiasnet();
		inxbuild();
		return colorMap();
	}

	/* Unbias network to give byte values 0..255 and record position i to prepare for sort */
	public void unbiasnet(){
		for(int i = 0; i < NET_SIZE; i ++){
			network[i][0] >>= NET_BIAS_SHIFT;
			network[i][1] >>= NET_BIAS_SHIFT;
			network[i][2] >>= NET_BIAS_SHIFT;
			network[i][3] = i;
			/* record colour no */
		}
	}

	/* Move adjacent neurons by precomputed alpha*(1-((i-j)^2/[r]^2)) in radpower[|i-j|] */
	protected void alterNeighbors(final int rad, final int i, final int b, final int g, final int r){
		int lo = i - rad;
		if(lo < -1)
			lo = -1;
		int hi = i + rad;
		if(hi > NET_SIZE)
			hi = NET_SIZE;

		int j = i + 1;
		int k = i - 1;
		int m = 1;
		while((j < hi) || (k > lo)){
			final int a = rad_power[m ++];
			if(j < hi){
				final int[] p = network[j ++];
				try{
					p[0] -= (a * (p[0] - b)) / ALPHA_RAD_BIAS;
					p[1] -= (a * (p[1] - g)) / ALPHA_RAD_BIAS;
					p[2] -= (a * (p[2] - r)) / ALPHA_RAD_BIAS;
				}
				catch(Exception e){} // prevents 1.3 miscompilation
			}
			if(k > lo){
				final int[] p = network[k --];
				try{
					p[0] -= (a * (p[0] - b)) / ALPHA_RAD_BIAS;
					p[1] -= (a * (p[1] - g)) / ALPHA_RAD_BIAS;
					p[2] -= (a * (p[2] - r)) / ALPHA_RAD_BIAS;
				}
				catch(final Exception e){
					//FIXME
				}
			}
		}
	}

	/* Move neuron i towards biased (b,g,r) by factor alpha */
	protected void altersingle(final int alpha, final int i, final int b, final int g, final int r){
		/* alter hit neuron */
		final int[] n = network[i];
		n[0] -= (alpha * (n[0] - b)) / INITIAL_ALPHA;
		n[1] -= (alpha * (n[1] - g)) / INITIAL_ALPHA;
		n[2] -= (alpha * (n[2] - r)) / INITIAL_ALPHA;
	}

	/* Search for biased BGR values */
	protected int contest(final int b, final int g, final int r){
		/* finds closest neuron (min dist) and updates freq */
		 /* finds best neuron (min dist-bias) and returns position */
		 /* for frequently chosen neurons, freq[i] is high and bias[i] is negative */
		 /* bias[i] = gamma*((1/netsize)-freq[i]) */
		int bestd = ~(1 << 31);
		int bestbiasd = bestd;
		int bestpos = -1;
		int bestbiaspos = bestpos;

		for(int i = 0; i < NET_SIZE; i ++){
			final int[] p = network[i];
			int dist = p[0] - b;
			if(dist < 0)
				dist = -dist;
			int a = p[1] - g;
			if(a < 0)
				a = -a;
			dist += a;
			a = p[2] - r;
			if(a < 0)
				a = -a;
			dist += a;
			if(dist < bestd){
				bestd = dist;
				bestpos = i;
			}
			final int biasdist = dist - ((bias[i]) >> (int_bias_shift - NET_BIAS_SHIFT));
			if(biasdist < bestbiasd){
				bestbiasd = biasdist;
				bestbiaspos = i;
			}
			final int betafreq = (frequency[i] >> BETA_EXPONENT);
			frequency[i] -= betafreq;
			bias[i] += (betafreq << GAMMA_EXPONENT);
		}
		frequency[bestpos] += BETA;
		bias[bestpos] -= BETA_GAMMA;
		return (bestbiaspos);
	}

}
