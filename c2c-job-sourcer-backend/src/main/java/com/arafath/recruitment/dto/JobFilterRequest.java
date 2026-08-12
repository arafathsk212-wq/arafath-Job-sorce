package com.arafath.recruitment.dto;

public class JobFilterRequest {

    private String title;
    private String company;
    private String location;
    private String remoteType;
    private String jobType;
    private String seniority;
    private String skills;
    private Boolean onlyC2C;
    private Boolean interviewOffline;
    private Boolean interviewOnline;
    private Boolean linkedinOnly;
    private Boolean visaGC;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRemoteType() {
        return remoteType;
    }

    public void setRemoteType(String remoteType) {
        this.remoteType = remoteType;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getSeniority() {
        return seniority;
    }

    public void setSeniority(String seniority) {
        this.seniority = seniority;
    }

    public String getSkills() {
        return skills;
    }

    public void setSkills(String skills) {
        this.skills = skills;
    }

    public Boolean getOnlyC2C() {
        return onlyC2C;
    }

    public void setOnlyC2C(Boolean onlyC2C) {
        this.onlyC2C = onlyC2C;
    }

    public Boolean getInterviewOffline() {
        return interviewOffline;
    }

    public void setInterviewOffline(Boolean interviewOffline) {
        this.interviewOffline = interviewOffline;
    }

    public Boolean getInterviewOnline() {
        return interviewOnline;
    }

    public void setInterviewOnline(Boolean interviewOnline) {
        this.interviewOnline = interviewOnline;
    }

    public Boolean getLinkedinOnly() {
        return linkedinOnly;
    }

    public void setLinkedinOnly(Boolean linkedinOnly) {
        this.linkedinOnly = linkedinOnly;
    }

    public Boolean getVisaGC() {
        return visaGC;
    }

    public void setVisaGC(Boolean visaGC) {
        this.visaGC = visaGC;
    }
}
