package io.github.mtrevisan.familylegacy.v2.services;


public final class FallbackAstronomicalEngine implements AstronomicalEngine{

	@Override
	public boolean isAvailable(){
		return false;
	}

	@Override
	public long getNextNewMoonJdn(final double approxJdn, final double utcOffset){
		throw new UnsupportedOperationException("Astronomical engine plugin not installed.");
	}

	@Override
	public double getSolarLongitudeJdn(final int year, final double targetLongitude, final double utcOffset){
		throw new UnsupportedOperationException("Astronomical engine plugin not installed.");
	}

}
