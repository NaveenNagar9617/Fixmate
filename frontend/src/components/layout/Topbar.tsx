import { Menu, LogOut } from 'lucide-react';
import { Button } from '@/components/ui/Button';
import NotificationBell from '@/components/notification/NotificationBell';
import { useAuth } from '@/hooks/useAuth';
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/Avatar';
import { useAuthStore } from '@/store/authStore';

interface TopbarProps {
  onMenuClick: () => void;
}

export default function Topbar({ onMenuClick }: TopbarProps) {
  const { logout } = useAuth();
  const user = useAuthStore((s) => s.user);

  return (
    <header className="sticky top-0 z-30 h-16 border-b bg-background/80 backdrop-blur-xl">
      <div className="flex items-center justify-between h-full px-4 lg:px-6">
        <button
          onClick={onMenuClick}
          className="lg:hidden p-2 rounded-lg hover:bg-muted transition-colors"
        >
          <Menu className="h-5 w-5" />
        </button>

        <div className="hidden lg:block">
          <h2 className="text-sm font-medium text-muted-foreground">
            Welcome back, <span className="text-foreground font-semibold">{user?.name}</span>
          </h2>
        </div>

        <div className="flex items-center gap-3">
          <NotificationBell />

          <div className="h-8 w-px bg-border" />

          <div className="flex items-center gap-2">
            <Avatar className="h-8 w-8">
              <AvatarImage src={user?.profilePhotoUrl || undefined} />
              <AvatarFallback>{user?.name?.charAt(0).toUpperCase()}</AvatarFallback>
            </Avatar>
            <span className="hidden sm:block text-sm font-medium">{user?.name}</span>
          </div>

          <Button variant="ghost" size="icon" onClick={logout} className="text-muted-foreground hover:text-destructive">
            <LogOut className="h-4 w-4" />
          </Button>
        </div>
      </div>
    </header>
  );
}
