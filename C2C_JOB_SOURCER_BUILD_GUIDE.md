================================================================================
COPILOT PROMPT: C2C JOB SOURCER - COMPLETE BUILD GUIDE
================================================================================

You are an expert full-stack developer helping build a C2C Job Sourcer application.
The user is a Java Full Stack Developer building this for recruitment sourcing.

================================================================================
PROJECT OVERVIEW
================================================================================

PROJECT NAME: C2C Job Sourcer
PURPOSE: Auto-scrape job postings from multiple portals, filter by C2C criteria,
extract recruiter contacts, and send emails to matching jobs with one click.

PROBLEM IT SOLVES:
- Currently spending 2+ hours daily manually scrolling job portals
- Missing out on jobs because they're posted too fast
- Time-consuming to extract recruiter email addresses
- No centralized place to track which jobs were sent to which recruiters

SOLUTION:
- Automated job scraper running 24/7
- Smart filtering by location, job type, skills, remote option
- Automatic recruiter contact extraction
- Dashboard to review and send with one click
- Analytics to track performance

USER PROFILE:
- Name: Arafath
- Skills: Java, Spring Boot, REST APIs, MySQL, Docker, Flutter, Git
- Candidates: Raksha (Java Full Stack, 10 yrs, GC), Pravalika (.NET, 10+ yrs)
- Email: arafathsk212@gmail.com
- Phone: +91 6300901257

================================================================================
TECH STACK (FINAL)
================================================================================

FRONTEND:
- React 18
- Tailwind CSS
- Axios (HTTP client)
- React Router
- Recharts (analytics)

BACKEND:
- Spring Boot 3.x
- Java 17+
- Spring Data JPA
- Spring Scheduling
- Spring Mail
- Spring Security

SCRAPING LIBRARIES:
- Jsoup (HTML parsing)
- Selenium WebDriver (JavaScript-heavy sites)
- Playwright (alternative to Selenium)

DATABASE:
- MySQL 8.0
- JPA/Hibernate ORM

EMAIL:
- Gmail API
- JavaMailSender (Spring)

DEPLOYMENT:
- Railway.app (Backend + Database)
- Vercel (Frontend)
- GitHub (Version Control)

TOOLS:
- Maven (Build)
- Git (Version Control)
- Postman (API Testing)
- Visual Studio Code or IntelliJ IDEA

================================================================================
DATABASE SCHEMA
================================================================================

TABLE 1: candidates
├─ id (INT, PRIMARY KEY)
├─ name (VARCHAR(100), NOT NULL)
├─ tech_stack (VARCHAR(500))
├─ years_experience (INT)
├─ visa_status (VARCHAR(50)) -- GC, H1B, OPT, Citizen
├─ current_rate (INT)
├─ current_location (VARCHAR(100))
├─ willing_to_relocate (BOOLEAN)
├─ resume_url (VARCHAR(255))
├─ skills (TEXT)
├─ created_at (TIMESTAMP)
└─ updated_at (TIMESTAMP)

TABLE 2: jobs
├─ id (BIGINT, PRIMARY KEY)
├─ job_title (VARCHAR(200), NOT NULL)
├─ company_name (VARCHAR(200), NOT NULL)
├─ job_description (LONGTEXT)
├─ location (VARCHAR(100))
├─ job_type (VARCHAR(50)) -- C2C, W2, Contract
├─ salary_range (VARCHAR(100))
├─ required_skills (VARCHAR(500))
├─ remote_option (VARCHAR(50)) -- Remote, Hybrid, Onsite
├─ recruiter_name (VARCHAR(100))
├─ recruiter_email (VARCHAR(100))
├─ recruiter_phone (VARCHAR(20))
├─ company_website (VARCHAR(255))
├─ source_portal (VARCHAR(50)) -- Dice, Indeed, LinkedIn, AngelList, etc
├─ source_url (LONGTEXT)
├─ posted_date (DATETIME)
├─ extracted_date (DATETIME)
├─ is_matched_to_candidate (BOOLEAN, DEFAULT false)
└─ created_at (TIMESTAMP)

