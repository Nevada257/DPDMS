import { useState, useEffect, useCallback } from "react";
import { MapContainer, TileLayer, CircleMarker, Popup } from "react-leaflet";
import "leaflet/dist/leaflet.css";
import DroughtPanel from "./DroughtPanel";
import FirePanel from "./FirePanel";
import MiningPanel from "./MiningPanel";
import ZoonoticPanel from "./ZoonoticPanel";

const API_URL = "http://localhost:8080";

// Rushinga District, Mashonaland Central - default map centre
const MAP_CENTRE = [-16.65, 32.2];

const HAZARDS = [
  { key: "FLOOD", panel: null, label: "Flood", color: "#2563eb" },
  { key: "DROUGHT", panel: "drought", label: "Drought", color: "#d97706" },
  { key: "FIRE", panel: "fire", label: "Fire", color: "#dc2626" },
  { key: "ZOONOTIC", panel: "zoonotic", label: "Zoonotic Disease", color: "#7c3aed" },
  { key: "MINING", panel: "mining", label: "Mining Accident", color: "#4b5563" }
];
const HAZARD_BY_KEY = Object.fromEntries(HAZARDS.map((h) => [h.key, h]));

const SEVERITY_COLORS = { LOW: "#16a34a", MEDIUM: "#ca8a04", HIGH: "#ea580c", CRITICAL: "#b91c1c" };

const EMPTY_FILTERS = { hazard: "", district: "", ward: "", severity: "", from: "", to: "" };

const card = {
  background: "white",
  borderRadius: "10px",
  padding: "20px",
  boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
};

const input = {
  padding: "8px",
  border: "1px solid #d1d5db",
  borderRadius: "6px",
  fontSize: "14px"
};

function queryString(filters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([k, v]) => v && params.append(k, v));
  const s = params.toString();
  return s ? `?${s}` : "";
}

