import { Navigate } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

interface ProtectedRouteProps {
  children: React.ReactNode;
  roles?: string[];
}

export default function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  const { isAuthenticated, user, hasHydrated } = useAuthStore();

  // Wait for Zustand persist to finish loading from localStorage
  if (!hasHydrated) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <p>Loading...</p>
      </div>
    );
  }

  if (!isAuthenticated || !user) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(user.role)) {
    const redirect = user.role === 'ADMIN' ? '/admin/dashboard'
      : user.role === 'STAFF' ? '/staff/dashboard'
      : '/student/dashboard';
    return <Navigate to={redirect} replace />;
  }

  return <>{children}</>;
}
