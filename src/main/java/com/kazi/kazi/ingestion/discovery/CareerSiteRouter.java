package com.kazi.kazi.ingestion.discovery;

import com.kazi.kazi.ingestion.ashby.AshbyJobSource;
import com.kazi.kazi.ingestion.ashby.AshbyProperties;
import com.kazi.kazi.ingestion.bamboohr.BambooHrJobSource;
import com.kazi.kazi.ingestion.bamboohr.BambooHrProperties;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseJobSource;
import com.kazi.kazi.ingestion.greenhouse.GreenhouseProperties;
import com.kazi.kazi.ingestion.lever.LeverJobSource;
import com.kazi.kazi.ingestion.lever.LeverProperties;
import com.kazi.kazi.ingestion.personio.PersonioJobSource;
import com.kazi.kazi.ingestion.personio.PersonioProperties;
import com.kazi.kazi.ingestion.recruitee.RecruiteeJobSource;
import com.kazi.kazi.ingestion.recruitee.RecruiteeProperties;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersJobSource;
import com.kazi.kazi.ingestion.smartrecruiters.SmartRecruitersProperties;
import com.kazi.kazi.ingestion.source.JobSource;
import com.kazi.kazi.ingestion.workable.WorkableJobSource;
import com.kazi.kazi.ingestion.workable.WorkableProperties;
import com.kazi.kazi.ingestion.workday.WorkdayJobSource;
import com.kazi.kazi.ingestion.workday.WorkdayProperties;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class CareerSiteRouter {

    private final GreenhouseJobSource greenhouse;
    private final LeverJobSource lever;
    private final AshbyJobSource ashby;
    private final WorkdayJobSource workday;
    private final SmartRecruitersJobSource smartRecruiters;
    private final WorkableJobSource workable;
    private final RecruiteeJobSource recruitee;
    private final PersonioJobSource personio;
    private final BambooHrJobSource bambooHr;

    CareerSiteRouter(
        GreenhouseJobSource greenhouse,
        LeverJobSource lever,
        AshbyJobSource ashby,
        WorkdayJobSource workday,
        SmartRecruitersJobSource smartRecruiters,
        WorkableJobSource workable,
        RecruiteeJobSource recruitee,
        PersonioJobSource personio,
        BambooHrJobSource bambooHr
    ) {
        this.greenhouse = greenhouse;
        this.lever = lever;
        this.ashby = ashby;
        this.workday = workday;
        this.smartRecruiters = smartRecruiters;
        this.workable = workable;
        this.recruitee = recruitee;
        this.personio = personio;
        this.bambooHr = bambooHr;
    }

    public Optional<JobSource> route(String company, URI uri) {
        if (uri == null || uri.getHost() == null || !"https".equalsIgnoreCase(uri.getScheme())
            && !"http".equalsIgnoreCase(uri.getScheme())) {
            return Optional.empty();
        }
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        String token = boardSegment(uri);
        if (token == null && (host.equals("boards.greenhouse.io") || host.equals("job-boards.greenhouse.io")
            || host.equals("jobs.lever.co") || host.equals("jobs.ashbyhq.com")
            || host.equals("jobs.smartrecruiters.com") || host.equals("apply.workable.com")
            || host.endsWith(".myworkdayjobs.com"))) {
            return Optional.empty();
        }
        if (host.equals("boards.greenhouse.io") || host.equals("job-boards.greenhouse.io")) {
            var board = new GreenhouseProperties.Board(company, token);
            return source("greenhouse:" + token, lastRun -> greenhouse.fetchSince(board, lastRun));
        }
        if (host.equals("jobs.lever.co")) {
            var site = new LeverProperties.Site(company, token);
            return source("lever:" + token, lastRun -> lever.fetchSince(site, lastRun));
        }
        if (host.equals("jobs.ashbyhq.com")) {
            var organization = new AshbyProperties.Organization(company, token);
            return source("ashby:" + token, lastRun -> ashby.fetchSince(organization, lastRun));
        }
        if (host.equals("jobs.smartrecruiters.com")) {
            var smartCompany = new SmartRecruitersProperties.Company(company, token);
            return source("smartrecruiters:" + token, lastRun -> smartRecruiters.fetchSince(smartCompany, lastRun));
        }
        if (host.equals("apply.workable.com")) {
            var account = new WorkableProperties.Account(company, token);
            return source("workable:" + token, lastRun -> workable.fetchSince(account, lastRun));
        }
        if (host.endsWith(".myworkdayjobs.com")) {
            String tenant = host.substring(0, host.indexOf('.'));
            var site = new WorkdayProperties.Site(company, origin(uri), tenant, token);
            return source("workday:" + tenant + "/" + token, lastRun -> workday.fetchSince(site, lastRun));
        }
        if (host.endsWith(".recruitee.com")) {
            String siteName = host.substring(0, host.indexOf('.'));
            var site = new RecruiteeProperties.Company(company, siteName);
            return source("recruitee:" + siteName, lastRun -> recruitee.fetchSince(site, lastRun));
        }
        if (host.endsWith(".jobs.personio.de")) {
            String siteName = host.substring(0, host.indexOf('.'));
            var site = new PersonioProperties.Company(company, siteName);
            return source("personio:" + siteName, lastRun -> personio.fetchSince(site, lastRun));
        }
        if (host.endsWith(".bamboohr.com")) {
            String siteName = host.substring(0, host.indexOf('.'));
            var site = new BambooHrProperties.Company(company, siteName);
            return source("bamboohr:" + siteName, lastRun -> bambooHr.fetchSince(site, lastRun));
        }
        return Optional.empty();
    }

    private Optional<JobSource> source(String name, java.util.function.Function<java.time.Instant, List<com.kazi.kazi.ingestion.source.RawJob>> fetch) {
        return Optional.of(new RoutedJobSource(name, fetch));
    }

    private String boardSegment(URI uri) {
        List<String> segments = Arrays.stream(uri.getPath().split("/"))
            .filter(segment -> !segment.isBlank())
            .toList();
        if (segments.isEmpty()) {
            return null;
        }
        String segment = segments.getFirst();
        if (segment.matches("[a-z]{2}-[A-Z]{2}") && segments.size() > 1) {
            segment = segments.get(1);
        }
        return segment.equals("job") || segment.equals("jobs") || segment.equals("j") ? null : segment;
    }

    private String origin(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }
}
