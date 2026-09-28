import { useState, useEffect } from "react";

const API_URL = "http://localhost:8080";

function ZoonoticPanel({ credentials, onLogout, onBack }) {
  const [incidents, setIncidents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState(null);

  const emptyForm = {
    ward: credentials.ward || "",
    district: "Rushinga",
    province: "Mashonaland Central",
    occurrenceDateTime: "",
    reporter: credentials.username || "",
    severity: "MEDIUM",
    latitude: "",
    longitude: "",
    diseaseName: "",
    animalSpecies: "",
    confirmedHumanCases: "",
    confirmedAnimalCases: "",
    eventClassification: "CLUSTER"
  };

  const [form, setForm] = useState(emptyForm);

  useEffect(() => {
    loadIncidents();
  }, []);

  const loadIncidents = async () => {
    setLoading(true);
    setError("");

    try {
      const response = await fetch(`${API_URL}/api/zoonotic-incidents`, {
        headers: { Authorization: `Bearer ${credentials.auth}` }
      });

      if (!response.ok) {
        setError("Could not load zoonotic disease incidents.");
        setLoading(false);
        return;
      }

      const data = await response.json();
      setIncidents(Array.isArray(data) ? data : []);
      setLoading(false);
    } catch (err) {
      console.error(err);
      setError("Could not reach the Gateway.");
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm({ ...form, [name]: value });
  };

  const resetForm = () => {
    setForm(emptyForm);
    setShowForm(false);
    setEditingId(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setMessage("");

    const payload = {
      ...form,
      latitude: form.latitude === "" ? null : Number(form.latitude),
      longitude: form.longitude === "" ? null : Number(form.longitude),
      confirmedHumanCases: form.confirmedHumanCases === "" ? 0 : Number(form.confirmedHumanCases),
      confirmedAnimalCases: form.confirmedAnimalCases === "" ? 0 : Number(form.confirmedAnimalCases)
    };

    try {
      const url = editingId
        ? `${API_URL}/api/zoonotic-incidents/${editingId}`
        : `${API_URL}/api/zoonotic-incidents`;

      const response = await fetch(url, {
        method: editingId ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${credentials.auth}`
        },
        body: JSON.stringify(payload)
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.message || body.error || "Could not save the incident.");
        return;
      }

      setMessage(editingId ? "Incident updated." : "Incident submitted.");
      resetForm();
      loadIncidents();
    } catch (err) {
      console.error(err);
      setError("Could not reach the Gateway.");
    }
  };

  const startEdit = (incident) => {
    setForm({
      ward: incident.ward || "",
      district: incident.district || "",
      province: incident.province || "",
      occurrenceDateTime: incident.occurrenceDateTime || "",
      reporter: incident.reporter || "",
      severity: incident.severity || "MEDIUM",
      latitude: incident.latitude ?? "",
      longitude: incident.longitude ?? "",
      diseaseName: incident.diseaseName || "",
      animalSpecies: incident.animalSpecies || "",
      confirmedHumanCases: incident.confirmedHumanCases ?? "",
      confirmedAnimalCases: incident.confirmedAnimalCases ?? "",
      eventClassification: incident.eventClassification || "CLUSTER"
    });
    setEditingId(incident.id);
    setShowForm(true);
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this incident?")) return;

    try {
      const response = await fetch(`${API_URL}/api/zoonotic-incidents/${id}`, {
        method: "DELETE",
        headers: { Authorization: `Bearer ${credentials.auth}` }
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.message || body.error || "Could not delete the incident.");
        return;
      }

      setMessage("Incident deleted.");
      loadIncidents();
    } catch (err) {
      console.error(err);
      setError("Could not reach the Gateway.");
    }
  };

  const doAction = async (id, action) => {
    try {
      const response = await fetch(`${API_URL}/api/zoonotic-incidents/${id}/${action}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${credentials.auth}` }
      });

      if (!response.ok) {
        const body = await response.json().catch(() => ({}));
        setError(body.message || body.error || "Action failed.");
        return;
      }

      setMessage("Incident updated.");
      loadIncidents();
    } catch (err) {
      console.error(err);
      setError("Could not reach the Gateway.");
    }
  };

  const pending = incidents.filter((i) => i.status === "PENDING").length;
  const approved = incidents.filter((i) => i.status === "APPROVED").length;
  const rejected = incidents.filter((i) => i.status === "REJECTED").length;

  const inputStyle = {
    width: "100%",
    padding: "8px",
    marginBottom: "10px",
    border: "1px solid #d1d5db",
    borderRadius: "6px",
    boxSizing: "border-box"
  };

  const labelStyle = { fontSize: "13px", fontWeight: "bold", color: "#374151" };

  return (
    <div style={{ minHeight: "100vh", background: "#f3f4f6" }}>
      <div
        style={{
          background: "#7c3aed",
          color: "white",
          padding: "20px 32px",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
        }}
      >
        <div>
          <h1 style={{ margin: 0, fontSize: "24px" }}>DPDMS</h1>
          <p style={{ margin: 0, opacity: 0.85 }}>Zoonotic Disease Management</p>
        </div>
        <div style={{ textAlign: "right" }}>
          <div style={{ fontWeight: "bold" }}>{credentials.username}</div>
          <div style={{ opacity: 0.85, fontSize: "14px" }}>{credentials.role}</div>
          {onBack && (
            <button
              onClick={onBack}
              style={{
                marginTop: "8px",
                marginRight: "8px",
                background: "white",
                color: "#7c3aed",
                border: "none",
                borderRadius: "6px",
                padding: "6px 16px",
                cursor: "pointer",
                fontWeight: "bold"
              }}
            >
              ← Back to Dashboard
            </button>
          )}
          <button
            onClick={onLogout}
            style={{
              marginTop: "8px",
              background: "white",
              color: "#7c3aed",
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

      <div style={{ padding: "32px", maxWidth: "1100px", margin: "0 auto" }}>
        <h2 style={{ textAlign: "center" }}>Zoonotic Disease Dashboard</h2>

        {error && (
          <p style={{ textAlign: "center", color: "#dc2626", background: "#fee2e2", padding: "10px", borderRadius: "8px" }}>
            {error}
          </p>
        )}
        {message && (
          <p style={{ textAlign: "center", color: "#065f46", background: "#d1fae5", padding: "10px", borderRadius: "8px" }}>
            {message}
          </p>
        )}

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fit, minmax(150px, 1fr))",
            gap: "16px",
            margin: "24px 0"
          }}
        >
          {[
            ["Total", incidents.length, "#374151"],
            ["Pending", pending, "#d97706"],
            ["Approved", approved, "#059669"],
            ["Rejected", rejected, "#dc2626"]
          ].map(([label, value, color]) => (
            <div
              key={label}
              style={{
                background: "white",
                borderRadius: "10px",
                padding: "16px",
                textAlign: "center",
                borderLeft: `4px solid ${color}`,
                boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
              }}
            >
              <div style={{ color: "#6b7280" }}>{label}</div>
              <div style={{ fontSize: "28px", fontWeight: "bold" }}>{value}</div>
            </div>
          ))}
        </div>

        {credentials.role === "RECORDER" && (
          <div style={{ marginBottom: "20px" }}>
            <button
              onClick={() => (showForm ? resetForm() : setShowForm(true))}
              style={{
                background: "#7c3aed",
                color: "white",
                border: "none",
                borderRadius: "6px",
                padding: "10px 20px",
                cursor: "pointer",
                fontWeight: "bold"
              }}
            >
              {showForm ? "Cancel" : "+ Report Zoonotic Disease Incident"}
            </button>
          </div>
        )}

        {showForm && (
          <form
            onSubmit={handleSubmit}
            style={{
              background: "white",
              borderRadius: "10px",
              padding: "20px",
              marginBottom: "24px",
              boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
            }}
          >
            <h3>{editingId ? "Edit Incident" : "New Zoonotic Disease Incident"}</h3>

            <label style={labelStyle}>Ward</label>
            <input style={inputStyle} name="ward" value={form.ward} onChange={handleChange} required
              readOnly={credentials.role === "RECORDER"}
              title={credentials.role === "RECORDER" ? "Recorders can only capture incidents in their own ward" : undefined} />

            <label style={labelStyle}>District</label>
            <input style={inputStyle} name="district" value={form.district} onChange={handleChange} required />

            <label style={labelStyle}>Province</label>
            <input style={inputStyle} name="province" value={form.province} onChange={handleChange} required />

            <label style={labelStyle}>Occurrence Date/Time</label>
            <input style={inputStyle} type="datetime-local" name="occurrenceDateTime" value={form.occurrenceDateTime} onChange={handleChange} required />

            <label style={labelStyle}>Reporter</label>
            <input style={inputStyle} name="reporter" value={form.reporter} onChange={handleChange} required />

            <label style={labelStyle}>Severity</label>
            <select style={inputStyle} name="severity" value={form.severity} onChange={handleChange}>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="CRITICAL">CRITICAL</option>
            </select>

            <label style={labelStyle}>Latitude</label>
            <input style={inputStyle} type="number" step="any" name="latitude" value={form.latitude} onChange={handleChange} />

            <label style={labelStyle}>Longitude</label>
            <input style={inputStyle} type="number" step="any" name="longitude" value={form.longitude} onChange={handleChange} />

            <label style={labelStyle}>Disease / Pathogen Name</label>
            <input style={inputStyle} name="diseaseName" value={form.diseaseName} onChange={handleChange} required />

            <label style={labelStyle}>Animal Species Affected</label>
            <input style={inputStyle} name="animalSpecies" value={form.animalSpecies} onChange={handleChange} required />

            <label style={labelStyle}>Confirmed Human Cases</label>
            <input style={inputStyle} type="number" name="confirmedHumanCases" value={form.confirmedHumanCases} onChange={handleChange} />

            <label style={labelStyle}>Confirmed Animal Cases</label>
            <input style={inputStyle} type="number" name="confirmedAnimalCases" value={form.confirmedAnimalCases} onChange={handleChange} />

            <label style={labelStyle}>Event Classification</label>
            <select style={inputStyle} name="eventClassification" value={form.eventClassification} onChange={handleChange}>
              <option value="CLUSTER">CLUSTER</option>
              <option value="OUTBREAK">OUTBREAK</option>
            </select>

            <button
              type="submit"
              style={{
                background: "#7c3aed",
                color: "white",
                border: "none",
                borderRadius: "6px",
                padding: "10px 24px",
                cursor: "pointer",
                fontWeight: "bold",
                marginTop: "10px"
              }}
            >
              {editingId ? "Save Changes" : "Submit"}
            </button>
          </form>
        )}

        <div
          style={{
            background: "white",
            borderRadius: "10px",
            padding: "20px",
            boxShadow: "0 1px 3px rgba(0,0,0,0.1)"
          }}
        >
          <h3>Zoonotic Disease Incidents</h3>

          {loading && <p>Loading...</p>}

          {!loading && incidents.length === 0 && (
            <p style={{ color: "#6b7280" }}>No incidents visible to you yet.</p>
          )}

          {!loading && incidents.length > 0 && (
            <table style={{ width: "100%", borderCollapse: "collapse" }}>
              <thead>
                <tr style={{ borderBottom: "2px solid #e5e7eb" }}>
                  <th style={{ textAlign: "left", padding: "8px" }}>Ward</th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Disease</th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Severity</th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Status</th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Reporter</th>
                  <th style={{ textAlign: "left", padding: "8px" }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {incidents.map((incident) => (
                  <tr key={incident.id} style={{ borderBottom: "1px solid #f3f4f6" }}>
                    <td style={{ padding: "8px" }}>{incident.ward}</td>
                    <td style={{ padding: "8px" }}>{incident.diseaseName}</td>
                    <td style={{ padding: "8px" }}>{incident.severity}</td>
                    <td style={{ padding: "8px" }}>{incident.status}</td>
                    <td style={{ padding: "8px" }}>{incident.reporter}</td>
                    <td style={{ padding: "8px" }}>
                      {credentials.role === "RECORDER" &&
                        incident.status === "PENDING" &&
                        incident.createdByUsername === credentials.username && (
                          <>
                            <button onClick={() => startEdit(incident)} style={{ marginRight: "6px" }}>
                              Edit
                            </button>
                            <button onClick={() => handleDelete(incident.id)}>Delete</button>
                          </>
                        )}

                      {credentials.role === "SUPERVISOR" && incident.status === "PENDING" && (
                        <>
                          <button onClick={() => doAction(incident.id, "approve")} style={{ marginRight: "6px" }}>
                            Approve
                          </button>
                          <button onClick={() => doAction(incident.id, "reject")} style={{ marginRight: "6px" }}>
                            Reject
                          </button>
                          <button onClick={() => doAction(incident.id, "request-correction")}>
                            Request Correction
                          </button>
                        </>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}

export default ZoonoticPanel;