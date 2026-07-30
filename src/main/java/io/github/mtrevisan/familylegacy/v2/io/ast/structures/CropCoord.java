package io.github.mtrevisan.familylegacy.v2.io.ast.structures;


public record CropCoord(Point topLeft, Point bottomRight){
	public record Point(int x, int y){}
}
