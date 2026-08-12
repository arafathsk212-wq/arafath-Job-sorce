package com.arafath.recruitment.service;

import com.arafath.recruitment.dto.JobFilterRequest;
import com.arafath.recruitment.model.Job;
import com.arafath.recruitment.repository.JobRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JobService {

    private final JobRepository jobRepository;

    public JobService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public Optional<Job> getJobById(Long id) {
        return jobRepository.findById(id);
    }

    public Job saveJob(Job job) {
        return jobRepository.save(job);
    }

    public List<Job> getFilteredJobs(JobFilterRequest filter) {
        return jobRepository.findAll().stream()
                .filter(job -> filter.getTitle() == null || filter.getTitle().isBlank() || job.getTitle().toLowerCase().contains(filter.getTitle().toLowerCase()))
                .filter(job -> filter.getCompany() == null || filter.getCompany().isBlank() || job.getCompany().toLowerCase().contains(filter.getCompany().toLowerCase()))
                .filter(job -> filter.getLocation() == null || filter.getLocation().isBlank() || job.getLocation().toLowerCase().contains(filter.getLocation().toLowerCase()))
                .filter(job -> filter.getRemoteType() == null || filter.getRemoteType().isBlank() || (job.getRemoteType() != null && job.getRemoteType().toLowerCase().contains(filter.getRemoteType().toLowerCase())))
                .filter(job -> filter.getJobType() == null || filter.getJobType().isBlank() || (job.getJobType() != null && job.getJobType().toLowerCase().contains(filter.getJobType().toLowerCase())))
                .filter(job -> filter.getSeniority() == null || filter.getSeniority().isBlank() || (job.getSeniority() != null && job.getSeniority().toLowerCase().contains(filter.getSeniority().toLowerCase())))
                .filter(job -> filter.getSkills() == null || filter.getSkills().isBlank() || (job.getSkills() != null && job.getSkills().toLowerCase().contains(filter.getSkills().toLowerCase())))
                // Filter: only C2C jobs when requested — treat common contract/third-party labels as C2C
                .filter(job -> {
                    if (filter.getOnlyC2C() == null || !filter.getOnlyC2C()) return true;
                    String jt = job.getJobType() == null ? "" : job.getJobType().toLowerCase();
                    String title = job.getTitle() == null ? "" : job.getTitle().toLowerCase();
                    String company = job.getCompany() == null ? "" : job.getCompany().toLowerCase();
                    String desc = job.getDescription() == null ? "" : job.getDescription().toLowerCase();
                    return jt.contains("c2c") || jt.contains("contract") || jt.contains("third") ||
                            title.contains("c2c") || company.contains("c2c") || desc.contains("c2c");
                })
                // Filter: interview offline (onsite / in-person)
                .filter(job -> {
                    if (filter.getInterviewOffline() == null || !filter.getInterviewOffline()) return true;
                    String rt = job.getRemoteType() == null ? "" : job.getRemoteType().toLowerCase();
                    // common onsite indicators
                    if (rt.contains("onsite") || rt.contains("in-person") || rt.contains("in person") || rt.contains("on-site")) return true;
                    // if remoteType looks like a city/state (contains a comma and letters), treat as onsite
                    if (rt.matches(".*[a-z].*,.*[a-z].*")) return true;
                    // if remoteType explicitly says remote/hybrid, it's not offline
                    if (rt.contains("remote") || rt.contains("hybrid")) return false;
                    // fallback: if remoteType non-empty and not remote/hybrid, treat as onsite
                    return !rt.isBlank();
                })
                // Filter: interview online
                .filter(job -> {
                    if (filter.getInterviewOnline() == null || !filter.getInterviewOnline()) return true;
                    String rt = job.getRemoteType() == null ? "" : job.getRemoteType().toLowerCase();
                    return rt.contains("remote") || rt.contains("work from home") || rt.contains("wfh") || rt.contains("online") || rt.contains("hybrid");
                })
                // Filter: LinkedIn-only jobs
                .filter(job -> {
                    if (filter.getLinkedinOnly() == null || !filter.getLinkedinOnly()) return true;
                    return job.getSource() != null && job.getSource().equalsIgnoreCase("LinkedIn");
                })
                // Heuristic filter: visa/GC support — accept jobs unless they explicitly require US citizenship
                .filter(job -> {
                    if (filter.getVisaGC() == null || !filter.getVisaGC()) return true;
                    String desc = job.getDescription() == null ? "" : job.getDescription().toLowerCase();
                    String title = job.getTitle() == null ? "" : job.getTitle().toLowerCase();
                    String company = job.getCompany() == null ? "" : job.getCompany().toLowerCase();
                    String combined = desc + " " + title + " " + company;
                    // If the job explicitly requires US citizenship, exclude it for GC candidates
                    if (combined.contains("must be a us citizen") || combined.contains("must be us citizen") || combined.contains("us citizen only") || combined.contains("u.s. citizen") || combined.contains("us citizen")) return false;
                    if (combined.contains("citizen only") || combined.contains("must be citizen")) return false;
                    // Otherwise assume GC holders are acceptable (best-effort)
                    return true;
                })
                .toList();
    }

    // More permissive candidate-oriented matching to surface C2C + GC-friendly + offline jobs
    public List<Job> getCandidateMatches(JobFilterRequest filter) {
        return jobRepository.findAll().stream()
                .filter(job -> filter.getTitle() == null || filter.getTitle().isBlank() || (job.getTitle() != null && job.getTitle().toLowerCase().contains(filter.getTitle().toLowerCase())))
                // Only C2C/contract heuristics
                .filter(job -> {
                    if (filter.getOnlyC2C() == null || !filter.getOnlyC2C()) return true;
                    String jt = job.getJobType() == null ? "" : job.getJobType().toLowerCase();
                    String title = job.getTitle() == null ? "" : job.getTitle().toLowerCase();
                    String desc = job.getDescription() == null ? "" : job.getDescription().toLowerCase();
                    return jt.contains("contract") || jt.contains("third") || jt.contains("c2c") || title.contains("contract") || desc.contains("contract") || desc.contains("c2c");
                })
                // Visa/GC: exclude only if job explicitly requires US citizenship
                .filter(job -> {
                    if (filter.getVisaGC() == null || !filter.getVisaGC()) return true;
                    String combined = (job.getDescription() == null ? "" : job.getDescription().toLowerCase()) + " " + (job.getTitle() == null ? "" : job.getTitle().toLowerCase()) + " " + (job.getCompany() == null ? "" : job.getCompany().toLowerCase());
                    if (combined.contains("must be a us citizen") || combined.contains("must be us citizen") || combined.contains("us citizen only") || combined.contains("u.s. citizen") || combined.contains("us citizen")) return false;
                    if (combined.contains("must be citizen") || combined.contains("citizen only")) return false;
                    return true;
                })
                // Interview offline: prefer onsite / location-specified jobs
                .filter(job -> {
                    if (filter.getInterviewOffline() == null || !filter.getInterviewOffline()) return true;
                    String rt = job.getRemoteType() == null ? "" : job.getRemoteType().toLowerCase();
                    if (rt.contains("remote") || rt.contains("hybrid")) return false;
                    if (!rt.isBlank()) return true;
                    return true;
                })
                // Interview online: prefer remote/hybrid/online jobs
                .filter(job -> {
                    if (filter.getInterviewOnline() == null || !filter.getInterviewOnline()) return true;
                    String rt = job.getRemoteType() == null ? "" : job.getRemoteType().toLowerCase();
                    return rt.contains("remote") || rt.contains("hybrid") || rt.contains("work from home") || rt.contains("wfh") || rt.contains("online");
                })
                // LinkedIn-only filter
                .filter(job -> {
                    if (filter.getLinkedinOnly() == null || !filter.getLinkedinOnly()) return true;
                    return job.getSource() != null && job.getSource().equalsIgnoreCase("LinkedIn");
                })
                .limit(200)
                .toList();
    }
}
