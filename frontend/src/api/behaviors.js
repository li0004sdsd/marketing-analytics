import api from './axios';

export const getBehaviorEvents = () => api.get('/events');
export const trackEvent = (data) => api.post('/events/track', data);
export const getEventsByUser = (userId) => api.get(`/events/user/${userId}`);
export const deleteEvent = (id) => api.delete(`/events/${id}`);
