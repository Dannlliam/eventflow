"use client";

import { useAuth } from "@/contexts/auth-context";
import { useRouter } from "next/navigation";
import { useEffect } from "react";
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
  Sun
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
  const router = useRouter();

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
      {/* Sidebar - 240px fixed width */}
      <aside className="w-60 border-r bg-card flex flex-col">
        {/* Logo */}
        <div className="h-16 flex items-center px-6 border-b">
          <div className="h-8 w-8 rounded bg-primary flex items-center justify-center">
            <span className="text-lg font-bold text-primary-foreground">EF</span>
          </div>
          <span className="ml-3 text-lg font-semibold">EventFlow</span>
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
        <header className="h-16 border-b bg-card px-6 flex items-center justify-between">
          <div className="flex items-center">
            <h1 className="text-xl font-semibold">Dashboard</h1>
          </div>
          <div className="flex items-center gap-2">
            <Button variant="ghost" size="icon" aria-label="Toggle theme">
              <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
              <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
            </Button>
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-y-auto p-6">
          {children}
        </main>
      </div>
    </div>
  );
}
