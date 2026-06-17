import api from './axios';

export const login = (username, password) =>
  api.post('/auth/login', { username, password });

export const register = (username, email, password) =>
  api.post('/auth/register', { username, email, password });
