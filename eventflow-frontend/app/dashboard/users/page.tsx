"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Plus, Mail, Shield } from "lucide-react";
import { useAuth } from "@/contexts/auth-context";

/**
 * Users Page
 * Per PRD Part 10 - UI Section 3 (Users)
 * 
 * Features:
 * - RBAC table (name, email, role, status, last login)
 * - Invite user dialog
 * - Role management dropdown
 * - Prevent self-admin-revocation
 * - Role change confirmation modal
 */
export default function UsersPage() {
  const { user: currentUser } = useAuth();
  const [showInviteDialog, setShowInviteDialog] = useState(false);

  // TODO: Fetch from GraphQL
  const users = [
    {
      id: "1",
      name: "Admin User",
      email: "admin@eventflow.com",
      role: "WORKSPACE_ADMIN",
      status: "ACTIVE",
      lastLogin: new Date().toISOString(),
    },
    {
      id: "2",
      name: "Developer One",
      email: "dev@eventflow.com",
      role: "DEVELOPER",
      status: "ACTIVE",
      lastLogin: new Date(Date.now() - 86400000).toISOString(),
    },
    {
      id: "3",
      name: "Analyst User",
      email: "analyst@eventflow.com",
      role: "ANALYST",
      status: "INVITED",
      lastLogin: null,
    },
  ];

  const handleRoleChange = (userId: string, currentRole: string, newRole: string) => {
    // Check if user is trying to revoke own admin rights
    if (userId === currentUser?.id && currentRole === "WORKSPACE_ADMIN" && newRole !== "WORKSPACE_ADMIN") {
      alert("You cannot revoke your own admin privileges.");
      return;
    }

    if (confirm(`Change user role from ${currentRole} to ${newRole}?`)) {
      console.log("Changing role for user:", userId);
      // TODO: Call updateUserRole mutation
    }
  };

  const getRoleBadgeVariant = (role: string) => {
    switch (role) {
      case "WORKSPACE_ADMIN":
        return "destructive";
      case "DEVELOPER":
        return "default";
      case "ANALYST":
        return "secondary";
      default:
        return "secondary";
    }
  };

  const getStatusBadgeVariant = (status: string) => {
    switch (status) {
      case "ACTIVE":
        return "success";
      case "INVITED":
        return "warning";
      case "SUSPENDED":
        return "destructive";
      default:
        return "secondary";
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Users</h2>
          <p className="text-muted-foreground">
            Manage workspace access and role-based permissions
          </p>
        </div>
        <Button onClick={() => setShowInviteDialog(true)}>
          <Plus className="mr-2 h-4 w-4" />
          Invite User
        </Button>
      </div>

      {/* Users Table */}
      <Card>
        <CardHeader>
          <CardTitle>Workspace Members</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Name</TableHead>
                  <TableHead>Email</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Last Login</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {users.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">{user.name}</TableCell>
                    <TableCell className="text-muted-foreground">{user.email}</TableCell>
                    <TableCell>
                      <select
                        value={user.role}
                        onChange={(e) => handleRoleChange(user.id, user.role, e.target.value)}
                        className="h-8 rounded-md border border-input bg-background px-2 py-1 text-sm"
                        disabled={user.id === currentUser?.id && user.role === "WORKSPACE_ADMIN"}
                      >
                        <option value="WORKSPACE_ADMIN">Admin</option>
                        <option value="DEVELOPER">Developer</option>
                        <option value="ANALYST">Analyst</option>
                      </select>
                    </TableCell>
                    <TableCell>
                      <Badge variant={getStatusBadgeVariant(user.status)}>
                        {user.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {user.lastLogin
                        ? new Date(user.lastLogin).toLocaleDateString()
                        : "Never"}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm">
                        Edit
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Role Descriptions */}
      <Card>
        <CardHeader>
          <CardTitle>Role Permissions</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <Shield className="h-5 w-5 text-destructive mt-0.5" />
              <div>
                <h4 className="font-medium">Workspace Admin</h4>
                <p className="text-sm text-muted-foreground">
                  Full access to all features including user management, provider configuration, and billing.
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Shield className="h-5 w-5 text-primary mt-0.5" />
              <div>
                <h4 className="font-medium">Developer</h4>
                <p className="text-sm text-muted-foreground">
                  Can manage templates, view notifications, trigger DLQ replays. Cannot manage billing or users.
                </p>
              </div>
            </div>
            <div className="flex items-start gap-3">
              <Shield className="h-5 w-5 text-muted-foreground mt-0.5" />
              <div>
                <h4 className="font-medium">Analyst</h4>
                <p className="text-sm text-muted-foreground">
                  Read-only access to analytics, notifications, and audit logs. Cannot modify any settings.
                </p>
              </div>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Invite User Dialog */}
      {showInviteDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Invite User</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Email Address</label>
                <Input type="email" placeholder="user@example.com" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Role</label>
                <select className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
                  <option value="DEVELOPER">Developer</option>
                  <option value="ANALYST">Analyst</option>
                  <option value="WORKSPACE_ADMIN">Workspace Admin</option>
                </select>
              </div>
              <div className="flex gap-2">
                <Button className="flex-1">
                  <Mail className="mr-2 h-4 w-4" />
                  Send Invitation
                </Button>
                <Button variant="outline" onClick={() => setShowInviteDialog(false)}>
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
