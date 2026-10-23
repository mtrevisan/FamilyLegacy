/**
 * Copyright (c) 2026 Mauro Trevisan
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
package io.github.mtrevisan.calendars.liturgical;

import io.github.mtrevisan.calendars.text.TextNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The feasts of the (Tridentine) Roman calendar, and the traditional English, French, and German names of the days
 * used to date the documents, with their names in Italian, Latin, English, and (for the main ones) French and German.
 * <p>
 * The names are matched after the normalization of the text (see {@link TextNormalizer}) and the removal of the words
 * that carry no information (articles, prepositions, "festa", "San", "Sancti", ...). A bare name that is shared by
 * more saints is mapped to the feast it usually stands for in the documents (e.g. "San Giovanni" is the Nativity of
 * Saint John the Baptist, 24 June, "San Pietro" is Saints Peter and Paul, 29 June).
 */
public final class LiturgicalFeasts{

	/** Words removed before matching a feast name. */
	static final Set<String> FILLERS = Set.of("il", "lo", "la", "l", "i", "gli", "le", "les", "the", "of", "de", "del",
		"della", "dello", "delle", "dei", "degli", "dell", "nell", "all", "dall", "sull", "di", "d", "da", "dal", "dalla",
		"du", "des", "der", "die", "das", "dem",
		"den", "in", "nel", "nella", "a", "al", "alla", "festo", "festum", "festi", "festa", "feast", "fete", "feste", "fest",
		"giorno", "day", "dies", "jour", "tag", "s", "st", "ss", "san", "sant", "sankt", "saint", "sainte", "saints",
		"sancti", "sanctae", "sancte", "sanctorum", "sanctis", "beati", "beatae", "beato", "beata", "b", "hl", "heiligen",
		"heilige", "e", "ed", "et", "and", "und", "y", "on", "am", "au", "el", "los", "las");
	/** Words removed only when the name is not found otherwise (e.g. "Santo Stefano", but "Giovedì Santo"). */
	static final Set<String> SECONDARY_FILLERS = Set.of("santo", "santa", "santi", "sante", "santissima", "santissimo",
		"sancto", "sanctum", "sancta", "sanctus", "sanctissimae", "sanctissimi", "beatissimae", "gloriosae", "gloriosi",
		"apostoli", "apostolo", "apostle", "apostles", "apostolorum", "martyris", "martire", "martyr", "virginis",
		"vergine", "virgin", "episcopi", "vescovo", "bishop", "confessoris", "abbatis", "abate", "abbot");

	private static final Map<String, Feast> FEASTS = new HashMap<>();