TABLE 3: job_applications
├─ id (BIGINT, PRIMARY KEY)
├─ job_id (BIGINT, FOREIGN KEY -> jobs.id)
├─ candidate_id (INT, FOREIGN KEY -> candidates.id)
├─ status (VARCHAR(50)) -- Sent, Opened, Replied, Interviewing, Rejected, Placed
├─ email_sent_date (DATETIME)
├─ recruiter_response (TEXT)
├─ response_date (DATETIME)
├─ interview_scheduled_date (DATETIME)
├─ interview_status (VARCHAR(50)) -- Scheduled, Completed, Offered, Rejected
├─ notes (TEXT)
└─ created_at (TIMESTAMP)

TABLE 4: portal_credentials
├─ id (INT, PRIMARY KEY)
├─ portal_name (VARCHAR(100)) -- Dice, Indeed, LinkedIn, etc
├─ api_key (VARCHAR(255))
├─ username (VARCHAR(100))
├─ password (VARCHAR(255)) -- Encrypted
├─ last_scraped (DATETIME)
├─ is_active (BOOLEAN)
└─ created_at (TIMESTAMP)

TABLE 5: email_templates
├─ id (INT, PRIMARY KEY)
├─ template_name (VARCHAR(100))
├─ subject (VARCHAR(200))
├─ body (LONGTEXT)
├─ placeholders (VARCHAR(500)) -- {recruiter_name}, {job_title}, {candidate_name}
├─ created_at (TIMESTAMP)
└─ updated_at (TIMESTAMP)

TABLE 6: scraping_logs
├─ id (BIGINT, PRIMARY KEY)
├─ portal_name (VARCHAR(100))
├─ jobs_scraped (INT)
├─ jobs_matched (INT)
├─ errors (TEXT)
├─ execution_time_ms (BIGINT)
├─ status (VARCHAR(50)) -- Success, Failed, PartialSuccess
├─ scraped_at (DATETIME)
└─ created_at (TIMESTAMP)

================================================================================
API ENDPOINTS (Backend Specifications)
================================================================================

BASE URL: http://localhost:8080/api

--- JOBS ---
GET    /jobs/filtered           → Get filtered jobs matching criteria
GET    /jobs/{id}               → Get job details
GET    /jobs/search?q=Java      → Search jobs
POST   /jobs/filter             → Apply custom filters
DELETE /jobs/{id}               → Delete job from dashboard

--- JOB APPLICATIONS ---
POST   /applications/send       → Send email to recruiter
GET    /applications/{id}       → Get application status
GET    /applications/stats      → Get overall stats (sent, replied, interviewed)
GET    /applications/history    → Get full history with filters

--- CANDIDATES ---
GET    /candidates              → List all candidates
GET    /candidates/{id}         → Get candidate details
POST   /candidates              → Add new candidate
PUT    /candidates/{id}         → Update candidate
DELETE /candidates/{id}         → Delete candidate

--- EMAIL TEMPLATES ---
GET    /email-templates         → Get all templates
POST   /email-templates         → Create new template
PUT    /email-templates/{id}    → Update template
DELETE /email-templates/{id}    → Delete template

--- SCRAPING ---
POST   /scraper/start           → Manually trigger scraping
GET    /scraper/status          → Get scraping status
GET    /scraper/logs            → Get scraping logs
GET    /scraper/schedule        → Get scheduled scraping info

--- ANALYTICS ---
GET    /analytics/dashboard     → Get dashboard stats
GET    /analytics/best-portals  → Get best performing portals
GET    /analytics/best-recruiters → Get most responsive recruiters

================================================================================
FEATURE BREAKDOWN (WEEK BY WEEK)
================================================================================

WEEK 1: SCRAPING FOUNDATION
--- Day 1-2: Setup Project
- Create Spring Boot project with Maven
- Create React project
- Setup MySQL database
- Create database tables
- Setup Git repository

--- Day 3-4: Dice.com Scraper
- Build Dice job scraper using Jsoup
- Extract: title, company, description, location, recruiter email
- Store in database
- Build error handling and logging
- Test with 100 jobs

--- Day 5: Indeed.com Scraper
- Build Indeed scraper using Jsoup
- Extract job details
- Handle pagination
- Store in database
- Test thoroughly

