import React, { useEffect, useState } from 'react';
import './App.css';

function App() {
  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [query, setQuery] = useState('java');
  const [location, setLocation] = useState('remote');
  const [titleFilter] = useState('');
  const [companyFilter] = useState('');
  const [onlyC2C, setOnlyC2C] = useState(true);
  const [visaGC, setVisaGC] = useState(true);
  const [interviewOffline, setInterviewOffline] = useState(false);
  const [interviewOnline, setInterviewOnline] = useState(true);
  const [linkedinOnly, setLinkedinOnly] = useState(true);
  const [message, setMessage] = useState('');
  const apiBase = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

  const loadJobs = () => {
    setLoading(true);
    setError(null);
    fetch(`${apiBase}/jobs`)
      .then((response) => response.json())
      .then((data) => {
        setJobs(data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  };

  // loadJobs intentionally not included in deps to run only on mount
  // eslint-disable-next-line react-hooks/exhaustive-deps
  useEffect(() => {
    loadJobs();
  }, []);

  const handleScrape = () => {
    setMessage('Scraping Dice...');
    fetch(`${apiBase}/jobs/scrape/dice?query=${encodeURIComponent(query)}&location=${encodeURIComponent(location)}`, {
      method: 'POST',
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Scrape failed');
        }
        return response.json();
      })
      .then((data) => {
        setMessage(`Saved ${data.length} scraped jobs.`);
        setJobs(data);
      })
      .catch((err) => {
        setError(err.message);
        setMessage('');
      });
  };

  const handleFilter = () => {
    setMessage('Applying filters...');
    fetch(`${apiBase}/jobs/filter`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: titleFilter,
        company: companyFilter,
        location,
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Filter request failed');
        }
        return response.json();
      })
      .then((data) => {
        setJobs(data);
        setMessage(`Found ${data.length} matching jobs.`);
      })
      .catch((err) => {
        setError(err.message);
        setMessage('');
      });
  };

  const handleCandidateSearch = () => {
    // Search for the current query as a candidate-focused search:
    // only C2C, visa GC (heuristic), interview online, and LinkedIn only
    setMessage('Searching jobs for candidate (LinkedIn, C2C, GC, online)...');
    fetch(`${apiBase}/jobs/filter/candidate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: query,
        onlyC2C,
        interviewOffline,
        interviewOnline,
        linkedinOnly,
        visaGC
      }),
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Filter request failed');
        }
        return response.json();
      })
      .then((data) => {
        setJobs(data);
        setMessage(`Found ${data.length} matching jobs for candidate.`);
      })
      .catch((err) => {
        setError(err.message);
        setMessage('');
      });
  };

  const handleScrapeAll = () => {
    setMessage('Scraping all portals...');
    fetch(`${apiBase}/jobs/scrape/all?query=${encodeURIComponent(query)}&location=${encodeURIComponent(location)}`, {
      method: 'POST',
    })
      .then((response) => {
        if (!response.ok) {
          throw new Error('Scrape all request failed');
        }
        return response.json();
      })
      .then((data) => {
        setMessage(`Saved ${data.length} scraped jobs from all portals.`);
        setJobs(data);
      })
      .catch((err) => {
        setError(err.message);
        setMessage('');
      });
  };

  const handleScrapeLinkedIn = () => {
    setMessage('Scraping LinkedIn...');
    fetch(`${apiBase}/jobs/scrape/linkedin?query=${encodeURIComponent(query)}&location=${encodeURIComponent(location)}`, {
      method: 'POST',
    })
      .then((response) => {
        if (response.status === 204) {
          setMessage('No LinkedIn jobs were returned.');
          return [];
        }
        if (!response.ok) {
          throw new Error('LinkedIn scrape request failed');
        }
        return response.json();
      })
      .then((data) => {
        if (data.length > 0) {
          setMessage(`Saved ${data.length} LinkedIn jobs.`);
          setJobs(data);
        } else {
          setMessage('No LinkedIn jobs were returned.');
        }
      })
      .catch((err) => {
        setError(err.message);
        setMessage('');
      });
  };

  if (loading) {
    return <div className="App">Loading jobs...</div>;
  }

  if (error) {
    return <div className="App">Error: {error}</div>;
  }

  return (
    <div className="App">
      <h1>C2C Job Sourcer</h1>

      <div className="control-panel">
        <div className="control-group">
          <label>Scrape query</label>
          <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Job title or keyword" />
        </div>
        <div className="control-group">
          <label>Location</label>
          <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="remote or city" />
        </div>
        <button onClick={handleScrape}>Scrape Dice</button>
        <button onClick={handleScrapeAll} style={{ marginLeft: '8px' }}>Scrape All Portals</button>
        <button onClick={handleScrapeLinkedIn} style={{ marginLeft: '8px' }}>Scrape LinkedIn</button>
      </div>
      <div className="control-panel">
        <button onClick={handleFilter}>Apply Filters</button>
        <button onClick={handleCandidateSearch} style={{ marginLeft: '8px' }}>Search for Candidate</button>
        <div style={{ display: 'inline-block', marginLeft: '12px' }}>
          <label style={{ marginRight: '8px' }}>
            <input type="checkbox" checked={onlyC2C} onChange={(e) => setOnlyC2C(e.target.checked)} /> Only C2C
          </label>
          <label style={{ marginRight: '8px' }}>
            <input type="checkbox" checked={visaGC} onChange={(e) => setVisaGC(e.target.checked)} /> Visa/GC
          </label>
          <label style={{ marginRight: '8px' }}>
            <input type="checkbox" checked={interviewOffline} onChange={(e) => setInterviewOffline(e.target.checked)} /> Interview Offline
          </label>
          <label style={{ marginRight: '8px' }}>
            <input type="checkbox" checked={interviewOnline} onChange={(e) => setInterviewOnline(e.target.checked)} /> Interview Online
          </label>
          <label>
            <input type="checkbox" checked={linkedinOnly} onChange={(e) => setLinkedinOnly(e.target.checked)} /> LinkedIn Only
          </label>
        </div>
      </div>

      {message && <div className="status-message">{message}</div>}

      <div className="job-list">
        {jobs.map((job) => (
          <div key={job.id} className="job-card">
            <h2>{job.title || 'Untitled'}</h2>
            <p><strong>Company:</strong> {job.company || 'Unknown'}</p>
            <p><strong>Location:</strong> {job.location || 'N/A'} ({job.remoteType || 'N/A'})</p>
            <p><strong>Job Type:</strong> {job.jobType || 'N/A'}</p>
            <p><strong>Source:</strong> {job.source || 'N/A'}</p>
            {job.recruiterEmail && <p><strong>Recruiter:</strong> {job.recruiterEmail}</p>}
            <a href={job.url} target="_blank" rel="noreferrer">View job</a>
          </div>
        ))}
      </div>
    </div>
  );
}

export default App;
