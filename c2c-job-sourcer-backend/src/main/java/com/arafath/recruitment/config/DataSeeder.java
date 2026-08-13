package com.arafath.recruitment.config;

import com.arafath.recruitment.model.Job;
import com.arafath.recruitment.repository.JobRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedJobs(JobRepository jobRepository) {
        return args -> {
            if (jobRepository.count() > 0) {
                return;
            }

            List<Job> sample = new ArrayList<>();

            Job j1 = new Job();
            j1.setTitle("Senior Java Developer (C2C)");
            j1.setCompany("Acme Tech");
            j1.setLocation("Remote");
            j1.setRemoteType("Fully Remote");
            j1.setDescription("Contract-to-Contract senior Java developer role. Online interview.");
            j1.setUrl("https://example.com/jobs/1");
            j1.setSource("Seed");
            j1.setJobType("Contract");
            j1.setSeniority("Senior");
            j1.setSkills("Java, Spring Boot, SQL");
            j1.setPostedDate(LocalDateTime.now().minusDays(2));

            Job j2 = new Job();
            j2.setTitle("Frontend Engineer (React) - 12 month C2C");
            j2.setCompany("Globex Corp");
            j2.setLocation("US");
            j2.setRemoteType("Remote");
            j2.setDescription("React developer for contractor engagement. Online interview.");
            j2.setUrl("https://example.com/jobs/2");
            j2.setSource("Seed");
            j2.setJobType("Contract");
            j2.setSeniority("Mid");
            j2.setSkills("React, TypeScript, CSS");
            j2.setPostedDate(LocalDateTime.now().minusDays(1));

            sample.add(j1);
            sample.add(j2);

            jobRepository.saveAll(sample);
        };
    }
}
