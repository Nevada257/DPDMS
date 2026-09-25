import { useEffect, useState } from "react";

const API = "/api";

function App() {
  const [loggedIn, setLoggedIn] = useState(
      !!localStorage.getItem("token")
  );

  const [username, setUsername] = useState(
      localStorage.getItem("username") || ""
  );

  const [role, setRole] = useState(
      localStorage.getItem("role") || ""
  );

  const [page, setPage] = useState("dashboard");

  const [incidents, setIncidents] = useState([]);
  const [auditTrails, setAuditTrails] = useState([]);

  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  /* =========================
     LOGIN
  ========================= */

  const [loginUsername, setLoginUsername] = useState("");
  const [loginPassword, setLoginPassword] = useState("");
  const [loginLoading, setLoginLoading] = useState(false);

  const handleLogin = async (event) => {
    event.preventDefault();

    setLoginLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(`${API}/auth/login`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          username: loginUsername,
          password: loginPassword,
        }),
      });

      const data = await response.json();

      if (!response.ok) {
        setError(
            typeof data === "string"
                ? data
                : "Invalid username or password"
        );
        return;
      }

      localStorage.setItem("token", data.token);
      localStorage.setItem("username", data.username);
      localStorage.setItem("role", data.role);

      setUsername(data.username);
      setRole(data.role);
      setLoggedIn(true);
      setPage("dashboard");

      setLoginUsername("");
      setLoginPassword("");

    } catch (err) {
      console.error(err);
      setError("Could not connect to the backend.");
    } finally {
      setLoginLoading(false);
    }
  };

  /* =========================
     LOGOUT
  ========================= */

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("role");

    setLoggedIn(false);
    setUsername("");
    setRole("");
    setIncidents([]);
    setAuditTrails([]);
    setPage("dashboard");
  };

  /* =========================
     AUTH HEADERS
  ========================= */

  const authHeaders = () => ({
    Authorization: `Bearer ${localStorage.getItem("token")}`,
  });

  /* =========================
     GET INCIDENTS
  ========================= */

  const loadIncidents = async () => {
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
          `${API}/zoonotic-incidents`,
          {
            headers: authHeaders(),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      if (response.status === 403) {
        setError("You are not authorized to view these incidents.");
        return;
      }

      if (!response.ok) {
        throw new Error(
            `Request failed with status ${response.status}`
        );
      }

      const data = await response.json();

      setIncidents(data);
      setPage("incidents");

    } catch (err) {
      console.error(err);
      setError("Could not load incidents.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     DELETE INCIDENT
  ========================= */

  const deleteIncident = async (id) => {
    const confirmed = window.confirm(
        `Are you sure you want to delete incident ${id}?`
    );

    if (!confirmed) {
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
          `${API}/zoonotic-incidents/${id}`,
          {
            method: "DELETE",
            headers: authHeaders(),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      if (response.status === 403) {
        setError("You are not authorized to delete this incident.");
        return;
      }

      if (!response.ok) {
        throw new Error("Delete failed");
      }

      setMessage(`Incident ${id} deleted successfully.`);

      await loadIncidents();

    } catch (err) {
      console.error(err);
      setError("Could not delete the incident.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     CREATE INCIDENT
  ========================= */

  const emptyIncident = {
    ward: "",
    district: "",
    province: "",
    occurrenceDateTime: "",
    reporter: "",
    severity: "LOW",
    diseaseName: "",
    animalSpecies: "",
    confirmedHumanCases: 0,
    confirmedAnimalCases: 0,
    eventClassification: "CLUSTER",
    latitude: "",
    longitude: "",
  };

  const [newIncident, setNewIncident] =
      useState(emptyIncident);

  const handleIncidentChange = (event) => {
    const { name, value } = event.target;

    setNewIncident((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const createIncident = async (event) => {
    event.preventDefault();

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const incidentData = {
        ...newIncident,

        confirmedHumanCases:
            Number(newIncident.confirmedHumanCases),

        confirmedAnimalCases:
            Number(newIncident.confirmedAnimalCases),

        latitude:
            Number(newIncident.latitude),

        longitude:
            Number(newIncident.longitude),
      };

      const response = await fetch(
          `${API}/zoonotic-incidents`,
          {
            method: "POST",

            headers: {
              ...authHeaders(),
              "Content-Type": "application/json",
            },

            body: JSON.stringify(incidentData),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      const data = await response.json();

      if (!response.ok) {
        if (typeof data === "object") {
          setError(
              Object.values(data).join(" | ")
          );
        } else {
          setError(String(data));
        }

        return;
      }

      setMessage(
          `Incident created successfully. Status: ${data.status}`
      );

      setNewIncident(emptyIncident);

      setPage("incidents");

      await loadIncidents();

    } catch (err) {
      console.error(err);
      setError("Could not create incident.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     SUPERVISOR DATA
  ========================= */

  const loadSupervisorIncidents = async () => {
    if (role !== "PROVINCIAL_SUPERVISOR") {
      setError(
          "Only provincial supervisors can access this page."
      );
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
          `${API}/zoonotic-incidents`,
          {
            headers: authHeaders(),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      if (response.status === 403) {
        setError("Supervisor access is required.");
        return;
      }

      if (!response.ok) {
        throw new Error("Could not load supervisor data");
      }

      const data = await response.json();

      setIncidents(data);
      setPage("supervisor");

    } catch (err) {
      console.error(err);
      setError("Could not load supervisor incidents.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     APPROVE / REJECT / CORRECTION
  ========================= */

  const changeIncidentStatus = async (id, action) => {
    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
          `${API}/zoonotic-incidents/${id}/${action}`,
          {
            method: "POST",
            headers: authHeaders(),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      const data = await response.json();

      if (!response.ok) {
        setError(
            data?.message ||
            `Action failed with status ${response.status}`
        );
        return;
      }

      setMessage(
          `Incident ${id} is now ${data.status}.`
      );

      await loadSupervisorIncidents();

    } catch (err) {
      console.error(err);
      setError("Could not update incident status.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     AUDIT TRAIL
  ========================= */

  const loadAuditTrails = async () => {
    if (
        role !== "PROVINCIAL_SUPERVISOR" &&
        role !== "NATIONAL_USER"
    ) {
      setError(
          "Only supervisors and national users can view audit trails."
      );
      return;
    }

    setLoading(true);
    setError("");
    setMessage("");

    try {
      const response = await fetch(
          `${API}/audit-trails`,
          {
            headers: authHeaders(),
          }
      );

      if (response.status === 401) {
        logout();
        return;
      }

      if (response.status === 403) {
        setError("You are not authorized to view audit trails.");
        return;
      }

      if (!response.ok) {
        throw new Error("Could not load audit trails");
      }

      const data = await response.json();

      setAuditTrails(data);
      setPage("audit");

    } catch (err) {
      console.error(err);
      setError("Could not load audit trails.");
    } finally {
      setLoading(false);
    }
  };

  /* =========================
     CLEAR NOTIFICATIONS
  ========================= */

  const clearMessages = () => {
    setError("");
    setMessage("");
  };

  /* =========================
     LOGIN SCREEN
  ========================= */

  if (!loggedIn) {
    return (
        <div className="login-page">

          <div className="login-card">

            <h1>Zoonotic Disease Service</h1>

            <p className="subtitle">
              Incident Management System
            </p>

            <form onSubmit={handleLogin}>

              <label>Username</label>

              <input
                  type="text"
                  placeholder="Enter username"
                  value={loginUsername}
                  onChange={(event) =>
                      setLoginUsername(event.target.value)
                  }
                  required
              />

              <label>Password</label>

              <input
                  type="password"
                  placeholder="Enter password"
                  value={loginPassword}
                  onChange={(event) =>
                      setLoginPassword(event.target.value)
                  }
                  required
              />

              <button
                  type="submit"
                  disabled={loginLoading}
              >
                {loginLoading
                    ? "Logging in..."
                    : "Login"}
              </button>

            </form>

            {error && (
                <div className="error-message">
                  {error}
                </div>
            )}

          </div>

        </div>
    );
  }

  /* =========================
     COMMON HEADER
  ========================= */

  const Header = () => (
      <header className="dashboard-header">

        <div>
          <h1>Zoonotic Disease Service</h1>
          <p>Incident Management System</p>
        </div>

        <div className="header-user">

        <span>
          {username} | {role}
        </span>

          <button
              className="logout-button"
              onClick={logout}
          >
            Logout
          </button>

        </div>

      </header>
  );

  /* =========================
     DASHBOARD
  ========================= */

  if (page === "dashboard") {
    return (
        <div className="dashboard-page">

          <Header />

          <main className="dashboard-container">

            <div className="welcome-card">

              <h2>Dashboard</h2>

              <p>
                Welcome, <strong>{username}</strong>
              </p>

              <p>
                Role: <strong>{role}</strong>
              </p>

            </div>

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            <div className="dashboard-cards">

              <div className="dashboard-card">

                <div className="card-icon">
                  📋
                </div>

                <h3>Incidents</h3>

                <p>
                  View and manage zoonotic disease incidents.
                </p>

                <button onClick={loadIncidents}>
                  View Incidents
                </button>

              </div>

              <div className="dashboard-card">

                <div className="card-icon">
                  ➕
                </div>

                <h3>Create Incident</h3>

                <p>
                  Report a new zoonotic disease incident.
                </p>

                <button
                    onClick={() => {
                      clearMessages();
                      setPage("create");
                    }}
                >
                  Create Incident
                </button>

              </div>

              <div className="dashboard-card">

                <div className="card-icon">
                  ✅
                </div>

                <h3>Supervisor Approval</h3>

                <p>
                  Review and approve pending incidents.
                </p>

                <button
                    onClick={loadSupervisorIncidents}
                    disabled={
                        role !== "PROVINCIAL_SUPERVISOR"
                    }
                >
                  {role === "PROVINCIAL_SUPERVISOR"
                      ? "Open Approval"
                      : "Supervisor Only"}
                </button>

              </div>

              <div className="dashboard-card">

                <div className="card-icon">
                  📜
                </div>

                <h3>Audit Trail</h3>

                <p>
                  View incident actions and status changes.
                </p>

                <button
                    onClick={loadAuditTrails}
                    disabled={
                        role !== "PROVINCIAL_SUPERVISOR" &&
                        role !== "NATIONAL_USER"
                    }
                >
                  {role === "PROVINCIAL_SUPERVISOR" ||
                  role === "NATIONAL_USER"
                      ? "View Audit Trail"
                      : "Restricted"}
                </button>

              </div>

            </div>

          </main>

        </div>
    );
  }

  /* =========================
     INCIDENTS
  ========================= */

  if (page === "incidents") {
    return (
        <div className="dashboard-page">

          <Header />

          <main className="dashboard-container">

            <div className="page-title-row">

              <div>
                <h2>Incidents</h2>

                <p>
                  Incidents available according to your role
                  and scope.
                </p>
              </div>

              <button
                  className="back-button"
                  onClick={() => setPage("dashboard")}
              >
                ← Dashboard
              </button>

            </div>

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            <div className="refresh-row">

              <button
                  className="refresh-button"
                  onClick={loadIncidents}
              >
                🔄 Refresh
              </button>

            </div>

            {loading && (
                <div className="loading">
                  Loading...
                </div>
            )}

            {!loading &&
                incidents.length === 0 && (
                    <div className="empty-box">
                      No incidents found.
                    </div>
                )}

            {!loading &&
                incidents.length > 0 && (

                    <div className="table-container">

                      <table>

                        <thead>

                        <tr>
                          <th>ID</th>
                          <th>Ward</th>
                          <th>District</th>
                          <th>Province</th>
                          <th>Disease</th>
                          <th>Animal Species</th>
                          <th>Human Cases</th>
                          <th>Animal Cases</th>
                          <th>Severity</th>
                          <th>Classification</th>
                          <th>Status</th>
                          <th>Actions</th>
                        </tr>

                        </thead>

                        <tbody>

                        {incidents.map((incident) => (

                            <tr key={incident.id}>

                              <td>{incident.id}</td>
                              <td>{incident.ward}</td>
                              <td>{incident.district}</td>
                              <td>{incident.province}</td>
                              <td>{incident.diseaseName}</td>
                              <td>{incident.animalSpecies}</td>

                              <td>
                                {incident.confirmedHumanCases}
                              </td>

                              <td>
                                {incident.confirmedAnimalCases}
                              </td>

                              <td>
                          <span className="severity">
                            {incident.severity}
                          </span>
                              </td>

                              <td>
                                {incident.eventClassification}
                              </td>

                              <td>
                          <span className="status">
                            {incident.status}
                          </span>
                              </td>

                              <td>

                                {(role === "WARD_RECORDER" ||
                                    role ===
                                    "PROVINCIAL_SUPERVISOR") && (

                                    <button
                                        className="delete-button"
                                        onClick={() =>
                                            deleteIncident(
                                                incident.id
                                            )
                                        }
                                    >
                                      Delete
                                    </button>

                                )}

                              </td>

                            </tr>

                        ))}

                        </tbody>

                      </table>

                    </div>

                )}

          </main>

        </div>
    );
  }

  /* =========================
     CREATE INCIDENT
  ========================= */

  if (page === "create") {
    return (
        <div className="dashboard-page">

          <Header />

          <main className="dashboard-container">

            <div className="page-title-row">

              <div>
                <h2>Create Zoonotic Incident</h2>

                <p>
                  New incidents are created with PENDING
                  status.
                </p>
              </div>

              <button
                  className="back-button"
                  onClick={() => setPage("dashboard")}
              >
                ← Dashboard
              </button>

            </div>

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            {role === "NATIONAL_USER" ? (

                <div className="error-box">
                  National users are read-only and cannot
                  create incidents.
                </div>

            ) : (

                <form
                    className="incident-form"
                    onSubmit={createIncident}
                >

                  <div className="form-grid">

                    <div>
                      <label>Ward</label>

                      <input
                          name="ward"
                          value={newIncident.ward}
                          onChange={handleIncidentChange}
                          placeholder="e.g. Ward 5"
                          required
                      />
                    </div>

                    <div>
                      <label>District</label>

                      <input
                          name="district"
                          value={newIncident.district}
                          onChange={handleIncidentChange}
                          placeholder="e.g. Harare"
                          required
                      />
                    </div>

                    <div>
                      <label>Province</label>

                      <input
                          name="province"
                          value={newIncident.province}
                          onChange={handleIncidentChange}
                          placeholder="e.g. Harare"
                          required
                      />
                    </div>

                    <div>
                      <label>Reporter</label>

                      <input
                          name="reporter"
                          value={newIncident.reporter}
                          onChange={handleIncidentChange}
                          placeholder="Reporter name"
                      />
                    </div>

                    <div>
                      <label>Occurrence Date & Time</label>

                      <input
                          type="datetime-local"
                          name="occurrenceDateTime"
                          value={
                            newIncident.occurrenceDateTime
                          }
                          onChange={handleIncidentChange}
                          required
                      />
                    </div>

                    <div>
                      <label>Disease Name</label>

                      <input
                          name="diseaseName"
                          value={newIncident.diseaseName}
                          onChange={handleIncidentChange}
                          placeholder="Disease name"
                          required
                      />
                    </div>

                    <div>
                      <label>Animal Species</label>

                      <input
                          name="animalSpecies"
                          value={newIncident.animalSpecies}
                          onChange={handleIncidentChange}
                          placeholder="e.g. Cattle"
                          required
                      />
                    </div>

                    <div>
                      <label>Severity</label>

                      <select
                          name="severity"
                          value={newIncident.severity}
                          onChange={handleIncidentChange}
                      >
                        <option value="LOW">LOW</option>
                        <option value="MEDIUM">
                          MEDIUM
                        </option>
                        <option value="HIGH">HIGH</option>
                        <option value="CRITICAL">
                          CRITICAL
                        </option>
                      </select>
                    </div>

                    <div>
                      <label>Event Classification</label>

                      <select
                          name="eventClassification"
                          value={
                            newIncident.eventClassification
                          }
                          onChange={handleIncidentChange}
                      >
                        <option value="CLUSTER">
                          CLUSTER
                        </option>

                        <option value="OUTBREAK">
                          OUTBREAK
                        </option>
                      </select>
                    </div>

                    <div>
                      <label>Confirmed Human Cases</label>

                      <input
                          type="number"
                          min="0"
                          name="confirmedHumanCases"
                          value={
                            newIncident.confirmedHumanCases
                          }
                          onChange={handleIncidentChange}
                          required
                      />
                    </div>

                    <div>
                      <label>Confirmed Animal Cases</label>

                      <input
                          type="number"
                          min="0"
                          name="confirmedAnimalCases"
                          value={
                            newIncident.confirmedAnimalCases
                          }
                          onChange={handleIncidentChange}
                          required
                      />
                    </div>

                    <div>
                      <label>Latitude</label>

                      <input
                          type="number"
                          step="any"
                          min="-90"
                          max="90"
                          name="latitude"
                          value={newIncident.latitude}
                          onChange={handleIncidentChange}
                          placeholder="-17.8252"
                          required
                      />
                    </div>

                    <div>
                      <label>Longitude</label>

                      <input
                          type="number"
                          step="any"
                          min="-180"
                          max="180"
                          name="longitude"
                          value={newIncident.longitude}
                          onChange={handleIncidentChange}
                          placeholder="31.0335"
                          required
                      />
                    </div>

                  </div>

                  <div className="form-actions">

                    <button
                        type="submit"
                        className="submit-button"
                        disabled={loading}
                    >
                      {loading
                          ? "Creating..."
                          : "Create Incident"}
                    </button>

                    <button
                        type="button"
                        className="cancel-button"
                        onClick={() => setPage("dashboard")}
                    >
                      Cancel
                    </button>

                  </div>

                </form>

            )}

          </main>

        </div>
    );
  }

  /* =========================
     SUPERVISOR
  ========================= */

  if (page === "supervisor") {
    const pendingCount =
        incidents.filter(
            (incident) =>
                incident.status === "PENDING"
        ).length;

    const approvedCount =
        incidents.filter(
            (incident) =>
                incident.status === "APPROVED"
        ).length;

    const rejectedCount =
        incidents.filter(
            (incident) =>
                incident.status === "REJECTED"
        ).length;

    return (
        <div className="dashboard-page">

          <Header />

          <main className="dashboard-container">

            <div className="page-title-row">

              <div>
                <h2>Supervisor Approval</h2>

                <p>
                  Review incidents within your province.
                </p>
              </div>

              <button
                  className="back-button"
                  onClick={() => setPage("dashboard")}
              >
                ← Dashboard
              </button>

            </div>

            <div className="summary-cards">

              <div className="summary-card">
                <strong>
                  {incidents.length}
                </strong>
                <span>Total</span>
              </div>

              <div className="summary-card pending">
                <strong>
                  {pendingCount}
                </strong>
                <span>Pending</span>
              </div>

              <div className="summary-card approved">
                <strong>
                  {approvedCount}
                </strong>
                <span>Approved</span>
              </div>

              <div className="summary-card rejected">
                <strong>
                  {rejectedCount}
                </strong>
                <span>Rejected</span>
              </div>

            </div>

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            <div className="refresh-row">

              <button
                  className="refresh-button"
                  onClick={loadSupervisorIncidents}
              >
                🔄 Refresh
              </button>

            </div>

            <div className="table-container">

              <table>

                <thead>

                <tr>
                  <th>ID</th>
                  <th>Ward</th>
                  <th>Disease</th>
                  <th>Species</th>
                  <th>Severity</th>
                  <th>Human Cases</th>
                  <th>Animal Cases</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>

                </thead>

                <tbody>

                {incidents.map((incident) => (

                    <tr key={incident.id}>

                      <td>{incident.id}</td>
                      <td>{incident.ward}</td>
                      <td>{incident.diseaseName}</td>
                      <td>{incident.animalSpecies}</td>
                      <td>{incident.severity}</td>
                      <td>
                        {incident.confirmedHumanCases}
                      </td>
                      <td>
                        {incident.confirmedAnimalCases}
                      </td>

                      <td>
                      <span className="status">
                        {incident.status}
                      </span>
                      </td>

                      <td>

                        {incident.status ===
                        "PENDING" ? (

                            <div className="action-buttons">

                              <button
                                  className="approve-button"
                                  onClick={() =>
                                      changeIncidentStatus(
                                          incident.id,
                                          "approve"
                                      )
                                  }
                              >
                                Approve
                              </button>

                              <button
                                  className="reject-button"
                                  onClick={() =>
                                      changeIncidentStatus(
                                          incident.id,
                                          "reject"
                                      )
                                  }
                              >
                                Reject
                              </button>

                              <button
                                  className="correction-button"
                                  onClick={() =>
                                      changeIncidentStatus(
                                          incident.id,
                                          "request-correction"
                                      )
                                  }
                              >
                                Correction
                              </button>

                            </div>

                        ) : (

                            <span className="completed">
                          Completed
                        </span>

                        )}

                      </td>

                    </tr>

                ))}

                </tbody>

              </table>

            </div>

          </main>

        </div>
    );
  }

  /* =========================
     AUDIT TRAIL
  ========================= */

  if (page === "audit") {
    return (
        <div className="dashboard-page">

          <Header />

          <main className="dashboard-container">

            <div className="page-title-row">

              <div>
                <h2>Audit Trail</h2>

                <p>
                  Record of incident actions and status
                  transitions.
                </p>
              </div>

              <button
                  className="back-button"
                  onClick={() => setPage("dashboard")}
              >
                ← Dashboard
              </button>

            </div>

            {message && (
                <div className="success-box">
                  {message}
                </div>
            )}

            {error && (
                <div className="error-box">
                  {error}
                </div>
            )}

            <div className="refresh-row">

              <button
                  className="refresh-button"
                  onClick={loadAuditTrails}
              >
                🔄 Refresh
              </button>

            </div>

            {loading && (
                <div className="loading">
                  Loading audit records...
                </div>
            )}

            {!loading &&
                auditTrails.length === 0 && (
                    <div className="empty-box">
                      No audit records found.
                    </div>
                )}

            {!loading &&
                auditTrails.length > 0 && (

                    <div className="table-container">

                      <table>

                        <thead>

                        <tr>
                          <th>ID</th>
                          <th>Incident ID</th>
                          <th>Action</th>
                          <th>Previous Status</th>
                          <th>New Status</th>
                          <th>Performed By</th>
                          <th>Date & Time</th>
                        </tr>

                        </thead>

                        <tbody>

                        {auditTrails.map((audit) => (

                            <tr key={audit.id}>

                              <td>{audit.id}</td>

                              <td>{audit.incidentId}</td>

                              <td>
                                <strong>
                                  {audit.action}
                                </strong>
                              </td>

                              <td>
                                {audit.previousStatus || "-"}
                              </td>

                              <td>
                                {audit.newStatus || "-"}
                              </td>

                              <td>
                                {audit.performedBy}
                              </td>

                              <td>
                                {audit.performedAt
                                    ? new Date(
                                        audit.performedAt
                                    ).toLocaleString()
                                    : "-"}
                              </td>

                            </tr>

                        ))}

                        </tbody>

                      </table>

                    </div>

                )}

          </main>

        </div>
    );
  }

  return null;
}

export default App;