package pe.edu.unmsm.servicios.requests.domain;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a requester left typed in the new request form, to pick it up later. The values are the form's own (names,
 * not ids) and any of them may be empty, because a draft is by definition unfinished.
 */
public record RequestDraft(String category, String description, String priority, String faculty, String pavilion,
		String room, Instant updatedAt) {

	public static final int DESCRIPTION_MAX = ServiceRequest.DESCRIPTION_MAX;
	public static final int FIELD_MAX = 150;

	public RequestDraft {
		category = orEmpty(category);
		description = orEmpty(description);
		priority = orEmpty(priority);
		faculty = orEmpty(faculty);
		pavilion = orEmpty(pavilion);
		room = orEmpty(room);
	}

	/**
	 * The draft as typed, or {@link InvalidRequestException} when a field is longer than the form allows. Texts are
	 * kept as typed (also their spaces): the form gets back exactly what the person left.
	 */
	public static RequestDraft of(String category, String description, String priority, String faculty,
			String pavilion, String room, Instant now) {
		Map<String, String> errors = new LinkedHashMap<>();
		check(errors, "category", category, FIELD_MAX);
		check(errors, "description", description, DESCRIPTION_MAX);
		check(errors, "priority", priority, FIELD_MAX);
		check(errors, "faculty", faculty, FIELD_MAX);
		check(errors, "pavilion", pavilion, FIELD_MAX);
		check(errors, "room", room, FIELD_MAX);
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
		return new RequestDraft(category, description, priority, faculty, pavilion, room, now);
	}

	/** Nothing worth keeping: every field is empty or only spaces. */
	public boolean isEmpty() {
		return category.isBlank() && description.isBlank() && priority.isBlank() && faculty.isBlank()
				&& pavilion.isBlank() && room.isBlank();
	}

	private static void check(Map<String, String> errors, String field, String value, int max) {
		if (value != null && value.length() > max) {
			errors.put(field, "Puede tener hasta " + max + " caracteres");
		}
	}

	private static String orEmpty(String value) {
		return value == null ? "" : value;
	}

}
