package io.github.mtrevisan.familylegacy.services;


public final class FallbackAstronomicalEngine implements AstronomicalEngine{

	@Override
	public boolean isAvailable(){
		return false;
	}

	@Override
	public double getNextNewMoonJdn(final double approxJdn, final double utcOffset){
		throw new UnsupportedOperationException("Astronomical engine plugin not installed.");
	}

	@Override
	public double getSolarLongitudeJdn(final int year, final double targetLongitude, final double utcOffset){
		throw new UnsupportedOperationException("Astronomical engine plugin not installed.");
	}

}
