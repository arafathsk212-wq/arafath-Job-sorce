package com.arafath.recruitment.service;

import com.arafath.recruitment.model.Job;
import com.arafath.recruitment.repository.JobRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JobScraperService {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String NEXT_F_PUSH_PREFIX = "self.__next_f.push([1,\"";

    private final JobRepository jobRepository;

    public JobScraperService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> scrapeDice(String query, String location) throws IOException {
        String url = "https://www.dice.com/jobs?q=" + query + "&location=" + location;
        Document document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .timeout(15000)
                .get();

        Optional<String> jobListJson = extractJobListJson(document.html());

        return jobListJson.map(json -> {
            try {
                return parseJobListJson(json, location);
            } catch (JsonProcessingException e) {
                return new ArrayList<Job>();
            }
        }).orElseGet(() -> scrapeDiceFallback(document, location));
    }

    private Optional<String> extractJobListJson(String html) {
        int pos = 0;
        while (pos < html.length()) {
            int start = html.indexOf(NEXT_F_PUSH_PREFIX, pos);
            if (start < 0) {
                break;
            }

            int encodedStart = start + NEXT_F_PUSH_PREFIX.length();
            int encodedEnd = findStringLiteralEnd(html, encodedStart);
            if (encodedEnd < 0) {
                pos = encodedStart;
                continue;
            }

            String encoded = html.substring(encodedStart, encodedEnd);
            String decoded;
            try {
                decoded = decodeEscapedJsonString(encoded);
            } catch (JsonProcessingException ignored) {
                pos = encodedEnd + 1;
                continue;
            }

            int startIndex = decoded.indexOf("{\"jobList\"");
            if (startIndex >= 0) {
                int endIndex = findClosingJsonObject(decoded, startIndex);
                if (endIndex > startIndex) {
                    return Optional.of(decoded.substring(startIndex, endIndex + 1));
                }
            }

            pos = encodedEnd + 1;
        }
        return Optional.empty();
    }

    private int findStringLiteralEnd(String html, int start) {
        boolean escaped = false;
        for (int i = start; i < html.length(); i++) {
            char c = html.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private String decodeEscapedJsonString(String raw) throws JsonProcessingException {
        String quoted = "\"" + raw + "\"";
        return objectMapper.readValue(quoted, String.class);
    }

    private int findClosingJsonObject(String text, int start) {
        int depth = 0;
        boolean inQuotes = false;
        boolean escaped = false;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (c == '\\') {
                escaped = true;
                continue;
            }
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }
            if (!inQuotes) {
                if (c == '{') {
                    depth++;
                } else if (c == '}') {
                    depth--;
                    if (depth == 0) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private List<Job> parseJobListJson(String jobListJson, String defaultLocation) throws JsonProcessingException {
        List<Job> jobs = new ArrayList<>();
        JsonNode root = objectMapper.readTree(jobListJson);
        JsonNode items = root.path("jobList").path("data");
        if (!items.isArray()) {
            return jobs;
        }

        for (JsonNode item : items) {
            String title = safeText(item, "title");
            if (title.isBlank()) {
                title = safeText(item, "jobTitle");
            }
            String company = safeText(item, "companyName");
            String postingUrl = safeText(item, "detailsPageUrl");
            if (postingUrl.isBlank()) {
                postingUrl = safeText(item, "url");
            }
            String location = safeText(item.path("jobLocation"), "displayName");
            if (location.isBlank()) {
                location = defaultLocation;
            }
            String description = safeText(item, "summary");
            String employmentType = safeText(item, "employmentType");
            String remoteType = "";
            if ("TRUE".equalsIgnoreCase(safeText(item, "workFromHomeAvailability")) || "true".equalsIgnoreCase(safeText(item, "isRemote"))) {
                remoteType = "Remote";
            } else if (item.path("workplaceTypes").isArray()) {
                for (JsonNode workplaceType : item.path("workplaceTypes")) {
                    if (workplaceType.asText().toLowerCase().contains("remote")) {
                        remoteType = "Remote";
                        break;
                    }
                }
            }
            if (remoteType.isBlank()) {
                remoteType = location;
            }

            LocalDateTime postedDate = LocalDateTime.now();
            String postedDateText = safeText(item, "postedDate");
            if (!postedDateText.isBlank()) {
                try {
                    postedDate = OffsetDateTime.parse(postedDateText).toLocalDateTime();
                } catch (Exception ignored) {
                }
            }

            if (title.isBlank() || postingUrl.isBlank()) {
                continue;
            }

            Job job = new Job();
            job.setTitle(title);
            job.setCompany(company);
            job.setLocation(location);
            job.setRemoteType(remoteType);
            job.setDescription(description);
            job.setUrl(postingUrl);
            job.setSource("Dice");
            job.setJobType(employmentType);
            job.setPostedDate(postedDate);
            job.setCreatedAt(LocalDateTime.now());
            jobs.add(job);
        }

        return jobs;
    }

    private String safeText(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isTextual() ? value.asText().trim() : "";
    }

    private List<Job> scrapeDiceFallback(Document document, String location) {
        List<Job> jobs = new ArrayList<>();
        Elements cards = document.select(".card-list-item");

        if (cards.isEmpty()) {
            cards = document.select("div.card, div.card-info, div.job-card, div.search-card");
        }

        if (cards.isEmpty()) {
            cards = document.select("a[href*='/jobs/detail/'], a[href*='/jobs/']");
        }

        for (Element card : cards) {
            try {
                Element anchor = card.tagName().equals("a") ? card : card.selectFirst("a[href*='/jobs/detail/']");
                if (anchor == null) {
                    anchor = card.selectFirst("a[href*='/jobs/']");
                }
                if (anchor == null) {
                    continue;
                }

                String title = anchor.text().trim();
                if (title.isEmpty()) {
                    Element header = card.selectFirst("h2, h3, h4, h5");
                    title = header != null ? header.text().trim() : "";
                }

                String postingUrl = anchor.absUrl("href");
                if (postingUrl.isEmpty()) {
                    postingUrl = anchor.attr("href");
                    if (!postingUrl.startsWith("http")) {
                        postingUrl = "https://www.dice.com" + postingUrl;
                    }
                }

                String company = getTextFromSelectors(card, ".company", ".company-name", ".compName", "[data-cy='companyName']", "span[data-testid='company-name']");
                String locationText = getTextFromSelectors(card, ".location", ".job-location", "[data-cy='jobLocation']", "span[data-testid='job-location']");
                if (locationText.isEmpty()) {
                    locationText = location;
                }

                if (title.isEmpty() || postingUrl.isEmpty()) {
                    continue;
                }

                Job job = new Job();
                job.setTitle(title);
                job.setCompany(company);
                job.setLocation(locationText);
                job.setSource("Dice");
                job.setUrl(postingUrl);
                job.setPostedDate(LocalDateTime.now());
                job.setCreatedAt(LocalDateTime.now());
                jobs.add(job);
            } catch (Exception ignored) {
            }
        }

        return jobs;
    }

    private String getTextFromSelectors(Element element, String... selectors) {
        for (String selector : selectors) {
            Element found = element.selectFirst(selector);
            if (found != null && !found.text().trim().isEmpty()) {
                return found.text().trim();
            }
        }
        return "";
    }

    public List<Job> saveJobs(List<Job> jobs) {
        return jobRepository.saveAll(jobs);
    }

    public List<Job> scrapeIndeed(String query, String location) {
        String url = "https://www.indeed.com/jobs?q=" + query + "&l=" + location;
        Document document;
        try {
            org.jsoup.Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .referrer("https://www.google.com")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(20000)
                    .ignoreHttpErrors(true)
                    .execute();
            if (response.statusCode() != 200) {
                System.err.println("Indeed returned status " + response.statusCode() + " for " + url);
                return new ArrayList<>();
            }
            document = response.parse();
        } catch (IOException e) {
            System.err.println("Indeed scrape failed: " + e.getMessage());
            return new ArrayList<>();
        }

        List<Job> jobs = new ArrayList<>();
        Elements cards = document.select("a.tapItem, div.job_seen_beacon, div.result");
        for (Element card : cards) {
            try {
                Element anchor = card.tagName().equals("a") ? card : card.selectFirst("a");
                if (anchor == null) continue;
                String postingUrl = anchor.absUrl("href");
                if (postingUrl.isBlank()) postingUrl = anchor.attr("href");

                String title = getTextFromSelectors(card, "h2.jobTitle", "h2 span");
                String company = getTextFromSelectors(card, ".companyName", ".company");
                String locationText = getTextFromSelectors(card, ".companyLocation", ".location");
                if (locationText.isBlank()) locationText = location;

                if (title.isBlank() || postingUrl.isBlank()) continue;

                Job job = new Job();
                job.setTitle(title);
                job.setCompany(company);
                job.setLocation(locationText);
                job.setRemoteType(locationText);
                job.setSource("Indeed");
                job.setUrl(postingUrl);
                job.setPostedDate(LocalDateTime.now());
                job.setCreatedAt(LocalDateTime.now());
                jobs.add(job);
            } catch (Exception ignored) {
            }
        }
        return jobs;
    }

    public List<Job> scrapeMonster(String query, String location) {
        String url = "https://www.monster.com/jobs/search/?q=" + query + "&where=" + location;
        Document document;
        try {
            document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .referrer("https://www.monster.com")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .timeout(20000)
                    .ignoreHttpErrors(true)
                    .get();
        } catch (org.jsoup.HttpStatusException e) {
            System.err.println("Monster fetch blocked with status " + e.getStatusCode() + ": " + e.getUrl());
            return new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Monster scrape failed: " + e.getMessage());
            return new ArrayList<>();
        }

        List<Job> jobs = new ArrayList<>();
        Elements cards = document.select("section.card, div.results-card");
        if (cards.isEmpty()) {
            cards = document.select("div.card-content, div.summary, a.card-link");
        }

        for (Element card : cards) {
            try {
                Element anchor = card.selectFirst("a[href]");
                if (anchor == null) continue;
                String postingUrl = anchor.absUrl("href");
                String title = getTextFromSelectors(card, "h2.title, h3.title");
                String company = getTextFromSelectors(card, ".company, .company-name");
                String locationText = getTextFromSelectors(card, ".location, .meta");
                if (locationText.isBlank()) locationText = location;

                if (title.isBlank() || postingUrl.isBlank()) continue;

                Job job = new Job();
                job.setTitle(title);
                job.setCompany(company);
                job.setLocation(locationText);
                job.setRemoteType(locationText);
                job.setSource("Monster");
                job.setUrl(postingUrl);
                job.setPostedDate(LocalDateTime.now());
                job.setCreatedAt(LocalDateTime.now());
                jobs.add(job);
            } catch (Exception ignored) {
            }
        }
        return jobs;
    }

    @Value("${linkedin.scraper.username:}")
    private String linkedinUsername;

    @Value("${linkedin.scraper.password:}")
    private String linkedinPassword;

    @Value("${linkedin.scraper.headless:true}")
    private boolean linkedinHeadless;

    public List<Job> scrapeLinkedIn(String query, String location) {
        if (linkedinUsername.isBlank() || linkedinPassword.isBlank()) {
            System.err.println("LinkedIn credentials not configured. Skipping LinkedIn scrape.");
            return new ArrayList<>();
        }

        List<Job> jobs = new ArrayList<>();
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36");
        if (linkedinHeadless) {
            options.addArguments("--headless=new");
        }

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

            driver.get("https://www.linkedin.com/login");
            wait.until(ExpectedConditions.elementToBeClickable(By.id("username"))).sendKeys(linkedinUsername);
            driver.findElement(By.id("password")).sendKeys(linkedinPassword);
            driver.findElement(By.xpath("//button[@type='submit']")).click();
            wait.until(ExpectedConditions.or(
                    ExpectedConditions.urlContains("/feed"),
                    ExpectedConditions.urlContains("/jobs"),
                    ExpectedConditions.elementToBeClickable(By.cssSelector("nav"))
            ));
            System.err.println("LinkedIn login success current URL: " + driver.getCurrentUrl());

            String searchUrl = "https://www.linkedin.com/jobs/search/?keywords=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                    + "&location=" + URLEncoder.encode(location, StandardCharsets.UTF_8);
            driver.get(searchUrl);
            Thread.sleep(3000);
            System.err.println("LinkedIn search page title: " + driver.getTitle());
            System.err.println("LinkedIn current URL: " + driver.getCurrentUrl());

            wait.until(ExpectedConditions.or(
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("ul.jobs-search__results-list")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.job-card-container")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("li.reusable-search__result-container")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.jobs-search-results__list")),
                    ExpectedConditions.presenceOfElementLocated(By.cssSelector("li[data-occludable-job-id]")),
                    ExpectedConditions.presenceOfElementLocated(By.xpath("//a[contains(@href,'/jobs/view/')][1]"))
            ));

            for (int i = 0; i < 6; i++) {
                ((JavascriptExecutor) driver).executeScript("window.scrollBy(0, document.body.scrollHeight);");
                Thread.sleep(1200);
            }

            List<WebElement> cards = driver.findElements(By.cssSelector(
                    "li[data-occludable-job-id], li.reusable-search__result-container, ul.jobs-search__results-list li, div.job-card-container, div.scaffold-layout__list-container li, div.jobs-search-results__list li, li.job-card-container--clickable"));
            System.err.println("LinkedIn cards found: " + cards.size());
            if (cards.isEmpty()) {
                cards = driver.findElements(By.xpath("//li[contains(@class,'result-card') or @data-occludable-job-id or .//a[contains(@href,'/jobs/view/')]]"));
                System.err.println("Fallback LinkedIn cards found: " + cards.size());
            }
            if (cards.isEmpty()) {
                List<WebElement> anchors = driver.findElements(By.xpath("//a[contains(@href,'/jobs/view/') and not(contains(@href,'/company/'))]"));
                System.err.println("LinkedIn job anchors found: " + anchors.size());
                for (WebElement anchor : anchors) {
                    try {
                        String postingUrl = anchor.getAttribute("href");
                        if (postingUrl == null || postingUrl.isBlank()) continue;

                        WebElement card = anchor.findElement(By.xpath("ancestor::li[1]"));
                        String title = safeText(card, "h3, h2");
                        String company = safeText(card, ".base-search-card__subtitle a, .job-card-container__company-name, .base-search-card__subtitle, .job-card-container__company-name-link");
                        String locationText = safeText(card, ".job-search-card__location, .base-search-card__metadata, .job-card-container__metadata-item, .job-card-list__location, .job-card__location");
                        String metadata = safeText(card, ".job-card-container__metadata-item, .base-search-card__metadata, .job-card__metadata, .job-card-list__metadata-item");
                        String summary = card.getText().trim();

                        if (title.isBlank()) continue;

                        Job job = new Job();
                        job.setTitle(title);
                        job.setCompany(company);
                        job.setLocation(locationText.isBlank() ? location : locationText);
                        job.setRemoteType(locationText.isBlank() ? location : locationText);
                        job.setJobType(detectLinkedInJobType(metadata + " " + summary));
                        job.setDescription(summary);
                        job.setSource("LinkedIn");
                        job.setUrl(postingUrl);
                        job.setPostedDate(LocalDateTime.now());
                        job.setCreatedAt(LocalDateTime.now());
                        jobs.add(job);
                    } catch (Exception ignored) {
                    }
                }
            } else {
                for (WebElement card : cards) {
                    try {
                        String title = safeText(card, "h3, h2");
                        String company = safeText(card, ".base-search-card__subtitle a, .job-card-container__company-name, .base-search-card__subtitle, .job-card-container__company-name-link");
                        String locationText = safeText(card, ".job-search-card__location, .base-search-card__metadata, .job-card-container__metadata-item, .job-card-list__location, .job-card__location");
                        String metadata = safeText(card, ".job-card-container__metadata-item, .base-search-card__metadata, .job-card__metadata, .job-card-list__metadata-item");
                        String summary = card.getText().trim();
                        WebElement anchor = card.findElement(By.cssSelector("a.base-card__full-link, a.job-card-list__title, a.job-card-container__link, a[href*='/jobs/view/']"));
                        String postingUrl = anchor.getAttribute("href");

                        if (title.isBlank() || postingUrl == null || postingUrl.isBlank()) {
                            continue;
                        }

                        Job job = new Job();
                        job.setTitle(title);
                        job.setCompany(company);
                        job.setLocation(locationText.isBlank() ? location : locationText);
                        job.setRemoteType(locationText.isBlank() ? location : locationText);
                        job.setJobType(detectLinkedInJobType(metadata + " " + summary));
                        job.setDescription(summary);
                        job.setSource("LinkedIn");
                        job.setUrl(postingUrl);
                        job.setPostedDate(LocalDateTime.now());
                        job.setCreatedAt(LocalDateTime.now());
                        jobs.add(job);
                    } catch (Exception ignored) {
                    }
                }
            }
            if (jobs.isEmpty()) {
                String sourceSnippet = driver.getPageSource();
                System.err.println("LinkedIn page source length: " + sourceSnippet.length());
                System.err.println("Page source snippet: " + sourceSnippet.substring(0, Math.min(1200, sourceSnippet.length())));
            }
        } catch (Exception e) {
            System.err.println("LinkedIn scrape failed: " + e.getMessage());
        } finally {
            if (driver != null) {
                try {
                    driver.quit();
                } catch (Exception ignored) {
                }
            }
        }

        return jobs;
    }

    private String safeText(WebElement element, String cssSelector) {
        try {
            WebElement found = element.findElement(By.cssSelector(cssSelector));
            return found.getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String detectLinkedInJobType(String text) {
        String lower = text == null ? "" : text.toLowerCase();
        if (lower.contains("c2c") || lower.contains("contract") || lower.contains("1099") || lower.contains("staffing")) {
            return "C2C/Contract";
        }
        if (lower.contains("full time") || lower.contains("permanent")) {
            return "Full Time";
        }
        if (lower.contains("part time")) {
            return "Part Time";
        }
        return "N/A";
    }

    // Orchestrator: run multiple scrapers and persist unique results
    public List<Job> scrapeAll(String query, String location) {
        List<Job> all = new ArrayList<>();
        try {
            all.addAll(scrapeDice(query, location));
        } catch (IOException e) {
            System.err.println("Dice scrape failed: " + e.getMessage());
        }
        all.addAll(scrapeIndeed(query, location));
        all.addAll(scrapeMonster(query, location));
        all.addAll(scrapeLinkedIn(query, location));

        // dedupe by URL
        List<Job> unique = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (Job existing : jobRepository.findAll()) {
            if (existing.getUrl() != null) seen.add(existing.getUrl());
        }

        for (Job j : all) {
            if (j.getUrl() == null || j.getUrl().isBlank()) continue;
            if (seen.contains(j.getUrl())) continue;
            seen.add(j.getUrl());
            unique.add(j);
        }

        if (!unique.isEmpty()) {
            jobRepository.saveAll(unique);
        }

        return unique;
    }
}
