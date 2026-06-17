import Navbar from './Navbar';

export default function Layout({ children }) {
  return (
    <div>
      <Navbar />
      <div className="layout">{children}</div>
    </div>
  );
}
