import api from './axios';

export const getEventTypes = () => api.get('/event-types');
export const getActiveEventTypes = () => api.get('/event-types/active');
export const createEventType = (data) => api.post('/event-types', data);
export const updateEventType = (id, data) => api.put(`/event-types/${id}`, data);
export const deleteEventType = (id) => api.delete(`/event-types/${id}`);
