import { useState, useEffect } from "react";

const API_URL = "http://localhost:8080";

const HAZARDS = [
  { key: "flood", label: "Flood", path: "/api/floods", color: "#2563eb" },
  { key: "drought", label: "Drought", path: "/api/drought/incidents", color: "#d97706" },
  { key: "fire", label: "Fire", path: "/api/fire-incidents", color: "#dc2626" },
  { key: "zoonotic", label: "Zoonotic Disease", path: "/api/zoonotic-incidents", color: "#7c3aed" },
  { key: "mining", label: "Mining Accident", path: "/api/mining-accidents", color: "#4b5563" }
];

function NationalDashboard({ credentials, onLogout }) {
  const [counts, setCounts] = useState({});
  const [allIncidents, setAllIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    loadAll();
  }, []);

  const loadAll = async () => {
    setLoading(true);
    setError("");

    const newCounts = {};
    const combined = [];

    for (const hazard of HAZARDS) {
      try {
        const response = await fetch(`${API_URL}${hazard.path}`, {
          headers: {
            Authorization: `Bearer ${credentials.auth}`
          }
        });

        if (!response.ok) {
          newCounts[hazard.key] = 0;
          continue;
        }

        const data = await response.json();
        const list = Array.isArray(data) ? data : [];

        newCounts[hazard.key] = list.length;

        list.forEach((incident) => {
          combined.push({
            hazard: hazard.label,
            color: hazard.color,
            ward: incident.ward,
            district: incident.district,
            province: incident.province,
            severity: incident.severity,
            reporter: incident.reporter
          });
        });
      } catch (err) {
        console.error(`Failed to load ${hazard.label}:`, err);
        newCounts[hazard.key] = 0;
      }
    }

    setCounts(newCounts);
    setAllIncidents(combined);
    setLoading(false);
  };

  const totalApproved = Object.values(counts).reduce(
    (sum, n) => sum + n,
    0
  );

  return (
    <div style={{ minHeight: "100vh", background: "#f3f4f6" }}>
      <div
        style={{
          background: "#0f766e",
          color: "white",
          padding: "20px 32px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: "24px" }}>DPDMS</h1>
          <p style={{ margin: 0, opacity: 0.85 }}>
            Disaster Monitoring & Management System
          </p>
        </div>
        <div style={{ textAlign: "right" }}>
          <div style={{ fontWeight: "bold" }}>{credentials.username}</div>
          <div style={{ opacity: 0.85, fontSize: "14px" }}>
            {credentials.role} — ALL HAZARDS
          </div>
          <button
            onClick={onLogout}
            style={{
              marginTop: "8px",
              background: "white",
              color: "#0f766e",
              border: "none",
              borderRadius: "6px",
              padding: "6px 16px",
              cursor: "pointer",
              fontWeight: "bold"
            }}
          >
            Logout
          </button>
        </div>
      </div>

      <div style={{ padding: "32px" }}>
        <h2 style={{ textAlign: "center" }}>National Dashboard</h2>
        <p style={{ textAlign: "center", color: "#6b7280" }}>
          Approved incidents across all five hazards.
        </p>

        {loading && (
          <p style={{ textAlign: "center" }}>Loading hazard data...</p>
        )}

        {error && (
          <p style={{ textAlign: "center", color: "#dc2626" }}>{error}</p>
        )}

        {!loading && (
          <>
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                gap: "16px",
                maxWidth: "1100px",
                margin: "24px auto"
              }}
            >
              <div
                style={{
                  background: "white",
                  borderRadius: "10px",
                  padding: "20px",
                  textAlign: "center",
                  borderLeft: "4px solid #0f766e",
                  boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
                }}
              >
                <div style={{ color: "#6b7280" }}>Total Approved</div>
                <div style={{ fontSize: "32px", fontWeight: "bold" }}>
                  {totalApproved}
                </div>
              </div>

              {HAZARDS.map((hazard) => (
                <div
                  key={hazard.key}
                  style={{
                    background: "white",
                    borderRadius: "10px",
                    padding: "20px",
                    textAlign: "center",
                    borderLeft: `4px solid ${hazard.color}`,
                    boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
                  }}
                >
                  <div style={{ color: "#6b7280" }}>{hazard.label}</div>
                  <div style={{ fontSize: "32px", fontWeight: "bold" }}>
                    {counts[hazard.key] ?? 0}
                  </div>
                </div>
              ))}
            </div>

            <div
              style={{
                background: "white",
                borderRadius: "10px",
                padding: "20px",
                maxWidth: "1100px",
                margin: "24px auto",
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
              }}
            >
              <h3>Approved Incidents — All Hazards</h3>

              {allIncidents.length === 0 && (
                <p style={{ color: "#6b7280" }}>
                  No approved incidents yet across any hazard.
                </p>
              )}

              {allIncidents.length > 0 && (
                <table style={{ width: "100%", borderCollapse: "collapse" }}>
                  <thead>
                    <tr style={{ borderBottom: "2px solid #e5e7eb" }}>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        Hazard
                      </th>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        Ward
                      </th>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        District
                      </th>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        Province
                      </th>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        Severity
                      </th>
                      <th style={{ textAlign: "left", padding: "8px" }}>
                        Reporter
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {allIncidents.map((incident, index) => (
                      <tr key={index} style={{ borderBottom: "1px solid #f3f4f6" }}>
                        <td style={{ padding: "8px", color: incident.color, fontWeight: "bold" }}>
                          {incident.hazard}
                        </td>
                        <td style={{ padding: "8px" }}>{incident.ward || "-"}</td>
                        <td style={{ padding: "8px" }}>{incident.district || "-"}</td>
                        <td style={{ padding: "8px" }}>{incident.province || "-"}</td>
                        <td style={{ padding: "8px" }}>{incident.severity || "-"}</td>
                        <td style={{ padding: "8px" }}>{incident.reporter || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </div>
    </div>
  );
}

export default NationalDashboard;