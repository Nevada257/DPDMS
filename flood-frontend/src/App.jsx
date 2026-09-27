import { useState } from "react";
import "./App.css";
import NationalDashboard from "./NationalDashboard";
import DroughtPanel from "./DroughtPanel";
import FirePanel from "./FirePanel";
import MiningPanel from "./MiningPanel";


import {
  MapContainer,
  TileLayer,
  Marker,
  Popup
} from "react-leaflet";

import "leaflet/dist/leaflet.css";
import L from "leaflet";

// Fix Leaflet marker icons
delete L.Icon.Default.prototype._getIconUrl;

L.Icon.Default.mergeOptions({
  iconRetinaUrl:
    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png",
  iconUrl:
    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png",
  shadowUrl:
    "https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png"
});

const API_URL = "http://localhost:8080";

function App() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");

  const [credentials, setCredentials] = useState(null);
  const [incidents, setIncidents] = useState([]);

  const [showForm, setShowForm] = useState(false);
  const [editingIncidentId, setEditingIncidentId] = useState(null);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const [form, setForm] = useState({
    ward: "",
    district: "",
    province: "",
    occurrenceDateTime: "",
    reporter: "",
    severity: "",
    status: "",
    latitude: "",
    longitude: "",
    peakWaterLevel: "",
    riverBasin: "",
    householdsDisplaced: "",
    areaFlooded: "",
    durationOfInundation: ""
  });

  // --------------------------------------------------
  // ROLE
  // --------------------------------------------------

  const getRole = (user) => {
    if (user === "recorder") return "RECORDER";
    if (user === "supervisor") return "SUPERVISOR";
    if (user === "national") return "NATIONAL";

    return "USER";
  };

  // --------------------------------------------------
  // LOAD INCIDENTS
  // --------------------------------------------------

  const loadIncidents = async (auth) => {
    try {
      const response = await fetch(`${API_URL}/api/floods`, {
        headers: {
          Authorization: `Bearer ${auth}`
        }
      });

      if (!response.ok) {
        throw new Error("Could not load flood incidents.");
      }

      const data = await response.json();

      setIncidents(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error(err);
      throw err;
    }
  };

  // --------------------------------------------------
  // LOGIN
  // --------------------------------------------------

    const handleLogin = async (e) => {
    e.preventDefault();

    setError("");
    setMessage("");
    setLoading(true);

    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ username, password })
      });

      if (response.status === 401) {
        setError("Invalid username or password.");
        setLoading(false);
        return;
      }

      if (!response.ok) {
        setError("Unable to connect to Auth Service.");
        setLoading(false);
        return;
      }

      const data = await response.json();

      setCredentials({
        username: data.username,
        role: data.role,
        hazardScope: data.hazardScope,
        ward: data.ward,
        auth: data.token
      });

      await loadIncidents(data.token);

      setLoading(false);
    } catch (err) {
      console.error(err);

      setError(
        "Could not reach the Gateway. Make sure it is running on port 8080."
      );

      setLoading(false);
    }
  };

  // --------------------------------------------------
  // FORM CHANGE
  // --------------------------------------------------

  const handleChange = (e) => {
    const { name, value } = e.target;

    setForm({
      ...form,
      [name]: value
    });
  };

  // --------------------------------------------------
  // EDIT CORRECTION INCIDENT
  // --------------------------------------------------

  const handleEditIncident = (incident) => {
    setEditingIncidentId(incident.id);

    setForm({
      ward: incident.ward || "",
      district: incident.district || "",
      province: incident.province || "",

      occurrenceDateTime: incident.occurrenceDateTime
        ? incident.occurrenceDateTime.slice(0, 16)
        : "",

      reporter: incident.reporter || "",
      severity: incident.severity || "",
      status: incident.status || "",

      latitude: incident.latitude ?? "",
      longitude: incident.longitude ?? "",

      peakWaterLevel: incident.peakWaterLevel ?? "",
      riverBasin: incident.riverBasin || "",
      householdsDisplaced: incident.householdsDisplaced ?? "",
      areaFlooded: incident.areaFlooded ?? "",
      durationOfInundation: incident.durationOfInundation ?? ""
    });

    setMessage("");
    setError("");
    setShowForm(true);
  };

  // --------------------------------------------------
  // SUBMIT / UPDATE INCIDENT
  // --------------------------------------------------

  const handleSubmitIncident = async (e) => {
    e.preventDefault();

    setMessage("");
    setError("");
    setLoading(true);

    const incident = {
      ward: form.ward,
      district: form.district,
      province: form.province,
      occurrenceDateTime: form.occurrenceDateTime,
      reporter: form.reporter,
      severity: form.severity,
      status: form.status,

      latitude: Number(form.latitude),
      longitude: Number(form.longitude),

      peakWaterLevel: Number(form.peakWaterLevel),
      riverBasin: form.riverBasin,

      householdsDisplaced: Number(form.householdsDisplaced),
      areaFlooded: Number(form.areaFlooded),
      durationOfInundation: Number(form.durationOfInundation)
    };

    try {
      const url = editingIncidentId
        ? `${API_URL}/api/floods/${editingIncidentId}`
        : `${API_URL}/api/floods`;

      const response = await fetch(url, {
        method: editingIncidentId ? "PUT" : "POST",

        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${credentials.auth}`
        },

        body: JSON.stringify(incident)
      });

      const data = await response.json();

      if (!response.ok) {
        if (data.errors) {
          const validationErrors = Object.entries(data.errors)
            .map(([field, msg]) => `${field}: ${msg}`)
            .join("\n");

          setError(validationErrors);
        } else {
          setError(
            data.message || "Could not create flood incident."
          );
        }

        setLoading(false);
        return;
      }

      if (editingIncidentId) {
        setMessage(
          `Flood incident #${editingIncidentId} updated and resubmitted successfully.`
        );
      } else {
        setMessage(
          `Flood incident #${data.id} created successfully. Approval status: ${data.approvalStatus}.`
        );
      }

      setForm({
        ward: "",
        district: "",
        province: "",
        occurrenceDateTime: "",
        reporter: "",
        severity: "",
        status: "",
        latitude: "",
        longitude: "",
        peakWaterLevel: "",
        riverBasin: "",
        householdsDisplaced: "",
        areaFlooded: "",
        durationOfInundation: ""
      });

      await loadIncidents(credentials.auth);

      setEditingIncidentId(null);
      setShowForm(false);
      setLoading(false);

    } catch (err) {
      console.error(err);

      setError(
        "Could not connect to the Flood Service."
      );

      setLoading(false);
    }
  };

  // --------------------------------------------------
  // APPROVAL WORKFLOW
  // --------------------------------------------------

  const handleApprovalAction = async (incidentId, action) => {
    let reason = "";

    if (action === "reject" || action === "corrections") {
      reason = window.prompt(
        action === "reject"
          ? "Enter rejection reason:"
          : "Enter correction request:"
      );

      if (!reason) {
        return;
      }
    }

    try {
      setLoading(true);
      setError("");
      setMessage("");

      const url =
        `${API_URL}/api/floods/${incidentId}/${action}`;

      const options = {
        method: "POST",

        headers: {
          Authorization: `Bearer ${credentials.auth}`,
          "Content-Type": "application/json"
        }
      };

      if (reason) {
        options.body = JSON.stringify({
          reason: reason
        });
      }

      const response = await fetch(url, options);

      const data = await response.json();

      if (!response.ok) {
        setError(
          data.message ||
          `Could not ${action} incident.`
        );

        return;
      }

      setMessage(
        `Incident #${incidentId} ${action} action completed successfully.`
      );

      await loadIncidents(credentials.auth);

    } catch (err) {
      console.error(err);

      setError(
        "Could not connect to the Flood Service."
      );

    } finally {
      setLoading(false);
    }
  };

  // --------------------------------------------------
  // LOGOUT
  // --------------------------------------------------

  const logout = () => {
    setCredentials(null);
    setIncidents([]);

    setUsername("");
    setPassword("");

    setError("");
    setMessage("");

    setEditingIncidentId(null);
    setShowForm(false);
  };

  // --------------------------------------------------
  // LOGIN SCREEN
  // --------------------------------------------------

  if (!credentials) {
    return (
      <div className="login-page">

        <div className="login-card">

          <div className="logo">
            DPDMS
          </div>

          <h1>
            Flood Management
          </h1>

          <p className="subtitle">
            Disaster Monitoring & Management System
          </p>

          <form onSubmit={handleLogin}>

            <label>
              Username
            </label>

            <input
              type="text"
              placeholder="Enter username"
              value={username}
              onChange={(e) =>
                setUsername(e.target.value)
              }
              required
            />

            <label>
              Password
            </label>

            <input
              type="password"
              placeholder="Enter password"
              value={password}
              onChange={(e) =>
                setPassword(e.target.value)
              }
              required
            />

            {error && (
              <div className="error">
                {error}
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Signing in..."
                : "Sign In"}
            </button>

          </form>

          <div className="login-info">
            <p>Flood Service</p>
            <span>
              Secure Role-Based Access
            </span>
          </div>

        </div>

      </div>
    );
  }

  // --------------------------------------------------
  // STATISTICS
  // --------------------------------------------------

  const pending = incidents.filter(
    (incident) =>
      incident.approvalStatus === "PENDING"
  ).length;

  const approved = incidents.filter(
    (incident) =>
      incident.approvalStatus === "APPROVED"
  ).length;

  const rejected = incidents.filter(
    (incident) =>
      incident.approvalStatus === "REJECTED"
  ).length;

  const corrections = incidents.filter(
    (incident) =>
      incident.approvalStatus ===
      "CORRECTIONS_REQUESTED"
  ).length;

  // --------------------------------------------------
  // APPROVED INCIDENTS ONLY
  // --------------------------------------------------

  const approvedIncidents =
    incidents.filter(
      (incident) =>
        incident.approvalStatus === "APPROVED"
    );

  // Only approved incidents with valid GPS coordinates
  // will appear on the map.
  const mapIncidents =
    approvedIncidents.filter(
      (incident) =>
        incident.latitude !== null &&
        incident.latitude !== undefined &&
        incident.longitude !== null &&
        incident.longitude !== undefined &&
        !Number.isNaN(Number(incident.latitude)) &&
        !Number.isNaN(Number(incident.longitude))
    );

  // --------------------------------------------------
  // NATIONAL USERS GET A CROSS-HAZARD DASHBOARD INSTEAD
  if (credentials.hazardScope === "ALL") {
    return (
      <NationalDashboard credentials={credentials} onLogout={logout} />
    );
  }


  if (credentials.hazardScope === "DROUGHT") {
    return (
      <DroughtPanel credentials={credentials} onLogout={logout} />
    );
  }
   if (credentials.hazardScope === "FIRE") {
    return (
      <FirePanel credentials={credentials} onLogout={logout} />
    );
  }

  if (credentials.hazardScope === "MINING") {
    return (
      <MiningPanel credentials={credentials} onLogout={logout} />
    );
  }

 // DASHBOARD
  // --------------------------------------------------

  return (
    <div className="dashboard">

      {/* TOP BAR */}
      <header className="topbar">

        <div>

          <div className="brand">
            DPDMS
          </div>

          <div className="brand-subtitle">
            Disaster Monitoring & Management System
          </div>

        </div>

        <div className="user-section">

          <div>
            <strong>
              {credentials.username}
            </strong>

            <span>
              {credentials.role}
            </span>
          </div>

          <button
            className="logout-button"
            onClick={logout}
          >
            Logout
          </button>

        </div>

      </header>

      <main className="dashboard-content">

        {/* WELCOME */}
        <div className="welcome">

          <div>

            <h1>
              Flood Dashboard
            </h1>

            <p>
              Monitor and manage flood incidents across the province.
            </p>

          </div>

        </div>

        {/* STATISTICS */}
        <div className="stats-grid">

          <div className="stat-card">
            <span>
              Total Incidents
            </span>

            <strong>
              {incidents.length}
            </strong>
          </div>

          <div className="stat-card pending-card">
            <span>
              Pending
            </span>

            <strong>
              {pending}
            </strong>
          </div>

          <div className="stat-card approved-card">
            <span>
              Approved
            </span>

            <strong>
              {approved}
            </strong>
          </div>

          <div className="stat-card rejected-card">
            <span>
              Rejected
            </span>

            <strong>
              {rejected}
            </strong>
          </div>

          <div className="stat-card correction-card">
            <span>
              Corrections
            </span>

            <strong>
              {corrections}
            </strong>
          </div>

        </div>

        {/* MESSAGES */}

        {message && (
          <div className="success-message">
            {message}
          </div>
        )}

        {error && (
          <div className="error dashboard-error">
            {error}
          </div>
        )}

        {/* =====================================================
            APPROVED FLOOD INCIDENTS
        ====================================================== */}

        <section className="incidents-section approved-dashboard-section">

          <div className="section-header">

            <div>

              <h2>
                Approved Flood Incidents
              </h2>

              <p>
                Only approved incidents are displayed on the dashboard.
              </p>

            </div>

            <div className="approved-count">
              {approvedIncidents.length} approved
            </div>

          </div>

          {approvedIncidents.length === 0 ? (

            <div className="empty-state">

              <h3>
                No approved flood incidents
              </h3>

              <p>
                Approved incidents will appear here after supervisor approval.
              </p>

            </div>

          ) : (

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
                    <th>Coordinates</th>
                  </tr>

                </thead>

                <tbody>

                  {approvedIncidents.map(
                    (incident) => (

                      <tr
                        key={`approved-${incident.id}`}
                      >

                        <td>
                          #{incident.id}
                        </td>

                        <td>
                          {incident.ward || "-"}
                        </td>

                        <td>
                          {incident.district || "-"}
                        </td>

                        <td>
                          {incident.province || "-"}
                        </td>

                        <td>
                          {incident.severity || "-"}
                        </td>

                        <td>
                          {incident.status || "-"}
                        </td>

                        <td>
                          {incident.latitude ?? "-"},
                          {" "}
                          {incident.longitude ?? "-"}
                        </td>

                      </tr>

                    )
                  )}

                </tbody>

              </table>

            </div>

          )}

        </section>

        {/* =====================================================
            FLOOD INCIDENT MAP
        ====================================================== */}

        <section className="incidents-section">

          <div className="section-header">

            <div>

              <h2>
                Flood Incident Map
              </h2>

              <p>
                Only APPROVED flood incidents are shown on the map.
              </p>

            </div>

            <div className="approved-count">
              {mapIncidents.length} mapped
            </div>

          </div>

          {mapIncidents.length === 0 ? (

            <div className="empty-state">

              <h3>
                No approved incidents with coordinates
              </h3>

              <p>
                Approved incidents with valid GPS coordinates will appear here.
              </p>

            </div>

          ) : (

            <MapContainer
              center={[
                Number(mapIncidents[0].latitude),
                Number(mapIncidents[0].longitude)
              ]}
              zoom={10}
              scrollWheelZoom={true}
              style={{
                height: "450px",
                width: "100%",
                borderRadius: "12px"
              }}
            >

              <TileLayer
                attribution='&copy; OpenStreetMap contributors'
                url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
              />

              {mapIncidents.map(
                (incident) => (

                  <Marker
                    key={`map-${incident.id}`}
                    position={[
                      Number(incident.latitude),
                      Number(incident.longitude)
                    ]}
                  >

                    <Popup>

                      <div>

                        <strong>
                          Flood Incident #{incident.id}
                        </strong>

                        <br />

                        <br />

                        <strong>
                          Ward:
                        </strong>{" "}
                        {incident.ward || "-"}

                        <br />

                        <strong>
                          District:
                        </strong>{" "}
                        {incident.district || "-"}

                        <br />

                        <strong>
                          Province:
                        </strong>{" "}
                        {incident.province || "-"}

                        <br />

                        <strong>
                          Severity:
                        </strong>{" "}
                        {incident.severity || "-"}

                        <br />

                        <strong>
                          Status:
                        </strong>{" "}
                        {incident.status || "-"}

                        <br />

                        <strong>
                          Peak Water Level:
                        </strong>{" "}
                        {incident.peakWaterLevel ?? "-"} m

                        <br />

                        <strong>
                          Households Displaced:
                        </strong>{" "}
                        {incident.householdsDisplaced ?? "-"}

                        <br />

                        <strong>
                          Area Flooded:
                        </strong>{" "}
                        {incident.areaFlooded ?? "-"} ha

                        <br />

                        <strong>
                          River Basin:
                        </strong>{" "}
                        {incident.riverBasin || "-"}

                        <br />

                        <br />

                        <strong>
                          APPROVED
                        </strong>

                      </div>

                    </Popup>

                  </Marker>

                )
              )}

            </MapContainer>

          )}

        </section>

        {/* =====================================================
            ADD / EDIT FORM
        ====================================================== */}

        {showForm ? (

          <section className="incident-form-section">

            <div className="section-header">

              <div>

                <h2>

                  {editingIncidentId
                    ? `Edit Flood Incident #${editingIncidentId}`
                    : "Add Flood Incident"}

                </h2>

                <p>

                  {editingIncidentId
                    ? "Correct the requested information and resubmit the incident."
                    : "Enter the details of the flood incident."}

                </p>

              </div>

              <button
                className="secondary-button"
                onClick={() => {
                  setEditingIncidentId(null);
                  setShowForm(false);
                }}
              >
                Cancel
              </button>

            </div>

            <form
              className="incident-form"
              onSubmit={handleSubmitIncident}
            >

              {/* LOCATION */}

              <h3>
                Location
              </h3>

              <div className="form-grid">

                <div>

                  <label>
                    Ward
                  </label>

                  <input
                    name="ward"
                    value={form.ward}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    District
                  </label>

                  <input
                    name="district"
                    value={form.district}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Province
                  </label>

                  <input
                    name="province"
                    value={form.province}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    River Basin / Catchment
                  </label>

                  <input
                    name="riverBasin"
                    value={form.riverBasin}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Latitude
                  </label>

                  <input
                    type="number"
                    step="any"
                    name="latitude"
                    value={form.latitude}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Longitude
                  </label>

                  <input
                    type="number"
                    step="any"
                    name="longitude"
                    value={form.longitude}
                    onChange={handleChange}
                    required
                  />

                </div>

              </div>

              {/* INCIDENT INFORMATION */}

              <h3>
                Incident Information
              </h3>

              <div className="form-grid">

                <div>

                  <label>
                    Occurrence Date & Time
                  </label>

                  <input
                    type="datetime-local"
                    name="occurrenceDateTime"
                    value={form.occurrenceDateTime}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Reporter
                  </label>

                  <input
                    name="reporter"
                    value={form.reporter}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Severity
                  </label>

                  <select
                    name="severity"
                    value={form.severity}
                    onChange={handleChange}
                    required
                  >

                    <option value="">
                      Select severity
                    </option>

                    <option value="LOW">
                      LOW
                    </option>

                    <option value="MEDIUM">
                      MEDIUM
                    </option>

                    <option value="HIGH">
                      HIGH
                    </option>

                    <option value="CRITICAL">
                      CRITICAL
                    </option>

                  </select>

                </div>

                <div>

                  <label>
                    Status
                  </label>

                  <select
                    name="status"
                    value={form.status}
                    onChange={handleChange}
                    required
                  >

                    <option value="">
                      Select status
                    </option>

                    <option value="ACTIVE">
                      ACTIVE
                    </option>

                    <option value="RESOLVED">
                      RESOLVED
                    </option>

                    <option value="CLOSED">
                      CLOSED
                    </option>

                  </select>

                </div>

              </div>

              {/* FLOOD INDICATORS */}

              <h3>
                Flood Indicators
              </h3>

              <div className="form-grid">

                <div>

                  <label>
                    Peak Water Level (metres)
                  </label>

                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    name="peakWaterLevel"
                    value={form.peakWaterLevel}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Households Displaced
                  </label>

                  <input
                    type="number"
                    min="0"
                    name="householdsDisplaced"
                    value={form.householdsDisplaced}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Area Flooded (hectares)
                  </label>

                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    name="areaFlooded"
                    value={form.areaFlooded}
                    onChange={handleChange}
                    required
                  />

                </div>

                <div>

                  <label>
                    Duration of Inundation (days)
                  </label>

                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    name="durationOfInundation"
                    value={form.durationOfInundation}
                    onChange={handleChange}
                    required
                  />

                </div>

              </div>

              {/* FORM BUTTONS */}

              <div className="form-actions">

                <button
                  type="button"
                  className="secondary-button"
                  onClick={() => {
                    setEditingIncidentId(null);
                    setShowForm(false);
                  }}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-button"
                  disabled={loading}
                >

                  {loading
                    ? "Submitting..."
                    : editingIncidentId
                      ? "Resubmit Corrected Incident"
                      : "Submit Flood Incident"}

                </button>

              </div>

            </form>

          </section>

        ) : (

          /* =====================================================
             INCIDENT MANAGEMENT
          ====================================================== */

          <section className="incidents-section">

            <div className="section-header">

              <div>

                <h2>
                  Incident Management
                </h2>

                <p>
                  Manage all flood incidents and approval workflow.
                </p>

              </div>

              {credentials.role === "RECORDER" && (

                <button
                  className="primary-button"
                  onClick={() => {
                    setMessage("");
                    setError("");
                    setEditingIncidentId(null);

                    setForm({
                      ward: "",
                      district: "",
                      province: "",
                      occurrenceDateTime: "",
                      reporter: "",
                      severity: "",
                      status: "",
                      latitude: "",
                      longitude: "",
                      peakWaterLevel: "",
                      riverBasin: "",
                      householdsDisplaced: "",
                      areaFlooded: "",
                      durationOfInundation: ""
                    });

                    setShowForm(true);
                  }}
                >
                  + Add Flood Incident
                </button>

              )}

            </div>

            {incidents.length === 0 ? (

              <div className="empty-state">

                <h3>
                  No flood incidents found
                </h3>

                <p>
                  There are currently no flood incidents in the database.
                </p>

              </div>

            ) : (

              <div className="table-container">

                <table>

                  <thead>

                    <tr>

                      <th>
                        ID
                      </th>

                      <th>
                        Ward
                      </th>

                      <th>
                        District
                      </th>

                      <th>
                        Province
                      </th>

                      <th>
                        Severity
                      </th>

                      <th>
                        Status
                      </th>

                      <th>
                        Approval
                      </th>

                      {(credentials.role === "SUPERVISOR" ||
                        credentials.role === "RECORDER") && (

                        <th>
                          Actions
                        </th>

                      )}

                    </tr>

                  </thead>

                  <tbody>

                    {incidents.map(
                      (incident) => (

                        <tr
                          key={incident.id}
                        >

                          <td>
                            #{incident.id}
                          </td>

                          <td>
                            {incident.ward || "-"}
                          </td>

                          <td>
                            {incident.district || "-"}
                          </td>

                          <td>
                            {incident.province || "-"}
                          </td>

                          <td>
                            {incident.severity || "-"}
                          </td>

                          <td>
                            {incident.status || "-"}
                          </td>

                          <td>

                            <span
                              className={`approval ${
                                incident.approvalStatus
                                  ? incident.approvalStatus.toLowerCase()
                                  : ""
                              }`}
                            >

                              {incident.approvalStatus || "-"}

                            </span>

                          </td>

                          {(credentials.role === "SUPERVISOR" ||
                            credentials.role === "RECORDER") && (

                            <td>

                              {/* SUPERVISOR ACTIONS */}

                              {credentials.role === "SUPERVISOR" &&
                                incident.approvalStatus === "PENDING" && (

                                  <>

                                    <button
                                      onClick={() =>
                                        handleApprovalAction(
                                          incident.id,
                                          "approve"
                                        )
                                      }
                                    >
                                      Approve
                                    </button>

                                    <button
                                      onClick={() =>
                                        handleApprovalAction(
                                          incident.id,
                                          "reject"
                                        )
                                      }
                                    >
                                      Reject
                                    </button>

                                    <button
                                      onClick={() =>
                                        handleApprovalAction(
                                          incident.id,
                                          "corrections"
                                        )
                                      }
                                    >
                                      Request Corrections
                                    </button>

                                  </>

                                )}

                              {/* RECORDER CORRECTION */}

                              {credentials.role === "RECORDER" &&
                                incident.approvalStatus ===
                                  "CORRECTIONS_REQUESTED" && (

                                  <button
                                    onClick={() =>
                                      handleEditIncident(incident)
                                    }
                                  >
                                    Edit & Resubmit
                                  </button>

                                )}

                            </td>

                          )}

                        </tr>

                      )
                    )}

                  </tbody>

                </table>

              </div>

            )}

          </section>

        )}

      </main>

    </div>
  );
}

export default App;