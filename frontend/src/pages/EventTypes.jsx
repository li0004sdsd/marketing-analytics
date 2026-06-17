import { useEffect, useState } from 'react';
import { getEventTypes, createEventType, updateEventType, deleteEventType } from '../api/events';

function EventTypeModal({ initial, onSave, onClose }) {
  const [form, setForm] = useState(initial || { name: '', description: '', category: '', active: true });
  const [error, setError] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    try {
      await onSave(form);
      onClose();
    } catch (err) {
      setError(err.response?.data?.message || 'Save failed');
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={e => e.stopPropagation()}>
        <div className="modal-title">{initial ? 'Edit Event Type' : 'New Event Type'}</div>
        {error && <div className="alert alert-error">{error}</div>}
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Name</label>
            <input value={form.name} onChange={e => setForm({...form, name: e.target.value})} required />
          </div>
          <div className="form-group">
            <label>Category</label>
            <input value={form.category} onChange={e => setForm({...form, category: e.target.value})} required />
          </div>
          <div className="form-group">
            <label>Description</label>
            <input value={form.description || ''} onChange={e => setForm({...form, description: e.target.value})} />
          </div>
          <div className="form-group">
            <label>Status</label>
            <select value={form.active ? 'true' : 'false'} onChange={e => setForm({...form, active: e.target.value === 'true'})}>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </div>
          <div className="modal-footer">
            <button type="button" className="btn btn-secondary" onClick={onClose}>Cancel</button>
            <button type="submit" className="btn btn-primary">Save</button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default function EventTypes() {
  const [eventTypes, setEventTypes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null);

  const load = () => getEventTypes().then(res => setEventTypes(res.data)).finally(() => setLoading(false));

  useEffect(() => { load(); }, []);

  const handleSave = async (form) => {
    if (editing) {
      await updateEventType(editing.id, form);
    } else {
      await createEventType(form);
    }
    load();
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this event type?')) return;
    await deleteEventType(id);
    load();
  };

  if (loading) return <div className="loading">Loading...</div>;

  return (
    <div>
      <div className="page-header">
        <h1 className="page-title">Event Types</h1>
        <button className="btn btn-primary" onClick={() => { setEditing(null); setShowModal(true); }}>
          + New Event Type
        </button>
      </div>
      <div className="card">
        {eventTypes.length === 0 ? (
          <div className="empty">No event types defined yet</div>
        ) : (
          <div className="table-container">
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Description</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {eventTypes.map(et => (
                  <tr key={et.id}>
                    <td><strong>{et.name}</strong></td>
                    <td>{et.category}</td>
                    <td>{et.description || '-'}</td>
                    <td>
                      <span className={`badge ${et.active ? 'badge-success' : 'badge-danger'}`}>
                        {et.active ? 'Active' : 'Inactive'}
                      </span>
                    </td>
                    <td>{new Date(et.createdAt).toLocaleDateString()}</td>
                    <td style={{display:'flex', gap:6}}>
                      <button className="btn btn-secondary btn-sm" onClick={() => { setEditing(et); setShowModal(true); }}>Edit</button>
                      <button className="btn btn-danger btn-sm" onClick={() => handleDelete(et.id)}>Delete</button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
      {showModal && (
        <EventTypeModal
          initial={editing}
          onSave={handleSave}
          onClose={() => setShowModal(false)}
        />
      )}
    </div>
  );
}
