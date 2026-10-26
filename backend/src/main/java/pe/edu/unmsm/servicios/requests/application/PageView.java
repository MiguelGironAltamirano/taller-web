package pe.edu.unmsm.servicios.requests.application;

import java.util.List;

public record PageView<T>(List<T> items, int page, int size, long total) {

	/** Number of pages of {@code size} needed for {@code total}; zero when there is nothing. */
	public int totalPages() {
		return (int) ((total + size - 1) / size);
	}

}
