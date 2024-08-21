/**
 * Copyright (c) 2024 Mauro Trevisan
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
package io.github.mtrevisan.familylegacy.flef.ui.helpers.images;

//import org.opencv.core.*;
//import org.opencv.imgcodecs.Imgcodecs;
//import org.opencv.imgproc.Imgproc;


public class ImageProjectionTypeDetector{

//	static{
//		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
//	}
//
//	public static void main(String[] args){
//		String imagePath = "path_to_your_image.jpg"; // Inserisci il percorso dell'immagine da analizzare
//		Mat image = Imgcodecs.imread(imagePath);
//
//		if(image.empty()){
//			System.err.println("Errore: Impossibile leggere l'immagine " + imagePath);
//			return;
//		}
//
//		// Converti l'immagine in scala di grigi
//		Mat grayImage = new Mat();
//		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
//
//		// Applica un filtro di smoothing per ridurre il rumore
//		Imgproc.GaussianBlur(grayImage, grayImage, new Size(5, 5), 0);
//
//		// Rilevamento dei bordi con Canny edge detection
//		Mat edges = new Mat();
//		Imgproc.Canny(grayImage, edges, 50, 150);
//
//		// Rilevamento delle linee con trasformata di Hough
//		Mat lines = new Mat();
//		Imgproc.HoughLinesP(edges, lines, 1, Math.PI / 180, 50, 50, 10);
//
//		// Analisi delle linee rilevate per determinare la geometria
//		final List<Line> detectedLines = new ArrayList<>();
//		for(int i = 0; i < lines.rows(); i++){
//			final double[] line = lines.get(i, 0);
//			final Point pt1 = new Point(line[0], line[1]);
//			final Point pt2 = new Point(line[2], line[3]);
//			detectedLines.add(new Line(pt1, pt2));
//		}
//
//		// Classificazione del tipo di proiezione basata sulle linee rilevate
//		ProjectionType projectionType = classifyProjectionType(detectedLines);
//
//		if(projectionType == ProjectionType.UNKNOWN)
//			projectionType = simpleClassifyProjectionType(imagePath);
//
//		// Output del risultato
//		System.out.println("Tipo di proiezione dell'immagine: " + projectionType);
//	}
//
//	// Metodo per classificare il tipo di proiezione basato sulle linee rilevate
//	private static ProjectionType classifyProjectionType(final List<Line> lines){
//		// Esempio di logica semplificata per la classificazione
//		if(lines.size() < 2){
//			return ProjectionType.UNKNOWN; // Non abbastanza linee per determinare la proiezione
//		}
//
//		// Conta i gruppi di linee parallele
//		int verticalCount = 0;
//		int horizontalCount = 0;
//		int diagonalCount = 0;
//
//		for(int i = 0; i < lines.size(); i++){
//			Line line1 = lines.get(i);
//			boolean isHorizontal = false;
//			boolean isVertical = false;
//
//			for(int j = i + 1; j < lines.size(); j++){
//				Line line2 = lines.get(j);
//				double angle = Math.abs(angleBetweenLines(line1, line2));
//
//				if(angle < 10 || angle > 170){
//					isHorizontal = true;
//				}
//				else if(angle > 80 && angle < 100){
//					isVertical = true;
//				}
//			}
//
//			if(isHorizontal){
//				horizontalCount++;
//			}
//			else if(isVertical){
//				verticalCount++;
//			}
//			else{
//				diagonalCount++;
//			}
//		}
//
//		// Classificazione basata sui conteggi
//		if(verticalCount > horizontalCount && verticalCount > diagonalCount){
//			return ProjectionType.CYLINDRICAL;
//		}
//		else if(horizontalCount > verticalCount && horizontalCount > diagonalCount){
//			return ProjectionType.SPHERICAL;
//		}
//		else{
//			return ProjectionType.UNKNOWN;
//		}
//	}
//
//	private static ProjectionType simpleClassifyProjectionType(String imagePath){
//		Mat image = Imgcodecs.imread(imagePath);
//
//		if(image.empty()){
//			System.err.println("Errore: Impossibile leggere l'immagine " + imagePath);
//			return ProjectionType.UNKNOWN;
//		}
//
//		// Converti l'immagine in scala di grigi
//		Mat grayImage = new Mat();
//		Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
//
//		// Trova i contorni dell'immagine
//		Mat contours = new Mat();
//		Imgproc.findContours(grayImage, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//
//		// Trova il contorno con l'area massima
//		double maxArea = - 1;
//		MatOfPoint2f maxContour = new MatOfPoint2f();
//		for(int i = 0; i < contours.rows(); i++){
//			MatOfPoint2f contour = new MatOfPoint2f();
//			contours.row(i).copyTo(contour);
//			double area = Imgproc.contourArea(contour);
//			if(area > maxArea){
//				maxArea = area;
//				maxContour = contour;
//			}
//		}
//
//		// Applica un'elaborazione per determinare il tipo di proiezione (esempio semplificato)
//		if(maxContour.total() > 0){
//			double perimeter = Imgproc.arcLength(maxContour, true);
//			MatOfPoint2f approx = new MatOfPoint2f();
//			Imgproc.approxPolyDP(maxContour, approx, 0.02 * perimeter, true);
//
//			int sides = approx.rows(); // Numero di lati approssimati
//
//			// Esempio di logica semplificata per determinare il tipo di proiezione
//			if(sides >= 5)
//				return ProjectionType.SPHERICAL;
//			else if(sides == 4)
//				return ProjectionType.CYLINDRICAL;
//
//			return ProjectionType.UNKNOWN;
//		}
//
//		return ProjectionType.UNKNOWN;
//	}
//
//	// Calcola l'angolo in gradi tra due linee
//	private static double angleBetweenLines(Line line1, Line line2){
//		double dx1 = line1.pt2.x - line1.pt1.x;
//		double dy1 = line1.pt2.y - line1.pt1.y;
//		double dx2 = line2.pt2.x - line2.pt1.x;
//		double dy2 = line2.pt2.y - line2.pt1.y;
//
//		double angle1 = Math.atan2(dy1, dx1);
//		double angle2 = Math.atan2(dy2, dx2);
//
//		double angle = Math.abs(Math.toDegrees(angle1 - angle2));
//		if(angle > 180){
//			angle = 360 - angle;
//		}
//		return angle;
//	}
//
//	// Classe per rappresentare una linea con due punti
//	private static class Line{
//		Point pt1;
//		Point pt2;
//
//		Line(Point pt1, Point pt2){
//			this.pt1 = pt1;
//			this.pt2 = pt2;
//		}
//	}
//
//	// Enumerazione per i tipi di proiezione
//	private enum ProjectionType{
//		SPHERICAL, CYLINDRICAL, UNKNOWN
//	}

}
