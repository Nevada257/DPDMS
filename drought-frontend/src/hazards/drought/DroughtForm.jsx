// hazards/drought/DroughtForm.jsx
import { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { droughtApi } from "./droughtApi";
import { getUsername, getRole } from "../../api/apiClient";
import "../../App.css";

const emptyForm = {
  ward: "",
  district: "",
  province: "",
  dateTimeOfOccurrence: "",
  reporter: "",
  severity: "LOW",
  latitude: "",
  longitude: "",
  rainfallDeficitMm: "",
  consecutiveDryDays: "",
  cropFailurePercentage: "",
  peopleFacingWaterShortages: "",
  livestockMortalityCount: "",
};

export default function DroughtForm() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();

  const [form, setForm] = useState(emptyForm);
  const [loading, setLoading] = useState(isEdit);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const username = getUsername() || "user";
  const role = getRole() || "USER";

  useEffect(() => {
    if (isEdit) loadIncident();
  }, [id]);

  async function loadIncident() {
    try {
      const inc = await droughtApi.getOne(id);
      setForm({ ...emptyForm, ...inc });
    } catch {
      setError("Could not load incident.");
    } finally {
      setLoading(false);
    }
  }

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setSaving(true);
    setError(null);

    try {
      const payload = {
        ...form,
        latitude: parseFloat(form.latitude),
        longitude: parseFloat(form.longitude),
        rainfallDeficitMm: parseFloat(form.rainfallDeficitMm),
        consecutiveDryDays: parseInt(form.consecutiveDryDays, 10),
        cropFailurePercentage: parseFloat(form.cropFailurePercentage),
        peopleFacingWaterShortages: parseInt(
          form.peopleFacingWaterShortages,
          10
        ),
        livestockMortalityCount: parseInt(form.livestockMortalityCount, 10),
      };

      if (isEdit) {
        await droughtApi.update(id, payload);
      } else {
        await droughtApi.create(payload);
      }
      navigate("/drought");
    } catch {
      setError("Save failed. Check the fields and try again.");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="empty-state">Loading...</div>;

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
          <button
            className="logout-button"
            onClick={() => navigate("/drought")}
          >
            ← Back
          </button>
        </div>
      </header>

      <main className="dashboard-content">
        <div className="welcome">
          <h1>
            {isEdit
              ? `Edit Drought Incident #${id}`
              : "Add Drought Incident"}
          </h1>
          <p>
            {isEdit
              ? "Update the details below and resubmit."
              : "Enter the details of the new drought incident."}
          </p>
        </div>

        {error && <div className="error dashboard-error">{error}</div>}

        <section className="incident-form-section">
          <form className="incident-form" onSubmit={handleSubmit}>
            <h3>Location</h3>
            <div className="form-grid">
              <div>
                <label>Ward</label>
                <input
                  value={form.ward}
                  onChange={(e) => update("ward", e.target.value)}
                  required
                />
              </div>
              <div>
                <label>District</label>
                <input
                  value={form.district}
                  onChange={(e) => update("district", e.target.value)}
                  required
                />
              </div>
              <div>
                <label>Province</label>
                <input
                  value={form.province}
                  onChange={(e) => update("province", e.target.value)}
                  required
                />
              </div>
              <div>
                <label>Latitude</label>
                <input
                  type="number"
                  step="any"
                  value={form.latitude}
                  onChange={(e) => update("latitude", e.target.value)}
                  required
                />
              </div>
              <div>
                <label>Longitude</label>
                <input
                  type="number"
                  step="any"
                  value={form.longitude}
                  onChange={(e) => update("longitude", e.target.value)}
                  required
                />
              </div>
            </div>

            <h3>Incident Information</h3>
            <div className="form-grid">
              <div>
                <label>Date &amp; Time of Occurrence</label>
                <input
                  type="datetime-local"
                  value={form.dateTimeOfOccurrence}
                  onChange={(e) =>
                    update("dateTimeOfOccurrence", e.target.value)
                  }
                  required
                />
              </div>
              <div>
                <label>Reporter</label>
                <input
                  value={form.reporter}
                  onChange={(e) => update("reporter", e.target.value)}
                  required
                />
              </div>
              <div>
                <label>Severity</label>
                <select
                  value={form.severity}
                  onChange={(e) => update("severity", e.target.value)}
                >
                  <option value="LOW">LOW</option>
                  <option value="MEDIUM">MEDIUM</option>
                  <option value="HIGH">HIGH</option>
                  <option value="CRITICAL">CRITICAL</option>
                </select>
              </div>
            </div>

            <h3>Drought Indicators</h3>
            <div className="form-grid">
              <div>
                <label>Rainfall Deficit (mm)</label>
                <input
                  type="number"
                  step="any"
                  value={form.rainfallDeficitMm}
                  onChange={(e) =>
                    update("rainfallDeficitMm", e.target.value)
                  }
                  required
                />
              </div>
              <div>
                <label>Consecutive Dry Days</label>
                <input
                  type="number"
                  value={form.consecutiveDryDays}
                  onChange={(e) =>
                    update("consecutiveDryDays", e.target.value)
                  }
                  required
                />
              </div>
              <div>
                <label>Crop Failure (%)</label>
                <input
                  type="number"
                  step="any"
                  value={form.cropFailurePercentage}
                  onChange={(e) =>
                    update("cropFailurePercentage", e.target.value)
                  }
                  required
                />
              </div>
              <div>
                <label>People Facing Water Shortages</label>
                <input
                  type="number"
                  value={form.peopleFacingWaterShortages}
                  onChange={(e) =>
                    update("peopleFacingWaterShortages", e.target.value)
                  }
                  required
                />
              </div>
              <div>
                <label>Livestock Mortality Count</label>
                <input
                  type="number"
                  value={form.livestockMortalityCount}
                  onChange={(e) =>
                    update("livestockMortalityCount", e.target.value)
                  }
                  required
                />
              </div>
            </div>

            <div className="form-actions">
              <button
                type="button"
                className="secondary-button"
                onClick={() => navigate("/drought")}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="primary-button"
                disabled={saving}
              >
                {saving
                  ? "Saving..."
                  : isEdit
                  ? "Update Incident"
                  : "Create Incident"}
              </button>
            </div>
          </form>
        </section>
      </main>
    </div>
  );
}