"use client";

import { useAuth } from "@/contexts/auth-context";
import { useTheme } from "@/contexts/theme-context";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Link from "next/link";
import { 
  LayoutDashboard, 
  Bell, 
  FileText, 
  Settings, 
  Users, 
  Database,
  AlertCircle,
  BarChart3,
  LogOut,
  Moon,
  Sun,
  Menu,
  X
} from "lucide-react";
import { Button } from "@/components/ui/button";

/**
 * Dashboard Layout with Sidebar
 * Per PRD Part 10 - Application Shell
 * 240px fixed left sidebar, sticky topbar, main content area
 */
export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const { user, logout, isLoading } = useAuth();
  const { theme, setTheme, resolvedTheme } = useTheme();
  const router = useRouter();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  useEffect(() => {
    if (!isLoading && !user) {
      router.push("/login");
    }
  }, [user, isLoading, router]);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-primary"></div>
      </div>
    );
  }

  if (!user) {
    return null;
  }

  const navigation = [
    { name: "Dashboard", href: "/dashboard", icon: LayoutDashboard, group: "Monitoring" },
    { name: "Analytics", href: "/dashboard/analytics", icon: BarChart3, group: "Monitoring" },
    { name: "Notifications", href: "/dashboard/notifications", icon: Bell, group: "Monitoring" },
    { name: "Queues", href: "/dashboard/queues", icon: Database, group: "Monitoring" },
    { name: "Retries", href: "/dashboard/retries", icon: AlertCircle, group: "Monitoring" },
    { name: "DLQ", href: "/dashboard/dlq", icon: AlertCircle, group: "Monitoring" },
    { name: "Audit Logs", href: "/dashboard/audit", icon: FileText, group: "Monitoring" },
    { name: "Templates", href: "/dashboard/templates", icon: FileText, group: "Configuration" },
    { name: "Providers", href: "/dashboard/providers", icon: Settings, group: "Configuration" },
    { name: "Users", href: "/dashboard/users", icon: Users, group: "Configuration" },
    { name: "Settings", href: "/dashboard/settings", icon: Settings, group: "Configuration" },
  ];

  const groupedNav = navigation.reduce((acc, item) => {
    if (!acc[item.group]) {
      acc[item.group] = [];
    }
    acc[item.group].push(item);
    return acc;
  }, {} as Record<string, typeof navigation>);

  return (
    <div className="flex h-screen bg-background">
      {/* Mobile Menu Overlay */}
      {isMobileMenuOpen && (
        <div 
          className="fixed inset-0 bg-black/50 z-40 lg:hidden"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar - 240px fixed width */}
      <aside className={`
        fixed lg:static inset-y-0 left-0 z-50
        w-60 border-r bg-card flex flex-col
        transform transition-transform duration-200 ease-in-out
        ${isMobileMenuOpen ? 'translate-x-0' : '-translate-x-full lg:translate-x-0'}
      `}>
        {/* Logo */}
        <div className="h-16 flex items-center justify-between px-6 border-b">
          <div className="flex items-center">
            <div className="h-8 w-8 rounded bg-primary flex items-center justify-center">
              <span className="text-lg font-bold text-primary-foreground">EF</span>
            </div>
            <span className="ml-3 text-lg font-semibold">EventFlow</span>
          </div>
          <button 
            className="lg:hidden"
            onClick={() => setIsMobileMenuOpen(false)}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Navigation */}
        <nav className="flex-1 overflow-y-auto p-4 space-y-6">
          {Object.entries(groupedNav).map(([group, items]) => (
            <div key={group}>
              <h3 className="px-3 text-xs font-semibold text-muted-foreground uppercase tracking-wider mb-2">
                {group}
              </h3>
              <div className="space-y-1">
                {items.map((item) => (
                  <Link
                    key={item.name}
                    href={item.href}
                    onClick={() => setIsMobileMenuOpen(false)}
                    className="flex items-center px-3 py-2 text-sm font-medium rounded-md hover:bg-accent hover:text-accent-foreground transition-colors"
                  >
                    <item.icon className="mr-3 h-5 w-5" />
                    {item.name}
                  </Link>
                ))}
              </div>
            </div>
          ))}
        </nav>

        {/* User Profile */}
        <div className="border-t p-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center min-w-0">
              <div className="h-8 w-8 rounded-full bg-primary flex items-center justify-center">
                <span className="text-sm font-medium text-primary-foreground">
                  {user.name.charAt(0).toUpperCase()}
                </span>
              </div>
              <div className="ml-3 min-w-0">
                <p className="text-sm font-medium truncate">{user.name}</p>
                <p className="text-xs text-muted-foreground truncate">{user.role}</p>
              </div>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={logout}
              aria-label="Logout"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Topbar */}
        <header className="h-16 border-b bg-card px-4 lg:px-6 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button 
              className="lg:hidden"
              onClick={() => setIsMobileMenuOpen(true)}
            >
              <Menu className="h-5 w-5" />
            </button>
            <h1 className="text-xl font-semibold">Dashboard</h1>
          </div>
          <div className="flex items-center gap-2">
            <Button 
              variant="ghost" 
              size="icon" 
              onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
              aria-label="Toggle theme"
            >
              {resolvedTheme === "dark" ? (
                <Sun className="h-5 w-5" />
              ) : (
                <Moon className="h-5 w-5" />
              )}
            </Button>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
