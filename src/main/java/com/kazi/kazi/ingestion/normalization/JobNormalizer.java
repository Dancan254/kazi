package com.kazi.kazi.ingestion.normalization;

import com.kazi.kazi.ingestion.normalization.NormalizedJob.EmploymentType;
import com.kazi.kazi.ingestion.source.RawJob;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class JobNormalizer {

    private static final Pattern SALARY_PATTERN = Pattern.compile(
        "(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k)?(?:\\s*(?:-|to)\\s*[$€£]?\\s*(\\d[\\d,]*(?:\\.\\d+)?)\\s*(k)?)?",
        Pattern.CASE_INSENSITIVE
    );

    public NormalizedJob normalize(RawJob rawJob) {
        Location location = parseLocation(rawJob.location());
        SalaryRange salary = parseSalary(rawJob.salary());

        return new NormalizedJob(
            rawJob,
            HtmlTextNormalizer.toPlainText(rawJob.description()),
            normalizeTitle(rawJob.title()),
            clean(rawJob.company()),
            location.city(),
            location.region(),
            location.country(),
            location.remote(),
            salary.min(),
            salary.max(),
            salary.currency(),
            parseEmploymentType(rawJob.employmentType())
        );
    }

    private String normalizeTitle(String title) {
        return clean(title)
            .replaceAll("\\bsr\\b", "senior")
            .replaceAll("\\bjr\\b", "junior")
            .replaceAll("\\bswe\\b", "software engineer")
            .replaceAll("\\beng\\b", "engineer")
            .replaceAll("\\bdev\\b", "developer");
    }

    private Location parseLocation(String value) {
        String location = nullToEmpty(value).toLowerCase(Locale.ROOT);
        boolean remote = location.matches(".*\\b(remote|anywhere|work from home)\\b.*");
        String withoutRemote = location
            .replaceAll("\\([^)]*remote[^)]*\\)", "")
            .replaceAll("\\b(remote|anywhere|work from home)\\b", "");
        List<String> parts = Arrays.stream(withoutRemote.split(","))
            .map(part -> part.replaceAll("\\s+", " ").trim())
            .filter(part -> !part.isBlank())
            .toList();

        if (parts.isEmpty()) {
            return new Location(null, null, null, remote);
        }

        String city = clean(parts.getFirst());
        String region = parts.size() > 2 ? clean(parts.get(1)) : null;
        String country = parts.size() > 1 ? clean(parts.getLast()) : null;
        return new Location(city, region, country, remote);
    }

    private SalaryRange parseSalary(String value) {
        if (value == null || value.isBlank()) {
            return new SalaryRange(null, null, null);
        }

        Matcher matcher = SALARY_PATTERN.matcher(value);
        if (!matcher.find()) {
            return new SalaryRange(null, null, detectCurrency(value));
        }

        int annualMultiplier = annualMultiplier(value);
        Integer min = parseAmount(matcher.group(1), matcher.group(2), annualMultiplier);
        Integer max = matcher.group(3) == null
            ? min
            : parseAmount(matcher.group(3), matcher.group(4), annualMultiplier);
        return new SalaryRange(min, max, detectCurrency(value));
    }

    private Integer parseAmount(String value, String suffix, int annualMultiplier) {
        double amount = Double.parseDouble(value.replace(",", ""));
        if ("k".equalsIgnoreCase(suffix)) {
            amount *= 1_000;
        }
        return (int) Math.round(amount * annualMultiplier);
    }

    private int annualMultiplier(String salary) {
        String value = salary.toLowerCase(Locale.ROOT);
        if (value.contains("hour")) {
            return 2_080;
        }
        if (value.contains("month")) {
            return 12;
        }
        return 1;
    }

    private String detectCurrency(String salary) {
        String value = salary.toUpperCase(Locale.ROOT);
        if (salary.contains("€") || value.contains("EUR")) {
            return "EUR";
        }
        if (salary.contains("£") || value.contains("GBP")) {
            return "GBP";
        }
        if (value.contains("KES") || value.contains("KSH")) {
            return "KES";
        }
        if (salary.contains("$") || value.contains("USD")) {
            return "USD";
        }
        return null;
    }

    private EmploymentType parseEmploymentType(String value) {
        return switch (clean(value)) {
            case "full time", "fulltime", "fte" -> EmploymentType.FULL_TIME;
            case "part time", "parttime" -> EmploymentType.PART_TIME;
            case "contract", "contractor" -> EmploymentType.CONTRACT;
            case "internship", "intern" -> EmploymentType.INTERNSHIP;
            default -> null;
        };
    }

    private String clean(String value) {
        return nullToEmpty(value)
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^\\p{L}\\p{N}\\s]", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Location(String city, String region, String country, boolean remote) {
    }

    private record SalaryRange(Integer min, Integer max, String currency) {
    }
}
