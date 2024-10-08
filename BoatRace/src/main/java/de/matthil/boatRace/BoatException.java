package de.matthil.boatRace;

/**
 * Exception class for errors related to the BoatRace plugin.
 */
public class BoatException extends Exception {
    public BoatException(String errorMessage) {
        super(errorMessage);
    }
}
