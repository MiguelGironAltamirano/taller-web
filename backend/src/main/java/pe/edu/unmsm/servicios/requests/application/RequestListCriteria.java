package pe.edu.unmsm.servicios.requests.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pe.edu.unmsm.servicios.requests.domain.InvalidRequestException;
import pe.edu.unmsm.servicios.requests.domain.RequestStatus;

/**
 * Filters, order and page of {@code GET /requests}. Every filter is optional and they combine with AND; the
 * visibility rule of the role is always applied on top of them. {@code statuses} empty means any status;
 * {@code text} empty means no text search; {@code from} and {@code to} are inclusive calendar days of the
 * registration date, counted in Lima time like the year of the code.
 */
public record RequestListCriteria(Set<RequestStatus> statuses, Long areaId, Long categoryId, Long priorityId,
		Long technicianId, boolean unassigned, Boolean overdue, String text, LocalDate from, LocalDate to,
		RequestSort sort, int page, int size) {

	public static final int DEFAULT_PAGE_SIZE = 20;
	public static final int MAX_PAGE_SIZE = 500;
	public static final int MAX_TEXT_LENGTH = 100;

	static final ZoneId ZONE = ZoneId.of("America/Lima");

	public RequestListCriteria {
		statuses = statuses == null || statuses.isEmpty() ? Set.of() : Set.copyOf(statuses);
		text = text == null ? "" : text.strip();
		sort = sort == null ? RequestSort.NEWEST : sort;
	}

	/** Everything visible, newest first, first page of the default size. */
	public static RequestListCriteria firstPage() {
		return new RequestListCriteria(null, null, null, null, null, false, null, null, null, null, null, 0,
				DEFAULT_PAGE_SIZE);
	}

	/**
	 * The requester never sees a request as overdue (they see an estimated date), so filtering by overdue would
	 * reveal a supervisor's priority change: it is ignored.
	 */
	public RequestListCriteria forRequester() {
		return new RequestListCriteria(statuses, areaId, categoryId, priorityId, technicianId, unassigned, null, text,
				from, to, sort, page, size);
	}

	public boolean hasText() {
		return !text.isEmpty();
	}

	/** First registration instant included, or {@code null} when there is no lower bound. */
	public Instant createdFrom() {
		return from == null ? null : from.atStartOfDay(ZONE).toInstant();
	}

	/** First registration instant excluded (start of the day after {@code to}), or {@code null} without one. */
	public Instant createdBefore() {
		return to == null ? null : to.plusDays(1).atStartOfDay(ZONE).toInstant();
	}

	/** Query parameters as received; {@link #parse} validates them and reports every invalid one at once. */
	public record Params(List<String> status, String areaId, String categoryId, String priorityId,
			String technicianId, String unassigned, String overdue, String q, String from, String to, String sort,
			String page, String size) {
	}

	/** Builds the criteria or throws {@link InvalidRequestException} with one message per invalid parameter. */
	public static RequestListCriteria parse(Params params) {
		Map<String, String> errors = new LinkedHashMap<>();
		Set<RequestStatus> statuses = statuses(params.status(), errors);
		Long areaId = id("areaId", params.areaId(), errors);
		Long categoryId = id("categoryId", params.categoryId(), errors);
		Long priorityId = id("priorityId", params.priorityId(), errors);
		Long technicianId = id("technicianId", params.technicianId(), errors);
		Boolean unassigned = flag("unassigned", params.unassigned(), errors);
		Boolean overdue = flag("overdue", params.overdue(), errors);
		String text = params.q() == null ? "" : params.q().strip();
		if (text.length() > MAX_TEXT_LENGTH) {
			errors.put("q", "La búsqueda admite hasta " + MAX_TEXT_LENGTH + " caracteres");
		}
		LocalDate from = date("from", params.from(), errors);
		LocalDate to = date("to", params.to(), errors);
		if (from != null && to != null && to.isBefore(from)) {
			errors.put("to", "La fecha final no puede ser anterior a la inicial");
		}
		RequestSort sort = sort(params.sort(), errors);
		Integer page = integer("page", params.page(), 0, 0, Integer.MAX_VALUE, errors);
		Integer size = integer("size", params.size(), DEFAULT_PAGE_SIZE, 1, MAX_PAGE_SIZE, errors);
		if (Boolean.TRUE.equals(unassigned) && technicianId != null) {
			errors.put("unassigned", "No se puede combinar con technicianId");
		}
		if (!errors.isEmpty()) {
			throw new InvalidRequestException(errors);
		}
		return new RequestListCriteria(statuses, areaId, categoryId, priorityId, technicianId,
				Boolean.TRUE.equals(unassigned), overdue, text, from, to, sort, page, size);
	}

	private static Set<RequestStatus> statuses(List<String> values, Map<String, String> errors) {
		if (values == null) {
			return Set.of();
		}
		Set<RequestStatus> statuses = EnumSet.noneOf(RequestStatus.class);
		for (String value : values) {
			for (String part : value.split(",")) {
				String name = part.strip();
				if (name.isEmpty()) {
					continue;
				}
				try {
					statuses.add(RequestStatus.valueOf(name));
				}
				catch (IllegalArgumentException ex) {
					errors.put("status", "Estado desconocido: " + name + ". Valores válidos: "
							+ Arrays.stream(RequestStatus.values()).map(Enum::name).collect(Collectors.joining(", ")));
				}
			}
		}
		return statuses;
	}

	private static Long id(String field, String value, Map<String, String> errors) {
		if (isBlank(value)) {
			return null;
		}
		try {
			long id = Long.parseLong(value.strip());
			if (id > 0) {
				return id;
			}
		}
		catch (NumberFormatException ex) {
			// reported below
		}
		errors.put(field, "Debe ser un identificador numérico positivo");
		return null;
	}

	private static Boolean flag(String field, String value, Map<String, String> errors) {
		if (isBlank(value)) {
			return null;
		}
		return switch (value.strip()) {
			case "true" -> true;
			case "false" -> false;
			default -> {
				errors.put(field, "Debe ser true o false");
				yield null;
			}
		};
	}

	private static LocalDate date(String field, String value, Map<String, String> errors) {
		if (isBlank(value)) {
			return null;
		}
		try {
			return LocalDate.parse(value.strip());
		}
		catch (DateTimeParseException ex) {
			errors.put(field, "Debe ser una fecha AAAA-MM-DD, p. ej. 2026-10-01");
			return null;
		}
	}

	private static RequestSort sort(String value, Map<String, String> errors) {
		if (isBlank(value)) {
			return RequestSort.NEWEST;
		}
		return RequestSort.fromParam(value.strip()).orElseGet(() -> {
			errors.put("sort", "Orden desconocido. Valores válidos: " + Arrays.stream(RequestSort.values())
					.map(RequestSort::param).collect(Collectors.joining(", ")));
			return RequestSort.NEWEST;
		});
	}

	private static Integer integer(String field, String value, int fallback, int min, int max,
			Map<String, String> errors) {
		if (isBlank(value)) {
			return fallback;
		}
		try {
			int number = Integer.parseInt(value.strip());
			if (number >= min && number <= max) {
				return number;
			}
		}
		catch (NumberFormatException ex) {
			// reported below
		}
		errors.put(field, max == Integer.MAX_VALUE ? "Debe ser un entero mayor o igual que " + min
				: "Debe ser un entero entre " + min + " y " + max);
		return fallback;
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

}