WEEK 2: MORE PORTALS + FILTERING
--- Day 1-2: AngelList Scraper
- Build AngelList/Wellfound API integration
- Extract startup jobs
- Store with company info

--- Day 3-4: LinkedIn Scraper (HARD)
- Use Selenium WebDriver for LinkedIn
- Handle login (or use API)
- Extract job postings
- Extract recruiter profile info
- Handle rate limiting

--- Day 5: Smart Filtering
- Build filtering logic:
  * Location: USA only
  * Job type: C2C/Contract
  * Skills: Java, Full Stack, DevOps, etc.
  * Remote option: Remote, Hybrid, or Onsite
  * Exclude: Onsite only (unless in candidate's location)
- Test filters with real data

WEEK 3: DASHBOARD + EMAIL
--- Day 1-2: React Dashboard
- Build job listing page
- Display filtered jobs with cards
- Show job details on click
- Search functionality
- Pagination

--- Day 3: Email Integration
- Setup Gmail API authentication
- Build email sending service
- Create email template system
- Build email tracking

--- Day 4-5: One-Click Send
- Add "Send to [Candidate]" button
- Auto-fill recruiter name, job title
- Send email with candidate resume
- Track in database
- Show confirmation

WEEK 4: TRACKING + DEPLOYMENT
--- Day 1-2: Analytics Dashboard
- Build stats page
- Show: Jobs Sent, Responses, Interviews, Conversion Rate
- Best performing portals
- Best recruiters
- Charts with Recharts

--- Day 3: Email Tracking
- Track email opens (Gmail API)
- Track responses manually
- Update application status
- Show in dashboard

--- Day 4-5: Deploy
- Deploy backend to Railway.app
- Deploy database to Railway.app
- Deploy frontend to Vercel
- Setup GitHub CI/CD
- Test production environment

================================================================================
SPRING BOOT BACKEND CODE STRUCTURE
================================================================================

PROJECT STRUCTURE:
```
c2c-job-sourcer-backend/
├── src/main/java/com/arafath/recruitment/
│   ├── config/
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   └── MailConfig.java
│   ├── controller/
│   │   ├── JobController.java
│   │   ├── ApplicationController.java
│   │   ├── CandidateController.java
│   │   ├── ScraperController.java
│   │   ├── AnalyticsController.java
│   │   └── EmailTemplateController.java
│   ├── service/
│   │   ├── JobService.java
│   │   ├── ApplicationService.java
│   │   ├── CandidateService.java
│   │   ├── ScraperService.java
│   │   ├── EmailService.java
│   │   ├── AnalyticsService.java
│   │   ├── scraper/
│   │   │   ├── DiceJobScraper.java
│   │   │   ├── IndeedJobScraper.java
│   │   │   ├── LinkedInJobScraper.java
│   │   │   ├── AngelListScraper.java
│   │   │   └── JobScraperOrchestrator.java
│   │   └── contact/
│   │       ├── RecruiterContactExtractor.java
│   │       └── EmailExtractor.java
│   ├── repository/
│   │   ├── JobRepository.java
│   │   ├── ApplicationRepository.java
│   │   ├── CandidateRepository.java
│   │   ├── EmailTemplateRepository.java
│   │   └── ScrapingLogRepository.java
│   ├── entity/
│   │   ├── Job.java
│   │   ├── JobApplication.java
│   │   ├── Candidate.java
│   │   ├── EmailTemplate.java
│   │   └── ScrapingLog.java
│   ├── dto/
│   │   ├── JobDTO.java
│   │   ├── ApplicationDTO.java
│   │   ├── CandidateDTO.java
│   │   ├── FilterCriteriaDTO.java
│   │   ├── AnalyticsDTO.java
│   │   └── ApplicationRequest.java
│   ├── exception/
│   │   ├── JobNotFoundException.java
│   │   ├── ScraperException.java
│   │   └── GlobalExceptionHandler.java
│   ├── util/
│   │   ├── EmailValidator.java
│   │   ├── SkillsMatcher.java
│   │   ├── DateUtil.java
│   │   └── StringUtil.java
│   └── RecruitmentApplication.java
├── src/main/resources/
│   ├── application.properties
│   ├── application-prod.properties
│   └── templates/
│       └── email-templates/
│           ├── job-submission-template.html
│           └── follow-up-template.html
├── pom.xml
└── README.md
```

================================================================================
SETUP INSTRUCTIONS (STEP-BY-STEP)
================================================================================

STEP 1: DATABASE SETUP
```bash
# Open MySQL
mysql -u root -p

# Create database
CREATE DATABASE c2c_job_sourcer;
USE c2c_job_sourcer;

# Tables will be created automatically by Hibernate (ddl-auto=update)
```

STEP 2: BACKEND SETUP
```bash
# Create Spring Boot project
mvn archetype:generate -DgroupId=com.arafath.recruitment -DartifactId=c2c-job-sourcer-backend

# Navigate to project
cd c2c-job-sourcer-backend

# Copy pom.xml dependencies (from above)
# Copy src/main/java files (from above)
# Copy application.properties (from above)

# Update application.properties with your credentials

# Run project
mvn spring-boot:run
```

STEP 3: FRONTEND SETUP
```bash
# Create React project
npx create-react-app c2c-job-sourcer-frontend

# Navigate to project
cd c2c-job-sourcer-frontend

# Install dependencies
npm install axios react-router-dom recharts

# Copy src files (from above)

# Create .env file
echo "REACT_APP_API_URL=http://localhost:8080/api" > .env

# Run frontend
npm start
```

STEP 4: TEST LOCALLY
```
Backend: http://localhost:8080/api
Frontend: http://localhost:3000
```

STEP 5: DEPLOYMENT
```
# Backend: Deploy to Railway.app
# Frontend: Deploy to Vercel
# Database: Railway PostgreSQL
```

================================================================================
IMPLEMENTATION CHECKLIST
================================================================================

WEEK 1:
☐ Setup Spring Boot project
☐ Setup React project
☐ Create MySQL database + tables
☐ Setup Git repo
☐ Build Dice scraper
☐ Build Indeed scraper
☐ Test scrapers with sample data

WEEK 2:
☐ Build LinkedIn scraper (Selenium)
☐ Build AngelList scraper
☐ Implement job filtering logic
☐ Test filters thoroughly
☐ Build React dashboard UI

WEEK 3:
☐ Setup Gmail API
☐ Build email sending service
☐ Build React job listing page
☐ Implement one-click send button
☐ Test email functionality

WEEK 4:
☐ Build analytics dashboard
☐ Implement email tracking
☐ Deploy backend to Railway
☐ Deploy database to Railway
☐ Deploy frontend to Vercel
☐ Test production environment

================================================================================
COMMON ISSUES & SOLUTIONS
================================================================================

ISSUE 1: LinkedIn Scraper Not Working
SOLUTION: LinkedIn blocks bots. Use Selenium + account login or switch to LinkedIn Jobs API

ISSUE 2: Gmail Sending Emails to Spam
SOLUTION: Setup SPF/DKIM records, use app-specific password, warmup account gradually

ISSUE 3: Database Connection Failing
SOLUTION: Check credentials in application.properties, verify MySQL is running

ISSUE 4: React API Calls Return 404
SOLUTION: Verify CORS settings, ensure backend is running, check API endpoint URLs

ISSUE 5: Jobs Duplicating in Database
SOLUTION: Add unique constraint on (source_url + source_portal), check before insert

================================================================================
NEXT STEPS AFTER BUILDING
================================================================================

1. Start using for real placements
   - Add your actual candidates
   - Start sourcing jobs
   - Track which recruiters respond

2. Optimize scrapers
   - Add more job portals
   - Improve filter accuracy
   - Reduce scraping time

3. Add features
   - Recruiter scoring (best performers)
   - Candidate matching algorithm
   - Interview scheduling integration
   - Slack/email notifications

4. Monetize
   - Charge other recruiters subscription fee
   - $50-100/month per user
   - SaaS business model

================================================================================
GET CODING! 🚀
================================================================================

You now have everything needed to build this tool.

Start with Week 1: Setup + Dice scraper
Then move through Week 2-4 in order

This tool will:
✅ Save 10+ hours per week
✅ Get you more placements
✅ Track your performance
✅ Become valuable product to sell

BEGIN WITH STEP 1 TODAY!
