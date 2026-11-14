package pe.edu.unmsm.servicios.requests.application;

/** The requester's satisfaction rating shown in the detail; the satisfaction module writes it. */
public record RatingView(int score, String comment) {
}
