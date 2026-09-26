// hazards/drought/droughtApi.js
import { api } from "../../api/apiClient";

const BASE = "/api/drought/incidents";

export const droughtApi = {
  list: () => api.get(BASE),

  getOne: (id) => api.get(`${BASE}/${id}`),

  create: (incident) => api.post(BASE, incident),

  update: (id, incident) => api.put(`${BASE}/${id}`, incident),

  remove: (id) => api.delete(`${BASE}/${id}`),

  approve: (id, performedBy) =>
    api.patch(`${BASE}/${id}/approve?performedBy=${encodeURIComponent(performedBy)}`),

  reject: (id, reason, performedBy) =>
    api.patch(
      `${BASE}/${id}/reject?reason=${encodeURIComponent(reason)}&performedBy=${encodeURIComponent(performedBy)}`
    ),

  requestCorrection: (id, notes, performedBy) =>
    api.patch(
      `${BASE}/${id}/request-correction?notes=${encodeURIComponent(notes)}&performedBy=${encodeURIComponent(performedBy)}`
    ),
};