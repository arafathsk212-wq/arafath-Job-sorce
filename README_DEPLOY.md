Deployment guide — quick steps to get a live app (free hosting)

Overview
- Backend: Deploy to Render (free web service) using Docker
- Frontend: Deploy to Vercel (free static hosting)

Preconditions
- Create GitHub account and push this repository to a remote GitHub repo.

Quick steps
1. Push repo to GitHub
   git remote add origin <YOUR_GIT_REMOTE_URL>
   git push -u origin main

2. Deploy backend on Render
   - Sign in to https://render.com
   - Create a new Web Service -> Connect to GitHub -> select repository
   - Choose branch `main`, Environment `Docker`, and set Dockerfile path to `c2c-job-sourcer-backend/Dockerfile`
   - Set the following environment variables in Render (use these names):
     - `LINKEDIN_SCRAPER_USERNAME` = your LinkedIn username
     - `LINKEDIN_SCRAPER_PASSWORD` = your LinkedIn password
     - `LINKEDIN_SCRAPER_HEADLESS` = true or false
     - (Optional) any DB/SMTP credentials if you move off H2
   - Deploy and note the service URL (e.g. `https://c2c-job-sourcer-backend.onrender.com`)

3. Deploy frontend on Vercel
   - Sign in to https://vercel.com
   - Import project from GitHub and select the repository
   - In Project Settings -> Environment Variables, set:
     - `REACT_APP_API_URL` = `https://<your-backend-host>/api`
   - Deploy. Vercel will provide a live frontend URL (e.g. `https://c2c-job-sourcer-frontend.vercel.app`)

Notes
- Spring Boot property mapping: set environment variables named `LINKEDIN_SCRAPER_USERNAME` etc. Spring Boot will map `LINKEDIN_SCRAPER_USERNAME` to `linkedin.scraper.username`.
- If you want me to push the repo and finish the deployment, provide a Git remote URL (or authorize me to create a GitHub repo). I cannot deploy to third-party services without your account credentials or connecting the repo.

Commands to run locally

# build backend image locally
cd c2c-job-sourcer-backend
docker build -t c2c-job-sourcer-backend .

# run backend locally
docker run -p 8080:8080 -e LINKEDIN_SCRAPER_USERNAME=... -e LINKEDIN_SCRAPER_PASSWORD=... c2c-job-sourcer-backend

# build frontend
cd c2c-job-sourcer-frontend
npm install
npm run build

GitHub Actions deployment (automated)

This repo includes two GitHub Actions workflows:

- `.github/workflows/deploy-backend-render.yml` — builds the backend and triggers a Render deploy via the Render API.
- `.github/workflows/deploy-frontend-vercel.yml` — builds the frontend and deploys to Vercel using the Vercel Action.

Before these workflows will succeed, add the following **Repository secrets** in GitHub (Settings → Secrets and variables → Actions → New repository secret):

- `RENDER_API_KEY` — Render API key with permission to trigger deploys.
- `RENDER_SERVICE_ID` — Render service id for your backend service (e.g. `srv-xxxxxxxx`).
- `VERCEL_TOKEN` — Vercel personal token.
- `VERCEL_ORG_ID` — Vercel organization id.
- `VERCEL_PROJECT_ID` — Vercel project id for the frontend.

After adding the secrets, push any commit to `main` or manually run the workflow from the Actions tab to trigger deploys. The backend workflow will call the Render API to start a build; Render will build using the `c2c-job-sourcer-backend/Dockerfile`.

If you prefer to trigger Render manually instead of using the API, connect the repo in the Render dashboard and enable automatic deploys for `main`.

