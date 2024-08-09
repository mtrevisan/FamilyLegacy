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
package io.github.mtrevisan.familylegacy.flef.ui.helpers;


public class CylindricalProjectionTypeDetector{

/*
https://chatgpt.com/c/7579f801-7f5f-497c-8c2b-a63585791180

Punti da considerare nell'algoritmo semplificato:
- Conteggio delle linee: L'algoritmo conta le linee con angoli vicini a 0° (orizzontali) e 90° (verticali) per determinare la predominanza.
- Sensibilità agli angoli: L'algoritmo utilizza un intervallo di angoli (10° per le linee orizzontali e 80-100° per le linee verticali) per distinguere tra linee orizzontali e verticali. Questo è un criterio semplificato e potrebbe non coprire tutte le possibili orientazioni delle linee nelle diverse proiezioni.
- Limitazioni dell'approccio semplificato: L'approccio semplificato può non essere sufficientemente robusto per situazioni complesse in cui le linee possono essere inclinate o curvate a causa della proiezione cilindrica. Ad esempio, potrebbero esserci casi in cui le linee orizzontali sono inclinate o le linee verticali sono distribuite in modo irregolare.

Possibili miglioramenti:
- Analisi più sofisticate delle linee: Utilizzare metodi più avanzati per l'analisi delle linee, come l'analisi delle trasformate di Hough per linee multiple con diversi intervalli di angoli.
- Utilizzo di modelli di machine learning: Addestrare modelli di machine learning con dati annotati per classificare automaticamente il tipo di proiezione cilindrica basato su caratteristiche estratte dall'immagine.
- Regolazione dei parametri: Ottimizzare i parametri dell'algoritmo, come gli intervalli di angoli e i criteri per il conteggio delle linee, in base alle caratteristiche specifiche delle immagini trattate.
*/

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
//		List<Line> detectedLines = new ArrayList<>();
//		for(int i = 0; i < lines.rows(); i++){
//			double[] line = lines.get(i, 0);
//			Point pt1 = new Point(line[0], line[1]);
//			Point pt2 = new Point(line[2], line[3]);
//			detectedLines.add(new Line(pt1, pt2));
//		}
//
//		// Classificazione del tipo di proiezione cilindrica
//		CylindricalProjectionType projectionType = classifyCylindricalProjectionType(detectedLines);
//
//		// Output del risultato
//		System.out.println("Tipo di proiezione cilindrica dell'immagine: " + projectionType);
//	}
//
//	// Metodo per classificare il tipo di proiezione cilindrica (orizzontale o verticale)
//	private static CylindricalProjectionType classifyCylindricalProjectionType(List<Line> lines){
//		// Esempio di logica semplificata per la classificazione
//		int verticalCount = 0;
//		int horizontalCount = 0;
//
//		for(Line line : lines){
//			double angle = Math.abs(angleOfLine(line));
//
//			if(angle < 10 || angle > 170){
//				horizontalCount++;
//			}
//			else if(angle > 80 && angle < 100){
//				verticalCount++;
//			}
//		}
//
//		// Classificazione basata sui conteggi
//		if(verticalCount > horizontalCount){
//			return CylindricalProjectionType.VERTICAL;
//		}
//		else{
//			return CylindricalProjectionType.HORIZONTAL;
//		}
//	}
//
//	// Calcola l'angolo in gradi della linea rispetto all'asse orizzontale
//	private static double angleOfLine(Line line){
//		double dx = line.pt2.x - line.pt1.x;
//		double dy = line.pt2.y - line.pt1.y;
//
//		return Math.toDegrees(Math.atan2(dy, dx));
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
//	// Enumerazione per i tipi di proiezione cilindrica
//	private enum CylindricalProjectionType{
//		HORIZONTAL, VERTICAL
//	}

}