	static{
		// ---- movable feasts, from Easter Sunday ----
		register(Feast.easter("septuagesima", -63), "Septuagesima", "Settuagesima", "Domenica di Settuagesima",
			"Septuagesima Sunday", "Circumdederunt");
		register(Feast.easter("sexagesima", -56), "Sexagesima", "Sessagesima", "Domenica di Sessagesima",
			"Sexagesima Sunday", "Exsurge");
		register(Feast.easter("quinquagesima", -49), "Quinquagesima", "Quinquagesima Sunday", "Esto mihi", "Estomihi",
			"Domenica di Quinquagesima", "Shrove Sunday");
		register(Feast.easter("giovedi_grasso", -52), "Giovedì grasso", "Fat Thursday");
		register(Feast.easter("martedi_grasso", -47), "Martedì grasso", "Shrove Tuesday", "Mardi gras", "Fastnacht",
			"Pancake Day");
		register(Feast.easter("ash_wednesday", -46), "Mercoledì delle Ceneri", "Le Ceneri", "Ceneri", "Dies cinerum",
			"Feria quarta cinerum", "Cinerum", "Ash Wednesday", "Mercredi des Cendres", "Aschermittwoch",
			// the beginning of Lent, so that "the third Sunday in Lent" is the third Sunday from Ash Wednesday
			"Quaresima", "Lent", "Carême", "Fastenzeit", "Cuaresma");
		register(Feast.easter("quadragesima", -42), "Quadragesima", "Quadragesimae", "Invocavit", "Prima domenica di Quaresima",
			"Dominica prima Quadragesimae", "Quadragesima Sunday");
		register(Feast.easter("reminiscere", -35), "Reminiscere");
		register(Feast.easter("oculi", -28), "Oculi");
		register(Feast.easter("laetare", -21), "Laetare", "Letare", "Domenica Laetare", "Mothering Sunday",
			"Refreshment Sunday");
		register(Feast.easter("judica", -14), "Judica", "Iudica", "Domenica di Passione", "Dominica Passionis",
			"Passion Sunday", "Passionssonntag");
		register(Feast.easter("palm_sunday", -7), "Domenica delle Palme", "Domenica degli Ulivi", "Le Palme",
			"Dominica in Palmis", "Dominica Palmarum", "Palmarum", "Palm Sunday", "Dimanche des Rameaux", "Rameaux",
			"Palmsonntag");
		register(Feast.easter("maundy_thursday", -3), "Giovedì Santo", "Coena Domini", "Feria quinta in Coena Domini",
			"In Coena Domini", "Maundy Thursday", "Jeudi saint", "Gründonnerstag");
		register(Feast.easter("good_friday", -2), "Venerdì Santo", "Parasceve", "Feria sexta in Parasceve",
			"Good Friday", "Vendredi saint", "Karfreitag");
		register(Feast.easter("holy_saturday", -1), "Sabato Santo", "Sabbatum Sanctum", "Holy Saturday", "Samedi saint",
			"Karsamstag");
		register(Feast.easter("easter", 0), "Pasqua", "Pasqua di Resurrezione", "Domenica di Pasqua", "Pascha",
			"Pascha Domini", "Paschae", "Dominica Resurrectionis", "Resurrectio Domini", "Easter", "Easter Sunday",
			"Easter Day", "Pâques", "Ostern", "Ostersonntag", "Pascua", "Domingo de Pascua");
		register(Feast.easter("easter_monday", 1), "Lunedì dell'Angelo", "Pasquetta", "Lunedì di Pasqua",
			"Feria secunda Paschae", "Easter Monday", "Lundi de Pâques", "Ostermontag");
		register(Feast.easter("easter_tuesday", 2), "Martedì di Pasqua", "Easter Tuesday", "Osterdienstag");
		register(Feast.easter("low_sunday", 7), "Domenica in Albis", "In Albis", "Dominica in Albis", "Quasimodo",
			"Quasimodogeniti", "Low Sunday", "Dimanche de Quasimodo", "Weißer Sonntag");
		register(Feast.easter("misericordia", 14), "Misericordias Domini", "Misericordia Domini", "Misericordias",
			"Good Shepherd Sunday");
		register(Feast.easter("jubilate", 21), "Jubilate", "Iubilate");
		register(Feast.easter("cantate", 28), "Cantate");
		register(Feast.easter("rogate", 35), "Rogate", "Vocem jucunditatis", "Rogation Sunday");
		register(Feast.easter("ascension", 39), "Ascensione", "Ascensione del Signore", "Ascensio Domini", "Ascensionis",
			"Ascension", "Ascension Day", "Ascension Thursday", "Holy Thursday", "Christi Himmelfahrt", "Himmelfahrt",
			"Ascensión");
		register(Feast.easter("exaudi", 42), "Exaudi");
		register(Feast.easter("pentecost", 49), "Pentecoste", "Domenica di Pentecoste", "Pentecostes", "Pentecosten",
			"Pentecostem", "Dominica Pentecostes", "Pentecost", "Whitsunday", "Whit Sunday", "Whitsun", "Pentecôte",
			"Pfingsten", "Pfingstsonntag", "Pentecostés");
		register(Feast.easter("pentecost_monday", 50), "Lunedì di Pentecoste", "Feria secunda Pentecostes",
			"Whit Monday", "Whitmonday", "Lundi de Pentecôte", "Pfingstmontag");
		register(Feast.easter("trinity", 56), "Santissima Trinità", "Trinità", "SS. Trinità", "Trinitas",
			"Sanctissimae Trinitatis", "Trinitatis", "Dominica Trinitatis", "Trinity", "Trinity Sunday", "Trinité",
			"Dreifaltigkeitssonntag");
		register(Feast.easter("corpus_christi", 60), "Corpus Domini", "Corpus Christi", "Festum Corporis Christi",
			"Fête-Dieu", "Fronleichnam");
		register(Feast.easter("sacred_heart", 68), "Sacro Cuore", "Sacratissimi Cordis Iesu", "Sacred Heart",
			"Sacré-Cœur");

		// ---- movable feasts, from the first Sunday of Advent ----
		register(Feast.advent("advent", 0), "Avvento", "Prima domenica di Avvento", "Adventus", "Dominica prima Adventus",
			"Ad te levavi", "Advent", "Advent Sunday", "Avent", "Erster Advent");
		register(Feast.advent("advent_2", 7), "Populus Sion");
		register(Feast.advent("gaudete", 14), "Gaudete");
		register(Feast.advent("rorate", 21), "Rorate");

		// ---- fixed feasts of the Lord and of the Virgin ----
		register(Feast.fixed("circumcision", 1, 1), "Circoncisione", "Circoncisione del Signore", "Circumcisio Domini",
			"Circumcisionis", "Capodanno", "Circumcision", "New Year's Day", "Neujahr");
		register(Feast.fixed("epiphany", 1, 6), "Epifania", "Epifania del Signore", "Befana", "Epiphania",
			"Epiphania Domini", "Epiphaniae", "Theophania", "Epiphany", "Twelfth Day", "Épiphanie", "Dreikönigstag",
			"Heilige Drei Könige", "Re Magi", "Tre Re", "Reyes");
		register(Feast.fixed("purification", 2, 2), "Purificazione", "Purificazione di Maria", "Purificazione della Vergine",
			"Candelora", "Purificatio", "Purificatio Beatae Mariae Virginis", "Purificationis", "Candlemas", "Purification",
			"Chandeleur", "Lichtmess", "Mariä Lichtmess");
		register(Feast.fixed("annunciation", 3, 25), "Annunciazione", "Annunciazione del Signore", "Annunziata",
			"Annunciatio", "Annunciatio Beatae Mariae Virginis", "Annunciationis", "Incarnatio", "Annunciation",
			"Lady Day", "Annonciation", "Mariä Verkündigung");
		register(Feast.fixed("visitation", 7, 2), "Visitazione", "Visitazione di Maria", "Visitatio",
			"Visitationis", "Visitation", "Mariä Heimsuchung");
		register(Feast.fixed("our_lady_of_mount_carmel", 7, 16), "Madonna del Carmine", "Madonna del Carmelo",
			"Beatae Mariae Virginis de Monte Carmelo", "Our Lady of Mount Carmel");
		register(Feast.fixed("our_lady_of_the_snow", 8, 5), "Madonna della Neve", "Sancta Maria ad Nives",
			"Our Lady of the Snows");
		register(Feast.fixed("transfiguration", 8, 6), "Trasfigurazione", "Transfiguratio Domini", "Transfigurationis",
			"Transfiguration");
		register(Feast.fixed("assumption", 8, 15), "Assunzione", "Assunta", "Assunzione di Maria", "Ferragosto",
			"Assumptio", "Assumptio Beatae Mariae Virginis", "Assumptionis", "Assumption", "Assomption",
			"Mariä Himmelfahrt", "Asunción");
		register(Feast.fixed("nativity_of_mary", 9, 8), "Natività di Maria", "Natività della Vergine", "Nativitas Beatae Mariae Virginis",
			"Nativitatis Beatae Mariae", "Nativity of the Virgin", "Nativity of Mary", "Mariä Geburt", "Madonna di Settembre");
		register(Feast.fixed("presentation_of_mary", 11, 21), "Presentazione di Maria", "Presentazione della Vergine",
			"Praesentatio Beatae Mariae Virginis", "Presentation of Mary");
		register(Feast.fixed("immaculate_conception", 12, 8), "Immacolata", "Immacolata Concezione", "Concezione",
			"Conceptio Beatae Mariae Virginis", "Conceptionis", "Immaculate Conception", "Conception of Mary",
			"Mariä Empfängnis");
		register(Feast.fixed("christmas", 12, 25), "Natale", "Natale del Signore", "Natività del Signore", "Nativitas Domini",
			"Nativitatis Domini", "In Nativitate Domini", "Christmas", "Christmas Day", "Noël", "Weihnachten",
			"Christtag", "Navidad");
		register(Feast.fixed("finding_of_the_cross", 5, 3), "Invenzione della Croce", "Santa Croce di maggio",
			"Inventio Sanctae Crucis", "Inventionis Crucis", "Finding of the Holy Cross", "Invention of the Cross",
			"Roodmas", "Kreuzauffindung");
		register(Feast.fixed("exaltation_of_the_cross", 9, 14), "Esaltazione della Croce", "Santa Croce di settembre",
			"Exaltatio Sanctae Crucis", "Exaltationis Crucis", "Exaltation of the Cross", "Holy Cross Day", "Holy Rood Day",
			"Holyrood Day", "Kreuzerhöhung");
		register(Feast.fixed("all_saints", 11, 1), "Ognissanti", "Tutti i Santi", "Omnium Sanctorum", "Festum Omnium Sanctorum",
			"All Saints", "All Saints' Day", "All Hallows", "Hallowmas", "Toussaint", "Allerheiligen", "Todos los Santos");
		register(Feast.fixed("all_souls", 11, 2), "Commemorazione dei Defunti", "Defunti", "I Morti", "Giorno dei Morti",
			"Commemoratio Omnium Fidelium Defunctorum", "Omnium Defunctorum", "All Souls", "All Souls' Day", "Jour des Morts",
			"Allerseelen");
		register(Feast.fixed("holy_innocents", 12, 28), "Santi Innocenti", "Innocenti", "Sanctorum Innocentium",
			"Innocentium", "Holy Innocents", "Innocents", "Childermas", "Unschuldige Kinder");

		// ---- fixed feasts of the saints ----
		register(Feast.fixed("hilary_english", 1, 13), "Hilary", "Hilarii", "Hilary's Day");
		register(Feast.fixed("hilary", 1, 14), "Ilario");
		register(Feast.fixed("anthony_abbot", 1, 17), "Antonio Abate", "Sant'Antonio Abate", "Antonio del Porcello",
			"Antonii Abbatis", "Anthony the Abbot", "Anthony Abbot", "Antoine");
		register(Feast.fixed("sebastian", 1, 20), "Sebastiano", "Fabiano e Sebastiano", "Sebastiani", "Fabiani et Sebastiani",
			"Sebastian", "Sébastien");
		register(Feast.fixed("agnes", 1, 21), "Agnese", "Agnetis", "Agnes");
		register(Feast.fixed("vincent", 1, 22), "Vincenzo", "Vincentii", "Vincent");
		register(Feast.fixed("conversion_of_paul", 1, 25), "Conversione di San Paolo", "Conversio Sancti Pauli",
			"Conversionis Pauli", "Conversion of Saint Paul", "Pauli Bekehrung");
		register(Feast.fixed("blaise", 2, 3), "Biagio", "Blasii", "Blaise", "Blasius");
		register(Feast.fixed("agatha", 2, 5), "Agata", "Agathae", "Agatha", "Agathe");
		register(Feast.fixed("valentine", 2, 14), "Valentino", "Valentini", "Valentine", "Valentin");
		register(Feast.fixed("chair_of_peter", 2, 22), "Cattedra di San Pietro", "Cathedra Sancti Petri", "Cathedra Petri",
			"Chair of Saint Peter", "Petri Stuhlfeier");
		register(Feast.fixedWithLeapShift("matthias", 2, 24), "Mattia", "Matthiae", "Mathiae", "Matthias", "Mathias");
		register(Feast.fixed("david", 3, 1), "David of Wales", "Saint David's Day", "Davidis");
		register(Feast.fixed("gregory", 3, 12), "Gregorio", "Gregorio Magno", "Gregorii", "Gregory", "Grégoire");
		register(Feast.fixed("patrick", 3, 17), "Patrizio", "Patricii", "Patrick");
		register(Feast.fixed("joseph", 3, 19), "Giuseppe", "Ioseph", "Joseph", "Josephi", "Iosephi", "Joseph's Day",
			"Josephstag");
		register(Feast.fixed("benedict", 3, 21), "Benedetto", "Benedicti", "Benedict", "Benoît");
		register(Feast.fixed("george", 4, 23), "Giorgio", "Georgii", "George", "Georges", "Georg", "Jorge");
		register(Feast.fixed("mark", 4, 25), "Marco", "Marco Evangelista", "Marci", "Marci Evangelistae", "Mark",
			"Marc", "Markus", "Marcos");
		register(Feast.fixed("philip_and_james", 5, 1), "Filippo e Giacomo", "Giacomo e Filippo", "Philippi et Iacobi",
			"Philippi et Jacobi", "Philip and James", "Philippe et Jacques", "Walpurgis", "Walburgae");
		register(Feast.fixed("barnabas", 6, 11), "Barnaba", "Barnabae", "Barnabas", "Barnabé");
		register(Feast.fixed("anthony_of_padua", 6, 13), "Antonio di Padova", "Sant'Antonio di Padova", "Antonii de Padua",
			"Anthony of Padua", "Antoine de Padoue");
		register(Feast.fixed("vitus", 6, 15), "Vito", "Viti", "Vitus", "Guy", "Veit");
		register(Feast.fixed("aloysius", 6, 21), "Luigi Gonzaga", "Aloysii", "Aloysius Gonzaga");
		register(Feast.fixed("nativity_of_john_the_baptist", 6, 24), "Giovanni", "Giovanni Battista", "San Giovanni Battista",
			"Natività di San Giovanni Battista", "Nativitas Sancti Ioannis Baptistae", "Ioannis Baptistae", "Johannis Baptistae",
			"Joannis Baptistae", "Iohannis Baptistae", "Johannis", "Ioannis", "Joannis", "John the Baptist", "Saint John's Day",
			"John", "Midsummer", "Midsummer Day", "Saint-Jean", "Jean-Baptiste", "Johannistag", "Johanni", "San Juan");
		register(Feast.fixed("peter_and_paul", 6, 29), "Pietro e Paolo", "Pietro", "Santi Pietro e Paolo", "Petri et Pauli",
			"Petri", "Peter and Paul", "Peter", "Pierre et Paul", "Peter und Paul");
		register(Feast.fixed("commemoration_of_paul", 6, 30), "Commemorazione di San Paolo", "Commemoratio Sancti Pauli");
		register(Feast.fixed("swithun", 7, 15), "Swithun", "Swithin", "Swithuni");
		register(Feast.fixed("margaret", 7, 20), "Margherita", "Margaritae", "Margaret", "Marguerite", "Margareta");
		register(Feast.fixed("mary_magdalene", 7, 22), "Maria Maddalena", "Maddalena", "Mariae Magdalenae", "Magdalenae",
			"Mary Magdalene", "Magdalene", "Madeleine", "Maria Magdalena");
		register(Feast.fixed("james", 7, 25), "Giacomo", "Giacomo Apostolo", "Iacobi", "Jacobi", "Jacobi Apostoli",
			"James", "Jacques", "Jakob", "Jakobi", "Santiago");
		register(Feast.fixed("christopher", 7, 25), "Cristoforo", "Christophori", "Christopher", "Christophe");
		register(Feast.fixed("anne", 7, 26), "Anna", "Sant'Anna", "Annae", "Anne", "Anna Mutter Mariens");
		register(Feast.fixed("peter_in_chains", 8, 1), "San Pietro in Vincoli", "Pietro in Vincoli", "Petri ad Vincula",
			"Ad Vincula", "Lammas", "Lammas Day", "Petri Kettenfeier");
		register(Feast.fixed("dominic", 8, 4), "Domenico", "Dominici", "Dominic", "Dominique");
		register(Feast.fixed("lawrence", 8, 10), "Lorenzo", "Laurentii", "Laurence", "Lawrence", "Laurent", "Laurentius");
		register(Feast.fixed("roch", 8, 16), "Rocco", "Rochi", "Roch", "Rochus");
		register(Feast.fixed("bernard", 8, 20), "Bernardo", "Bernardi", "Bernard", "Bernhard");
		register(Feast.fixed("bartholomew", 8, 24), "Bartolomeo", "Bartholomaei", "Bartholomew", "Barthélemy",
			"Bartholomäus", "Bartholomai");
		register(Feast.fixed("augustine", 8, 28), "Agostino", "Augustini", "Augustine", "Augustin");
		register(Feast.fixed("beheading_of_john", 8, 29), "Decollazione di San Giovanni", "Decollatio Sancti Ioannis Baptistae",
			"Decollationis Ioannis", "Beheading of John the Baptist", "Johannes Enthauptung");
		register(Feast.fixed("giles", 9, 1), "Egidio", "Aegidii", "Egidii", "Giles", "Gilles", "Ägidius");
		register(Feast.fixed("januarius", 9, 19), "Gennaro", "Ianuarii Episcopi", "Januarius");
		register(Feast.fixed("matthew", 9, 21), "Matteo", "Matthaei", "Matthew", "Matthieu", "Matthäus");
		register(Feast.fixed("maurice", 9, 22), "Maurizio", "Mauritii", "Maurice", "Mauritius");
		register(Feast.fixed("michael", 9, 29), "Michele", "Michele Arcangelo", "San Michele", "Michaelis", "Michaelis Archangeli",
			"Dedicatio Sancti Michaelis", "Michael", "Michaelmas", "Saint-Michel", "Michaeli", "Michaelistag");
		register(Feast.fixed("jerome", 9, 30), "Girolamo", "Hieronymi", "Jerome", "Jérôme");
		register(Feast.fixed("remigius", 10, 1), "Remigio", "Remigii", "Remigius", "Rémi");
		register(Feast.fixed("francis", 10, 4), "Francesco", "Francesco d'Assisi", "Francisci", "Francis", "François");
		register(Feast.fixed("denis", 10, 9), "Dionigi", "Dionysii", "Denis", "Dionysius");
		register(Feast.fixed("edward_the_confessor", 10, 13), "Edward the Confessor", "Edwardi Confessoris");
		register(Feast.fixed("gall", 10, 16), "Gallo", "Galli", "Gall", "Gallus");
		register(Feast.fixed("luke", 10, 18), "Luca", "Lucae", "Luke", "Luc", "Lukas");
		register(Feast.fixed("ursula", 10, 21), "Orsola", "Ursulae", "Ursula", "Ursule");
		register(Feast.fixed("simon_and_jude", 10, 28), "Simone e Giuda", "Simonis et Iudae", "Simonis et Judae",
			"Simon and Jude", "Simon et Jude", "Simon und Judas");
		register(Feast.fixed("leonard", 11, 6), "Leonardo", "Leonardi", "Leonard", "Léonard", "Leonhard");
		register(Feast.fixed("martin", 11, 11), "Martino", "Martini", "Martin", "Martinmas", "Saint-Martin", "Martinstag",
			"Martí");
		register(Feast.fixed("elizabeth", 11, 19), "Elisabetta", "Elisabeth", "Elisabethae", "Elizabeth of Hungary");
		register(Feast.fixed("cecilia", 11, 22), "Cecilia", "Caeciliae", "Ceciliae", "Cécile", "Cäcilia");
		register(Feast.fixed("clement", 11, 23), "Clemente", "Clementis", "Clement", "Clément");
		register(Feast.fixed("catherine", 11, 25), "Caterina", "Caterina d'Alessandria", "Catharinae", "Katharinae",
			"Catherine", "Katharina");
		register(Feast.fixed("andrew", 11, 30), "Andrea", "Andreae", "Andrew", "André", "Andreas");
		register(Feast.fixed("barbara", 12, 4), "Barbara", "Barbarae");
		register(Feast.fixed("nicholas", 12, 6), "Nicola", "Nicolò", "Niccolò", "Nicolao", "Nicolai", "Nicholas",
			"Nicolas", "Nikolaus");
		register(Feast.fixed("ambrose", 12, 7), "Ambrogio", "Ambrosii", "Ambrose", "Ambroise");
		register(Feast.fixed("lucy", 12, 13), "Lucia", "Luciae", "Lucy", "Lucie");
		register(Feast.fixed("thomas", 12, 21), "Tommaso", "Tommaso Apostolo", "Thomae", "Thomae Apostoli", "Thomas",
			"Thomas the Apostle");
		register(Feast.fixed("stephen", 12, 26), "Stefano", "Santo Stefano", "Stephani", "Stephen", "Étienne", "Stephanstag");
		register(Feast.fixed("john_the_evangelist", 12, 27), "Giovanni Evangelista", "Ioannis Evangelistae",
			"Johannis Evangelistae", "Joannis Evangelistae", "John the Evangelist", "Jean l'Évangéliste");
		register(Feast.fixed("thomas_becket", 12, 29), "Tommaso Becket", "Thomas Becket", "Thomas of Canterbury",
			"Thomae Cantuariensis");
		register(Feast.fixed("sylvester", 12, 31), "Silvestro", "Sylvestri", "Silvestri", "Sylvester", "Silvester",
			"Saint-Sylvestre");
	}


	private LiturgicalFeasts(){}


	private static void register(final Feast feast, final String... names){
		for(final String name : names)
			FEASTS.putIfAbsent(key(name, false), feast);
	}

	/**
	 * Finds the feast named by a sequence of normalized words.
	 *
	 * @param words the normalized words
	 * @return the feast, or {@code null} if the words do not name a known feast
	 */
	static Feast find(final List<String> words){
		final String joined = String.join(" ", words);
		final Feast feast = FEASTS.get(key(joined, false));
		return (feast != null? feast: FEASTS.get(key(joined, true)));
	}

	/**
	 * Builds the lookup key of a feast name, dropping the words that carry no information.
	 */
	private static String key(final String name, final boolean dropSecondary){
		final List<String> words = new ArrayList<>();
		for(final String word : TextNormalizer.tokenize(name))
			if(!FILLERS.contains(word) && !(dropSecondary && SECONDARY_FILLERS.contains(word)))
				words.add(word);
		return String.join(" ", words);
	}

}