function label(key) {
  return key.replaceAll("_", " ").toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

/** Horizontal bar list, e.g. incidents by severity. */
function BarList({ title, data, colors }) {
  const entries = Object.entries(data || {});
  const max = Math.max(1, ...entries.map(([, v]) => v));
  return (
    <div style={card}>
      <h3 style={{ marginTop: 0 }}>{title}</h3>
      {entries.length === 0 && <p style={{ color: "#6b7280" }}>No data</p>}
      {entries.map(([key, value]) => (
        <div key={key} style={{ marginBottom: "10px" }}>
          <div style={{ display: "flex", justifyContent: "space-between", fontSize: "14px" }}>
            <span>{label(key)}</span>
            <strong>{value}</strong>
          </div>
          <div style={{ background: "#f3f4f6", borderRadius: "4px", height: "10px" }}>
            <div
              style={{
                width: `${(value / max) * 100}%`,
                background: (colors && colors[key]) || "#0f766e",
                height: "10px",
                borderRadius: "4px"
              }}
            />
          </div>
        </div>
      ))}
    </div>
  );
}

/** Stacked monthly bar chart of approved incidents per hazard (plain SVG, no chart library). */
function TrendChart({ trend }) {
  if (!trend || !trend.months) return null;
  const months = trend.months;
  const series = trend.series || {};
  const totals = months.map((_, i) =>
    Object.values(series).reduce((sum, counts) => sum + (counts[i] || 0), 0)
  );
  const max = Math.max(1, ...totals);
  const width = 720;
  const height = 220;
  const pad = { left: 32, bottom: 36, top: 10, right: 10 };
  const plotH = height - pad.top - pad.bottom;
  const slot = (width - pad.left - pad.right) / months.length;
  const barW = Math.max(6, slot * 0.6);

  return (
    <div style={card}>
      <h3 style={{ marginTop: 0 }}>Trend — approved incidents per month</h3>
      <svg viewBox={`0 0 ${width} ${height}`} style={{ width: "100%", height: "auto" }} role="img"
           aria-label="Monthly incidents by hazard">
        {[0, 0.5, 1].map((f) => (
          <g key={f}>
            <line x1={pad.left} x2={width - pad.right}
                  y1={pad.top + plotH * (1 - f)} y2={pad.top + plotH * (1 - f)}
                  stroke="#e5e7eb" />
            <text x={pad.left - 6} y={pad.top + plotH * (1 - f) + 4} fontSize="11"
                  textAnchor="end" fill="#6b7280">{Math.round(max * f)}</text>
          </g>
        ))}
        {months.map((m, i) => {
          let y = pad.top + plotH;
          const x = pad.left + i * slot + (slot - barW) / 2;
          return (
            <g key={m}>
              {Object.entries(series).map(([hazard, counts]) => {
                const v = counts[i] || 0;
                if (!v) return null;
                const h = (v / max) * plotH;
                y -= h;
                return (
                  <rect key={hazard} x={x} y={y} width={barW} height={h}
                        fill={HAZARD_BY_KEY[hazard]?.color || "#999"}>
                    <title>{`${HAZARD_BY_KEY[hazard]?.label}: ${v} in ${m}`}</title>
                  </rect>
                );
              })}
              <text x={x + barW / 2} y={height - pad.bottom + 16} fontSize="10"
                    textAnchor="middle" fill="#6b7280">{m.slice(2)}</text>
            </g>
          );
        })}
      </svg>
      <div style={{ display: "flex", gap: "16px", flexWrap: "wrap", fontSize: "13px" }}>
        {Object.keys(series).map((h) => (
          <span key={h}>
            <span style={{
              display: "inline-block", width: "10px", height: "10px", marginRight: "6px",
              background: HAZARD_BY_KEY[h]?.color
            }} />
            {HAZARD_BY_KEY[h]?.label}
          </span>
        ))}
      </div>
    </div>
  );
}

function NationalDashboard({ credentials, onLogout, onBack }) {
  const [overview, setOverview] = useState(null);
  const [filters, setFilters] = useState(EMPTY_FILTERS);
  const [applied, setApplied] = useState(EMPTY_FILTERS);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [activePanel, setActivePanel] = useState(null);
  const [alerts, setAlerts] = useState([]);
  const [reportBusy, setReportBusy] = useState("");

  const authHeader = { Authorization: `Bearer ${credentials.auth}` };
  const allHazards = credentials.hazardScope === "ALL";
  const canSeeAlertLog = credentials.role !== "RECORDER";

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const response = await fetch(`${API_URL}/api/dashboard/overview${queryString(applied)}`, {
        headers: { Authorization: `Bearer ${credentials.auth}` }
      });
      if (!response.ok) {
        setError(`Dashboard service returned ${response.status}.`);
        setOverview(null);
      } else {
        setOverview(await response.json());
      }
    } catch (err) {
      console.error(err);
      setError("Could not reach the dashboard service through the gateway.");
    }

    if (canSeeAlertLog) {
      try {
        const r = await fetch(`${API_URL}/api/alerts/logs`, {
          headers: { Authorization: `Bearer ${credentials.auth}` }
        });
        setAlerts(r.ok ? (await r.json()).slice(0, 15) : []);
      } catch {
        setAlerts([]); // alert-service down: the dashboard still works
      }
    }
    setLoading(false);
  }, [applied, credentials.auth, canSeeAlertLog]);

  useEffect(() => {
    load();
  }, [load]);

  // Reports: report-service builds the file from approved incidents matching the
  // current filters (limited to this user's hazard scope) and returns it for download.
  const downloadReport = async (format) => {
    setReportBusy(format);
    setError("");
    try {
      const qs = queryString({ ...applied, format });
      const response = await fetch(`${API_URL}/api/reports/incidents${qs}`, { headers: authHeader });
      if (!response.ok) {
        let reason = `report-service returned ${response.status}`;
        try { reason = (await response.json()).error || reason; } catch { /* not JSON */ }
        throw new Error(reason);
      }

      // Use the file name chosen by the server (Content-Disposition), if exposed
      const disposition = response.headers.get("Content-Disposition") || "";
      const match = disposition.match(/filename="?([^"]+)"?/);
      const ext = { PDF: "pdf", DOCX: "docx", XLSX: "xlsx", CSV: "csv" }[format];
      const filename = match ? match[1] : `DPDMS_Report.${ext}`;

      const blob = await response.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = filename;
      a.click();
      URL.revokeObjectURL(url);
    } catch (err) {
      console.error(err);
      setError(`Report could not be generated: ${err.message}`);
    }
    setReportBusy("");
  };

  // Drill-in to a hazard's full panel (read-only for national / admin users)
  const back = () => setActivePanel(null);
  if (activePanel === "drought") return <DroughtPanel credentials={credentials} onLogout={onLogout} onBack={back} />;
  if (activePanel === "fire") return <FirePanel credentials={credentials} onLogout={onLogout} onBack={back} />;
  if (activePanel === "mining") return <MiningPanel credentials={credentials} onLogout={onLogout} onBack={back} />;
  if (activePanel === "zoonotic") return <ZoonoticPanel credentials={credentials} onLogout={onLogout} onBack={back} />;

  const visibleHazards = HAZARDS.filter((h) => (overview?.hazards || []).includes(h.key));
  const unavailable = Object.entries(overview?.unavailable || {});

  return (
    <div style={{ minHeight: "100vh", background: "#f3f4f6" }}>
      <div style={{
        background: "#0f766e", color: "white", padding: "20px 32px",
        display: "flex", justifyContent: "space-between", alignItems: "center"
      }}>
        <div>
          <h1 style={{ margin: 0, fontSize: "24px" }}>DPDMS</h1>
          <p style={{ margin: 0, opacity: 0.85 }}>Disaster Monitoring & Management System</p>
        </div>
        <div style={{ textAlign: "right" }}>
          <div style={{ fontWeight: "bold" }}>{credentials.username}</div>
          <div style={{ opacity: 0.85, fontSize: "14px" }}>
            {credentials.role} — {allHazards ? "ALL HAZARDS" : credentials.hazardScope}
          </div>
          <div style={{ marginTop: "8px", display: "flex", gap: "8px", justifyContent: "flex-end" }}>
            {onBack && (
              <button onClick={onBack} style={headerButton}>← Back to incidents</button>
            )}
            <button onClick={onLogout} style={headerButton}>Logout</button>
          </div>
        </div>
      </div>

      <div style={{ padding: "24px 32px", maxWidth: "1200px", margin: "0 auto" }}>
        <h2 style={{ textAlign: "center", marginBottom: "4px" }}>
          {allHazards ? "National Dashboard" : `${credentials.hazardScope} Dashboard`}
        </h2>
        <p style={{ textAlign: "center", color: "#6b7280", marginTop: 0 }}>
          Approved incidents only. Pending records never appear here, on the map or in reports.
        </p>

        {/* Filters */}
        <div style={{ ...card, display: "flex", gap: "10px", flexWrap: "wrap", alignItems: "flex-end" }}>
          {allHazards && (
            <label style={filterLabel}>Hazard
              <select style={input} value={filters.hazard}
                      onChange={(e) => setFilters({ ...filters, hazard: e.target.value })}>
                <option value="">All</option>
                {HAZARDS.map((h) => <option key={h.key} value={h.key}>{h.label}</option>)}
              </select>
            </label>
          )}
          <label style={filterLabel}>District
            <input style={input} value={filters.district} placeholder="e.g. Rushinga"
                   onChange={(e) => setFilters({ ...filters, district: e.target.value })} />
          </label>
          <label style={filterLabel}>Ward
            <input style={input} value={filters.ward} placeholder="e.g. Ward 1"
                   onChange={(e) => setFilters({ ...filters, ward: e.target.value })} />
          </label>
          <label style={filterLabel}>Severity
            <select style={input} value={filters.severity}
                    onChange={(e) => setFilters({ ...filters, severity: e.target.value })}>
              <option value="">All</option>
              {Object.keys(SEVERITY_COLORS).map((s) => <option key={s}>{s}</option>)}
            </select>
          </label>
          <label style={filterLabel}>From
            <input type="date" style={input} value={filters.from}
                   onChange={(e) => setFilters({ ...filters, from: e.target.value })} />
          </label>
          <label style={filterLabel}>To
            <input type="date" style={input} value={filters.to}
                   onChange={(e) => setFilters({ ...filters, to: e.target.value })} />
          </label>
          <button style={primaryButton} onClick={() => setApplied(filters)}>Apply</button>
          <button style={secondaryButton} onClick={() => { setFilters(EMPTY_FILTERS); setApplied(EMPTY_FILTERS); }}>
            Clear
          </button>
          <div style={{ marginLeft: "auto", display: "flex", gap: "6px", alignItems: "center" }}>
            <span style={{ fontSize: "13px", color: "#6b7280" }}>Report:</span>
            {["PDF", "DOCX", "XLSX", "CSV"].map((f) => (
              <button key={f} style={secondaryButton} disabled={!!reportBusy} onClick={() => downloadReport(f)}>
                {reportBusy === f ? "…" : f}
              </button>
            ))}
          </div>
        </div>

        {error && <p style={{ color: "#dc2626", textAlign: "center" }}>{error}</p>}
        {unavailable.length > 0 && (
          <div style={{ ...card, marginTop: "16px", background: "#fffbeb", borderLeft: "4px solid #d97706" }}>
            <strong>Some data is temporarily unavailable:</strong>
            <ul style={{ margin: "6px 0 0" }}>
              {unavailable.map(([h, why]) => <li key={h}>{HAZARD_BY_KEY[h]?.label || h}: {why}</li>)}
            </ul>
          </div>
        )}
        {loading && <p style={{ textAlign: "center" }}>Loading dashboard…</p>}

        {overview && (
          <>
            {/* KPI cards */}
            <div style={{
              display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(170px, 1fr))",
              gap: "16px", margin: "20px 0"
            }}>
              <div style={{ ...card, textAlign: "center", borderLeft: "4px solid #0f766e" }}>
                <div style={{ color: "#6b7280" }}>Total approved</div>
                <div style={{ fontSize: "32px", fontWeight: "bold" }}>{overview.total}</div>
              </div>
              {visibleHazards.map((h) => (
                <div key={h.key}
                     onClick={() => allHazards && h.panel && setActivePanel(h.panel)}
                     style={{
                       ...card, textAlign: "center", borderLeft: `4px solid ${h.color}`,
                       cursor: allHazards && h.panel ? "pointer" : "default"
                     }}>
                  <div style={{ color: "#6b7280" }}>{h.label}</div>
                  <div style={{ fontSize: "32px", fontWeight: "bold" }}>{overview.byHazard?.[h.key] ?? 0}</div>
                  {allHazards && h.panel && (
                    <div style={{ fontSize: "11px", color: h.color, marginTop: "4px" }}>Click to view →</div>
                  )}
                </div>
              ))}
            </div>

            {/* Map */}
            <div style={{ ...card, marginBottom: "20px" }}>
              <h3 style={{ marginTop: 0 }}>Incident map</h3>
              <div style={{ height: "440px", borderRadius: "8px", overflow: "hidden" }}>
                <MapContainer center={MAP_CENTRE} zoom={9} style={{ height: "100%", width: "100%" }}>
                  <TileLayer
                    attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                  />
                  {(overview.mapPoints || []).map((i) => (
                    <CircleMarker
                      key={`${i.hazard}-${i.id}`}
                      center={[i.latitude, i.longitude]}
                      radius={i.severity === "CRITICAL" ? 11 : i.severity === "HIGH" ? 9 : 7}
                      pathOptions={{
                        color: HAZARD_BY_KEY[i.hazard]?.color,
                        fillColor: HAZARD_BY_KEY[i.hazard]?.color,
                        fillOpacity: 0.7
                      }}
                    >
                      <Popup>
                        <div style={{ minWidth: "200px" }}>
                          <strong style={{ color: HAZARD_BY_KEY[i.hazard]?.color }}>
                            {HAZARD_BY_KEY[i.hazard]?.label} #{i.id}
                          </strong>
                          <div>{i.ward}, {i.district}</div>
                          <div>{(i.occurredAt || "").replace("T", " ").slice(0, 16)}</div>
                          <div>Severity: <strong>{i.severity}</strong> · {label(i.operationalStatus)}</div>
                          <div style={{ marginTop: "4px" }}>{i.headline}</div>
                          <table style={{ marginTop: "6px", fontSize: "12px" }}>
                            <tbody>
                              {Object.entries(i.indicators || {}).map(([k, v]) => (
                                <tr key={k}>
                                  <td style={{ paddingRight: "8px", color: "#6b7280" }}>{k}</td>
                                  <td>{v === null ? "-" : String(v)}</td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                          <div style={{ fontSize: "11px", color: "#6b7280", marginTop: "4px" }}>
                            GPS {i.latitude}, {i.longitude}
                          </div>
                        </div>
                      </Popup>
                    </CircleMarker>
                  ))}
                </MapContainer>
              </div>
              <div style={{ display: "flex", gap: "16px", flexWrap: "wrap", fontSize: "13px", marginTop: "8px" }}>
                {visibleHazards.map((h) => (
                  <span key={h.key}>
                    <span style={{
                      display: "inline-block", width: "10px", height: "10px", borderRadius: "50%",
                      marginRight: "6px", background: h.color
                    }} />
                    {h.label}
                  </span>
                ))}
                <span style={{ color: "#6b7280" }}>Larger markers = higher severity</span>
              </div>
            </div>

            {/* Breakdown + trend */}
            <div style={{
              display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(280px, 1fr))",
              gap: "16px", marginBottom: "20px"
            }}>
              <BarList title="By severity" data={overview.bySeverity} colors={SEVERITY_COLORS} />
              <BarList title="By status" data={overview.byStatus} />
            </div>
            <TrendChart trend={overview.trend} />

            {/* Recent incidents */}
            <div style={{ ...card, marginTop: "20px" }}>
              <h3 style={{ marginTop: 0 }}>Recent approved incidents</h3>
              {overview.recent.length === 0 && <p style={{ color: "#6b7280" }}>No approved incidents yet.</p>}
              {overview.recent.length > 0 && (
                <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "14px" }}>
                  <thead>
                    <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                      <th style={cell}>Hazard</th>
                      <th style={cell}>Date</th>
                      <th style={cell}>Ward</th>
                      <th style={cell}>District</th>
                      <th style={cell}>Severity</th>
                      <th style={cell}>Status</th>
                      <th style={cell}>Summary</th>
                    </tr>
                  </thead>
                  <tbody>
                    {overview.recent.map((i) => (
                      <tr key={`${i.hazard}-${i.id}`} style={{ borderBottom: "1px solid #f3f4f6" }}>
                        <td style={{ ...cell, color: HAZARD_BY_KEY[i.hazard]?.color, fontWeight: "bold" }}>
                          {HAZARD_BY_KEY[i.hazard]?.label}
                        </td>
                        <td style={cell}>{(i.occurredAt || "").slice(0, 10)}</td>
                        <td style={cell}>{i.ward || "-"}</td>
                        <td style={cell}>{i.district || "-"}</td>
                        <td style={{ ...cell, color: SEVERITY_COLORS[i.severity], fontWeight: "bold" }}>{i.severity}</td>
                        <td style={cell}>{label(i.operationalStatus)}</td>
                        <td style={cell}>{i.headline}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            {/* Alert log */}
            {canSeeAlertLog && (
              <div style={{ ...card, marginTop: "20px" }}>
                <h3 style={{ marginTop: 0 }}>Recent alerts sent</h3>
                {alerts.length === 0 && <p style={{ color: "#6b7280" }}>No alerts yet (or alert-service is offline).</p>}
                {alerts.length > 0 && (
                  <table style={{ width: "100%", borderCollapse: "collapse", fontSize: "14px" }}>
                    <thead>
                      <tr style={{ borderBottom: "2px solid #e5e7eb", textAlign: "left" }}>
                        <th style={cell}>Time</th>
                        <th style={cell}>Hazard</th>
                        <th style={cell}>Channel</th>
                        <th style={cell}>Recipient</th>
                        <th style={cell}>Delivery</th>
                        <th style={cell}>Reason</th>
                      </tr>
                    </thead>
                    <tbody>
                      {alerts.map((a) => (
                        <tr key={a.id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                          <td style={cell}>{(a.sentAt || "").replace("T", " ").slice(0, 16)}</td>
                          <td style={cell}>{HAZARD_BY_KEY[a.hazard]?.label || a.hazard}</td>
                          <td style={cell}>{a.channel}</td>
                          <td style={cell}>{a.recipient}</td>
                          <td style={{
                            ...cell, fontWeight: "bold",
                            color: a.deliveryStatus === "FAILED" ? "#b91c1c" : a.deliveryStatus === "SENT" ? "#15803d" : "#6b7280"
                          }}>{a.deliveryStatus}</td>
                          <td style={cell}>{a.triggerReason}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}

const cell = { padding: "8px" };
const filterLabel = { display: "flex", flexDirection: "column", fontSize: "12px", color: "#374151", gap: "4px" };
const headerButton = {
  background: "white", color: "#0f766e", border: "none", borderRadius: "6px",
  padding: "6px 16px", cursor: "pointer", fontWeight: "bold"
};
const primaryButton = {
  background: "#0f766e", color: "white", border: "none", borderRadius: "6px",
  padding: "8px 16px", cursor: "pointer", fontWeight: "bold"
};
const secondaryButton = {
  background: "white", color: "#0f766e", border: "1px solid #0f766e", borderRadius: "6px",
  padding: "7px 12px", cursor: "pointer", fontWeight: "bold"
};

export default NationalDashboard;
