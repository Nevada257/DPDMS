// hazards/drought/DroughtList.jsx
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { droughtApi } from "./droughtApi";
import { clearToken, getUsername, getRole } from "../../api/apiClient";
import "../../App.css";

export default function DroughtList() {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  const username = getUsername() || "user";
  const role = getRole() || "USER";

  useEffect(() => {
    loadIncidents();
  }, []);

  async function loadIncidents() {
    setLoading(true);
    setError(null);
    try {
      const data = await droughtApi.list();
      setIncidents(data || []);
    } catch (err) {
      if (err.status === 401 || err.status === 403) {
        setError("Access denied — please log in with a drought token.");
      } else {
        setError("Failed to load incidents.");
      }
    } finally {
      setLoading(false);
    }
  }

  function logout() {
    clearToken();
    navigate("/login");
  }

  const pending = incidents.filter((i) => i.status === "PENDING").length;
  const approved = incidents.filter((i) => i.status === "APPROVED").length;
  const rejected = incidents.filter((i) => i.status === "REJECTED").length;
  const corrections = incidents.filter(
    (i) => i.status === "CORRECTION_REQUESTED"
  ).length;

  return (
    <div className="dashboard">
      {/* TOP BAR */}
      <header className="topbar">
        <div>
          <div className="brand">DPDMS</div>
          <div className="brand-subtitle">
            Disaster Monitoring & Management System
          </div>
        </div>
        <div className="user-section">
          <div>
            <strong>{username}</strong>
            <span>{role}</span>
          </div>
          <button className="logout-button" onClick={logout}>
            Logout
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        {/* WELCOME */}
        <div className="welcome">
          <h1>Drought Dashboard</h1>
          <p>Monitor and manage drought incidents across the province.</p>
        </div>

        {/* STATS */}
        <div className="stats-grid">
          <div className="stat-card">
            <span>Total Incidents</span>
            <strong>{incidents.length}</strong>
          </div>
          <div className="stat-card pending-card">
            <span>Pending</span>
            <strong>{pending}</strong>
          </div>
          <div className="stat-card approved-card">
            <span>Approved</span>
            <strong>{approved}</strong>
          </div>
          <div className="stat-card rejected-card">
            <span>Rejected</span>
            <strong>{rejected}</strong>
          </div>
          <div className="stat-card correction-card">
            <span>Corrections</span>
            <strong>{corrections}</strong>
          </div>
        </div>

        {error && <div className="error dashboard-error">{error}</div>}

        {/* INCIDENT TABLE */}
        <section className="incidents-section">
          <div className="section-header">
            <div>
              <h2>Incident Management</h2>
              <p>All drought incidents and their approval status.</p>
            </div>
            {(role === "DROUGHT_RECORDER" || role === "PROVINCIAL_ADMIN") && (
              <Link to="/drought/new" className="primary-button">
                + Add Drought Incident
              </Link>
            )}
          </div>

          {loading && <div className="empty-state">Loading...</div>}

          {!loading && incidents.length === 0 && (
            <div className="empty-state">
              <h3>No drought incidents found</h3>
              <p>Click "Add Drought Incident" to create one.</p>
            </div>
          )}

          {!loading && incidents.length > 0 && (
            <div className="table-container">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Ward</th>
                    <th>District</th>
                    <th>Province</th>
                    <th>Severity</th>
                    <th>Status</th>
                    <th>Rainfall Deficit (mm)</th>
                    <th>Dry Days</th>
                    <th>Crop Failure %</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {incidents.map((inc) => (
                    <tr key={inc.id}>
                      <td>#{inc.id}</td>
                      <td>{inc.ward || "-"}</td>
                      <td>{inc.district || "-"}</td>
                      <td>{inc.province || "-"}</td>
                      <td className="severity">{inc.severity || "-"}</td>
                      <td>
                        <span
                          className={`approval ${
                            inc.status ? inc.status.toLowerCase() : ""
                          }`}
                        >
                          {inc.status || "-"}
                        </span>
                      </td>
                      <td>{inc.rainfallDeficitMm ?? "-"}</td>
                      <td>{inc.consecutiveDryDays ?? "-"}</td>
                      <td>{inc.cropFailurePercentage ?? "-"}</td>
                      <td>
                        <Link
                          to={`/drought/${inc.id}/approve`}
                          className="secondary-button"
                          style={{ padding: "6px 12px", fontSize: 13, textDecoration: "none", display: "inline-block" }}
                        >
                          Manage
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </main>
    </div>
  );
}