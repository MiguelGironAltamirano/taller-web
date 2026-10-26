package pe.edu.unmsm.servicios.requests.application;

/** A rating as shown in the request detail ({@code RatingDto}); {@code comment} may be null. */
public record RatingView(int score, String comment) {
}
