import { NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function Navbar() {
  const { user, logoutUser } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logoutUser();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <a href="/" className="navbar-brand">Analytics Platform</a>
      <div className="navbar-links">
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/event-types">Event Types</NavLink>
        <NavLink to="/behaviors">Behaviors</NavLink>
        <NavLink to="/analytics">Analytics</NavLink>
        <span style={{ color: '#aaa', fontSize: 13, marginLeft: 8 }}>{user?.username}</span>
        <button className="btn btn-secondary btn-sm" onClick={handleLogout}>Logout</button>
      </div>
    </nav>
  );
}
