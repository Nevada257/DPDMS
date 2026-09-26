// auth/Login.jsx
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { api, setToken } from "../api/apiClient";
import "../App.css";

export default function Login() {
  const [username, setUsername] = useState("");
  const [role, setRole] = useState("DROUGHT_RECORDER");
  const [hazardScope, setHazardScope] = useState("DROUGHT");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await api.post("/auth/login", { username, role, hazardScope });
      setToken(res.token);
      localStorage.setItem("username", res.username);
      localStorage.setItem("role", res.role);
      navigate("/drought");
    } catch {
      setError("Login failed. Check your inputs.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="logo">DPDMS</div>
        <h1>Drought Management</h1>
        <p className="subtitle">Disaster Monitoring & Management System</p>

        <form onSubmit={handleSubmit}>
          <label>Username</label>
          <input
            type="text"
            placeholder="Enter username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
          />

          <label>Role</label>
          <select
            value={role}
            onChange={(e) => setRole(e.target.value)}
            style={{ width: "100%", padding: 13, border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 15 }}
          >
            <option value="DROUGHT_RECORDER">Drought Recorder</option>
            <option value="DROUGHT_SUPERVISOR">Drought Supervisor</option>
            <option value="PROVINCIAL_ADMIN">Provincial Admin</option>
            <option value="NATIONAL_USER">National User</option>
          </select>

          <label>Hazard Scope</label>
          <select
            value={hazardScope}
            onChange={(e) => setHazardScope(e.target.value)}
            style={{ width: "100%", padding: 13, border: "1px solid #cbd5e1", borderRadius: 8, fontSize: 15 }}
          >
            <option value="DROUGHT">Drought</option>
            <option value="FLOOD">Flood</option>
            <option value="FIRE">Fire</option>
            <option value="ZOONOTIC">Zoonotic</option>
            <option value="MINING">Mining</option>
            <option value="ALL">All (National)</option>
          </select>

          {error && <div className="error">{error}</div>}

          <button type="submit" disabled={loading}>
            {loading ? "Signing in..." : "Sign In"}
          </button>
        </form>

        <div className="login-info">
          <p>Drought Service</p>
          <span>Secure Role-Based Access</span>
        </div>
      </div>
    </div>
  );
}