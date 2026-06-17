import { useEffect, useState } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer,
  LineChart, Line, PieChart, Pie, Cell, Legend
} from 'recharts';
import { getAnalyticsSummary } from '../api/analytics';

const COLORS = ['#1565c0','#0288d1','#00897b','#558b2f','#f57f17','#e65100','#6a1b9a','#ad1457'];

export default function Analytics() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getAnalyticsSummary()
      .then(res => setData(res.data))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="loading">Loading analytics...</div>;
  if (!data) return <div className="empty">No data available</div>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Analytics</h1>
      </div>
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-value">{data.totalEvents}</div>
          <div className="stat-label">Total Events</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{data.totalUsers}</div>
          <div className="stat-label">Unique Users</div>
        </div>
        <div className="stat-card">
          <div className="stat-value">{data.totalEventTypes}</div>
          <div className="stat-label">Event Types</div>
        </div>
      </div>
      <div className="charts-grid">
        <div className="card">
          <div className="card-title">Event Distribution</div>
          {data.eventDistribution?.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={data.eventDistribution} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                <YAxis tick={{ fontSize: 12 }} />
                <Tooltip />
                <Bar dataKey="count" fill="#1565c0" radius={[4,4,0,0]} />
              </BarChart>
            </ResponsiveContainer>
          ) : <div className="empty">No event data yet</div>}
        </div>
        <div className="card">
          <div className="card-title">Page Distribution</div>
          {data.pageDistribution?.length > 0 ? (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie
                  data={data.pageDistribution}
                  dataKey="count"
                  nameKey="page"
                  cx="50%"
                  cy="50%"
                  outerRadius={100}
                  label={({ page, percent }) => `${page} ${(percent * 100).toFixed(0)}%`}
                >
                  {data.pageDistribution.map((_, i) => (
                    <Cell key={i} fill={COLORS[i % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          ) : <div className="empty">No page data yet</div>}
        </div>
      </div>
      <div className="card">
        <div className="card-title">Daily Event Trend (Last 30 Days)</div>
        {data.dailyTrend?.length > 0 ? (
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={data.dailyTrend} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 11 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Line type="monotone" dataKey="count" stroke="#1565c0" strokeWidth={2} dot={{ r: 4 }} />
            </LineChart>
          </ResponsiveContainer>
        ) : <div className="empty">No trend data yet</div>}
      </div>
    </div>
  );
}
