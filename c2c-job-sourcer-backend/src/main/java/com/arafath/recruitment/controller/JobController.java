package com.arafath.recruitment.controller;

import com.arafath.recruitment.dto.JobFilterRequest;
import com.arafath.recruitment.model.Job;
import com.arafath.recruitment.service.JobScraperService;
import com.arafath.recruitment.service.JobService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@CrossOrigin(origins = "*")
public class JobController {

    private final JobService jobService;
    private final JobScraperService jobScraperService;

    public JobController(JobService jobService, JobScraperService jobScraperService) {
        this.jobService = jobService;
        this.jobScraperService = jobScraperService;
    }

    @GetMapping
    public List<Job> getAllJobs() {
        return jobService.getAllJobs();
    }

    @PostMapping("/filter")
    public List<Job> getFilteredJobs(@RequestBody JobFilterRequest filterRequest) {
        return jobService.getFilteredJobs(filterRequest);
    }

    @PostMapping("/filter/candidate")
    public List<Job> getCandidateMatches(@RequestBody JobFilterRequest filterRequest) {
        return jobService.getCandidateMatches(filterRequest);
    }

    @PostMapping("/scrape/dice")
    public ResponseEntity<List<Job>> scrapeDice(@RequestParam String query, @RequestParam String location) {
        try {
            List<Job> jobs = jobScraperService.scrapeDice(query, location);
            jobScraperService.saveJobs(jobs);
            return ResponseEntity.ok(jobs);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/scrape/all")
    public ResponseEntity<List<Job>> scrapeAll(@RequestParam String query, @RequestParam String location) {
        List<Job> jobs = jobScraperService.scrapeAll(query, location);
        return ResponseEntity.ok(jobs);
    }

    @PostMapping("/scrape/linkedin")
    public ResponseEntity<List<Job>> scrapeLinkedIn(@RequestParam String query, @RequestParam String location) {
        List<Job> jobs = jobScraperService.scrapeLinkedIn(query, location);
        if (!jobs.isEmpty()) {
            jobScraperService.saveJobs(jobs);
        }
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJobById(@PathVariable Long id) {
        return jobService.getJobById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Job> createJob(@RequestBody Job job) {
        Job savedJob = jobService.saveJob(job);
        return ResponseEntity.ok(savedJob);
    }
}
