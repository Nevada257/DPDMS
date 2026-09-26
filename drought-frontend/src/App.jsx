// App.jsx
import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import Login from "./auth/Login";
import DroughtList from "./hazards/drought/DroughtList";
import DroughtForm from "./hazards/drought/DroughtForm";
import DroughtApproval from "./hazards/drought/DroughtApproval";

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<Login />} />

        {/* Drought routes */}
        <Route path="/drought" element={<DroughtList />} />
        <Route path="/drought/new" element={<DroughtForm />} />
        <Route path="/drought/:id/edit" element={<DroughtForm />} />
        <Route path="/drought/:id/approve" element={<DroughtApproval />} />

        <Route path="*" element={<p style={{ color: "white", padding: 24 }}>Page not found</p>} />
      </Routes>
    </BrowserRouter>
  );
}