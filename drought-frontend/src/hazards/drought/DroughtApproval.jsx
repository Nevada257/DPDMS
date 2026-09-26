// hazards/drought/DroughtApproval.jsx
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { droughtApi } from "./droughtApi";
import { getUsername, getRole } from "../../api/apiClient";
import "../../App.css";

export default function DroughtApproval() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [incident, setIncident] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");

  const username = getUsername() || "supervisor1";
  const role = getRole();

  useEffect(() => {
    load();
  }, [id]);

  async function load() {
    try {
      const inc = await droughtApi.getOne(id);
      setIncident(inc);
    } catch {
      setError("Could not load incident.");
    } finally {
      setLoading(false);
    }
  }

  async function doAction(action) {
    setBusy(true);
    setError(null);
    setMessage("");
    try {
      if (action === "approve") {
        await droughtApi.approve(id, username);
      } else if (action === "reject") {
        if (!reason.trim()) {
          setError("Please provide a reason before rejecting.");
          setBusy(false);
          return;
        }
        await droughtApi.reject(id, reason, username);
      } else if (action === "correction") {
        if (!reason.trim()) {
          setError("Please provide notes before requesting correction.");
          setBusy(false);
          return;
        }
        await droughtApi.requestCorrection(id, reason, username);
      }
      navigate("/drought");
    } catch (err) {
      if (err.status === 403) {
        setError("You do not have permission to perform this action.");
      } else {
        setError("Action failed.");
      }
    } finally {
      setBusy(false);
    }
  }

  if (loading) return <div className="empty-state">Loading...</div>;
  if (error && !incident) return <div className="error dashboard-error">{error}</div>;
  if (!incident) return <div className="empty-state">Not found.</div>;

  const isPending = incident.status === "PENDING";

  return (
    <div className="dashboard">
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
          <button className="logout-button" onClick={() => navigate("/drought")}>
            ← Back
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="welcome">
          <h1>Drought Incident #{incident.id}</h1>
          <p>Review the incident and take the appropriate action.</p>
        </div>

        {message && <div className="success-message">{message}</div>}
        {error && <div className="error dashboard-error">{error}</div>}

        {/* INCIDENT DETAILS */}
        <section className="incidents-section">
          <div className="section-header">
            <div>
              <h2>Incident Details</h2>
              <p>All submitted information about this drought incident.</p>
            </div>
            <span
              className={`approval ${
                incident.status ? incident.status.toLowerCase() : ""
              }`}
            >
              {incident.status}
            </span>
          </div>

          <div className="table-container">
            <table>
              <tbody>
                <Row label="Ward" value={incident.ward} />
                <Row label="District" value={incident.district} />
                <Row label="Province" value={incident.province} />
                <Row label="Reporter" value={incident.reporter} />
                <Row label="Severity" value={incident.severity} />
                <Row label="Date/Time" value={incident.dateTimeOfOccurrence} />
                <Row
                  label="GPS Coordinates"
                  value={`${incident.latitude}, ${incident.longitude}`}
                />
                <Row
                  label="Rainfall Deficit (mm)"
                  value={incident.rainfallDeficitMm}
                />
                <Row
                  label="Consecutive Dry Days"
                  value={incident.consecutiveDryDays}
                />
                <Row
                  label="Crop Failure %"
                  value={incident.cropFailurePercentage}
                />
                <Row
                  label="People Facing Water Shortages"
                  value={incident.peopleFacingWaterShortages}
                />
                <Row
                  label="Livestock Mortality"
                  value={incident.livestockMortalityCount}
                />
                {incident.rejectionReason && (
                  <Row
                    label="Rejection / Correction Reason"
                    value={incident.rejectionReason}
                  />
                )}
              </tbody>
            </table>
          </div>
        </section>

        {/* SUPERVISOR ACTIONS */}
        {role === "DROUGHT_SUPERVISOR" && isPending && (
          <section className="incident-form-section">
            <div className="section-header">
              <div>
                <h2>Supervisor Actions</h2>
                <p>Approve, reject, or request corrections for this incident.</p>
              </div>
            </div>

            <div className="incident-form">
              <div className="form-grid">
                <div style={{ gridColumn: "1 / -1" }}>
                  <label>
                    Reason / Notes (required for reject or correction)
                  </label>
                  <textarea
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    rows={3}
                    style={{
                      width: "100%",
                      padding: 12,
                      border: "1px solid #cbd5e1",
                      borderRadius: 7,
                      fontSize: 14,
                      fontFamily: "inherit",
                    }}
                  />
                </div>
              </div>

              <div className="form-actions">
                <button
                  className="secondary-button"
                  onClick={() => doAction("correction")}
                  disabled={busy}
                >
                  Request Correction
                </button>
                <button
                  className="secondary-button"
                  onClick={() => doAction("reject")}
                  disabled={busy}
                  style={{ color: "#dc2626", borderColor: "#dc2626" }}
                >
                  Reject
                </button>
                <button
                  className="primary-button"
                  onClick={() => doAction("approve")}
                  disabled={busy}
                  style={{ background: "#16a34a" }}
                >
                  Approve
                </button>
              </div>
            </div>
          </section>
        )}

        {role !== "DROUGHT_SUPERVISOR" && isPending && (
          <div className="incidents-section">
            <div className="empty-state">
              <h3>Approval Restricted</h3>
              <p>
                Only a DROUGHT_SUPERVISOR can approve, reject, or request
                corrections.
              </p>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

function Row({ label, value }) {
  return (
    <tr>
      <td
        style={{
          width: 300,
          fontWeight: 600,
          color: "#475569",
          background: "#f8fafc",
        }}
      >
        {label}
      </td>
      <td>{String(value ?? "—")}</td>
    </tr>
  );
}