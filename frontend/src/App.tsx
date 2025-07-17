import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

import AppLayout from '@/components/layout/AppLayout';
import ProtectedRoute from '@/components/layout/ProtectedRoute';
import ErrorBoundary from '@/components/common/ErrorBoundary';

import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';

import StudentDashboard from '@/pages/student/StudentDashboard';
import SubmitComplaintPage from '@/pages/student/SubmitComplaintPage';

import StaffDashboard from '@/pages/staff/StaffDashboard';

import AdminDashboard from '@/pages/admin/AdminDashboard';
import AllComplaintsPage from '@/pages/admin/AllComplaintsPage';
import StaffManagementPage from '@/pages/admin/StaffManagementPage';
import AnnouncementsPage from '@/pages/admin/AnnouncementsPage';
import MaintenanceSchedulePage from '@/pages/admin/MaintenanceSchedulePage';

import ComplaintDetailPage from '@/pages/shared/ComplaintDetailPage';
import NotificationsPage from '@/pages/shared/NotificationsPage';
import ProfilePage from '@/pages/shared/ProfilePage';

function RootRedirect() {
  const user = useAuthStore((s) => s.user);
  const isAuth = useAuthStore((s) => s.isAuthenticated);
  const hasHydrated = useAuthStore((s) => s.hasHydrated);

  if (!hasHydrated) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <p>Loading...</p>
      </div>
    );
  }

  if (!isAuth || !user) return <Navigate to="/login" replace />;
  switch (user.role) {
    case 'ADMIN': return <Navigate to="/admin/dashboard" replace />;
    case 'STAFF': return <Navigate to="/staff/dashboard" replace />;
    default: return <Navigate to="/student/dashboard" replace />;
  }
}

export default function App() {
  return (
    <ErrorBoundary>
      <Routes>
        {/* Public */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* Protected Layout */}
        <Route element={
          <ProtectedRoute>
            <AppLayout />
          </ProtectedRoute>
        }>
          {/* Student */}
          <Route path="/student/dashboard" element={
            <ProtectedRoute roles={['STUDENT']}>
              <StudentDashboard />
            </ProtectedRoute>
          } />
          <Route path="/student/submit" element={
            <ProtectedRoute roles={['STUDENT']}>
              <SubmitComplaintPage />
            </ProtectedRoute>
          } />

          {/* Staff */}
          <Route path="/staff/dashboard" element={
            <ProtectedRoute roles={['STAFF']}>
              <StaffDashboard />
            </ProtectedRoute>
          } />

          {/* Admin */}
          <Route path="/admin/dashboard" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminDashboard />
            </ProtectedRoute>
          } />
          <Route path="/admin/complaints" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AllComplaintsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/staff" element={
            <ProtectedRoute roles={['ADMIN']}>
              <StaffManagementPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/announcements" element={
            <ProtectedRoute roles={['ADMIN']}>
              <AnnouncementsPage />
            </ProtectedRoute>
          } />
          <Route path="/admin/maintenance" element={
            <ProtectedRoute roles={['ADMIN']}>
              <MaintenanceSchedulePage />
            </ProtectedRoute>
          } />

          {/* Shared */}
          <Route path="/complaints/:id" element={<ComplaintDetailPage />} />
          <Route path="/notifications" element={<NotificationsPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Route>

        {/* Root redirect */}
        <Route path="/" element={<RootRedirect />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </ErrorBoundary>
  );
}
