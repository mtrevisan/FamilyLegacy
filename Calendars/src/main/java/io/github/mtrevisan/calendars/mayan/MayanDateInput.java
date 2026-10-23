package io.github.mtrevisan.calendars.mayan;


/**
 * Data container for parsed Mayan Long Count and seasonal tracking entries.
 *
 * @param baktun The Baktun cycle coefficient (or seasonal negative flag indicator)
 * @param katun  The Katun cycle coefficient
 * @param tun    The Tun cycle coefficient
 * @param uinal  The Uinal cycle coefficient
 * @param kin    The Kin day coefficient (or custom year reference token)
 */
public record MayanDateInput(int baktun, int katun, int tun, int uinal, int kin){}
