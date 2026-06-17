import { useEffect, useState } from 'react';
import { getAnalyticsSummary } from '../api/analytics';
import { getBehaviorEvents } from '../api/behaviors';

export default function Dashboard() {
  const [summary, setSummary] = useState(null);
  const [recentEvents, setRecentEvents] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getAnalyticsSummary(), getBehaviorEvents()])
      .then(([sumRes, eventsRes]) => {
        setSummary(sumRes.data);
        setRecentEvents(eventsRes.data.slice(0, 5));
      })
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading">Loading dashboard...</div>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Dashboard</h1>
      </div>
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{summary?.totalEvents ?? 0}</div>
          <div className="stat-label">Total Events</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{summary?.totalUsers ?? 0}</div>
          <div className="stat-label">Unique Users</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{summary?.totalEventTypes ?? 0}</div>
          <div className="stat-label">Event Types</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{summary?.eventDistribution?.length ?? 0}</div>
          <div className="stat-label">Event Categories</div>
        </div>
      </div>
      <div className="card">
        <div className="card-title">Recent Events</div>
        {recentEvents.length === 0 ? (
          <div className="empty">No events tracked yet</div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Event</th>
                  <th>Page</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {recentEvents.map(ev => (
                  <tr key={ev.id}>
                    <td>{ev.userId}</td>
                    <td>{ev.eventName}</td>
                    <td>{ev.page || '-'}</td>
                    <td>{new Date(ev.timestamp).toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
