import { useEffect, useState } from 'react';
import { getBehaviorEvents, trackEvent, deleteEvent } from '../api/behaviors';

function TrackModal({ onSave, onClose }) {
  const [form, setForm] = useState({ userId: '', eventName: '', page: '', sessionId: '', properties: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await onSave(form);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Track failed');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-title">Track Event</div>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>User ID</label>
            <input value={form.userId} onChange={e => setForm({...form, userId: e.target.value})} required />
          </div>
          <div className="form-group">
            <label>Event Name</label>
            <input value={form.eventName} onChange={e => setForm({...form, eventName: e.target.value})} required />
          </div>
          <div className="form-group">
            <label>Page</label>
            <input value={form.page} onChange={e => setForm({...form, page: e.target.value})} />
          </div>
          <div className="form-group">
            <label>Session ID</label>
            <input value={form.sessionId} onChange={e => setForm({...form, sessionId: e.target.value})} />
          </div>
          <div className="form-group">
            <label>Properties (JSON)</label>
            <input value={form.properties} onChange={e => setForm({...form, properties: e.target.value})} placeholder='{"key":"value"}' />
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">Track</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function BehaviorEvents() {
  const [events, setEvents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [filter, setFilter] = useState('');

  const load = () => getBehaviorEvents().then(res => setEvents(res.data)).finally(() => setLoading(false));

  useEffect(() => { load(); }, []);

  const handleTrack = async (form) => {
    await trackEvent(form);
    load();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this event?')) return;
    await deleteEvent(id);
    load();
  };

  const filtered = filter
    ? events.filter(e => e.userId.includes(filter) || e.eventName.includes(filter))
    : events;

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Behavior Events</h1>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>+ Track Event</button>
      </div>
      <div className="card">
        <div style={{ marginBottom: 16 }}>
          <input
            style={{ padding: '8px 12px', border: '1px solid #ddd', borderRadius: 4, width: 280, fontSize: 14 }}
            placeholder="Filter by user ID or event name..."
            value={filter}
            onChange={e => setFilter(e.target.value)}
          />
        </div>
        {filtered.length === 0 ? (
          <div className="empty">No events found</div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Event</th>
                  <th>Page</th>
                  <th>Session</th>
                  <th>Time</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(ev => (
                  <tr key={ev.id}>
                    <td>{ev.userId}</td>
                    <td>{ev.eventName}</td>
                    <td>{ev.page || '-'}</td>
                    <td>{ev.sessionId || '-'}</td>
                    <td>{new Date(ev.timestamp).toLocaleString()}</td>
                    <td>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(ev.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {showModal && <TrackModal onSave={handleTrack} onClose={() => setShowModal(false)} />}
    </div>
  );
}
