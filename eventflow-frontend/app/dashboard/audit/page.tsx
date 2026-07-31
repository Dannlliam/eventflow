"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
import { Search, ChevronDown, ChevronRight } from "lucide-react";

/**
 * Audit Logs Page
 * Per PRD Part 10 - UI Section 10 (Audit Logs)
 * 
 * Features:
 * - Table with timestamp, user, action, entity, IP address
 * - Filter by user or action type
 * - Expandable rows with JSON diff viewer
 * - Security and compliance tracking
 */
export default function AuditPage() {
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [searchTerm, setSearchTerm] = useState("");

  // TODO: Fetch from GraphQL
  const auditLogs = [
    {
      id: "1",
      timestamp: new Date().toISOString(),
      userId: "user_123",
      userName: "Admin User",
      action: "TEMPLATE_VERSION_PUBLISH",
      entityType: "TEMPLATE_VERSION",
      entityId: "tmpl_v_456",
      ipAddress: "192.168.1.100",
      changes: {
        before: { body: "Old template body", version: 1 },
        after: { body: "New template body", version: 2 },
      },
    },
    {
      id: "2",
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      userId: "user_789",
      userName: "Developer User",
      action: "DLQ_REPLAY",
      entityType: "NOTIFICATION",
      entityId: "evt_failed_001",
      ipAddress: "192.168.1.101",
      changes: {
        before: { status: "DLQ" },
        after: { status: "QUEUED", attemptCount: 0 },
      },
    },
    {
      id: "3",
      timestamp: new Date(Date.now() - 7200000).toISOString(),
      userId: "user_123",
      userName: "Admin User",
      action: "PROVIDER_CONFIG_UPDATE",
      entityType: "PROVIDER",
      entityId: "provider_sendgrid",
      ipAddress: "192.168.1.100",
      changes: {
        before: { rateLimit: 1000, enabled: true },
        after: { rateLimit: 1500, enabled: true },
      },
    },
  ];

  const toggleRow = (id: string) => {
    const newExpanded = new Set(expandedRows);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedRows(newExpanded);
  };

  const getActionBadgeVariant = (action: string) => {
    if (action.includes("DELETE") || action.includes("REVOKE")) {
      return "destructive";
    } else if (action.includes("CREATE") || action.includes("PUBLISH")) {
      return "success";
    } else if (action.includes("UPDATE") || action.includes("MODIFY")) {
      return "warning";
    }
    return "default";
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Audit Logs</h2>
        <p className="text-muted-foreground">
          Security and compliance tracking for all system changes
        </p>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-4">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by user, action, or entity..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <select className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
              <option value="">All Actions</option>
              <option value="TEMPLATE">Template Actions</option>
              <option value="DLQ">DLQ Actions</option>
              <option value="PROVIDER">Provider Actions</option>
              <option value="USER">User Actions</option>
            </select>
            <select className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
              <option value="">All Users</option>
              <option value="user_123">Admin User</option>
              <option value="user_789">Developer User</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Audit Logs Table */}
      <Card>
        <CardHeader>
          <CardTitle>Audit Trail</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12"></TableHead>
                  <TableHead>Timestamp</TableHead>
                  <TableHead>User</TableHead>
                  <TableHead>Action</TableHead>
                  <TableHead>Entity</TableHead>
                  <TableHead>IP Address</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {auditLogs.map((log) => (
                  <>
                    <TableRow
                      key={log.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => toggleRow(log.id)}
                    >
                      <TableCell>
                        {expandedRows.has(log.id) ? (
                          <ChevronDown className="h-4 w-4" />
                        ) : (
                          <ChevronRight className="h-4 w-4" />
                        )}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(log.timestamp).toLocaleString()}
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-medium">{log.userName}</p>
                          <p className="text-xs text-muted-foreground">{log.userId}</p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant={getActionBadgeVariant(log.action)}>
                          {log.action.replace(/_/g, " ")}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="text-sm">{log.entityType}</p>
                          <p className="text-xs font-mono text-muted-foreground">
                            {log.entityId}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell className="font-mono text-sm text-muted-foreground">
                        {log.ipAddress}
                      </TableCell>
                    </TableRow>
                    {expandedRows.has(log.id) && (
                      <TableRow>
                        <TableCell colSpan={6} className="bg-muted/30">
                          <div className="p-4 space-y-2">
                            <h4 className="text-sm font-medium mb-2">Changes</h4>
                            <div className="grid grid-cols-2 gap-4">
                              <div>
                                <p className="text-xs font-medium text-muted-foreground mb-1">
                                  Before
                                </p>
                                <pre className="text-xs bg-background p-3 rounded-md overflow-x-auto">
                                  {JSON.stringify(log.changes.before, null, 2)}
                                </pre>
                              </div>
                              <div>
                                <p className="text-xs font-medium text-muted-foreground mb-1">
                                  After
                                </p>
                                <pre className="text-xs bg-background p-3 rounded-md overflow-x-auto">
                                  {JSON.stringify(log.changes.after, null, 2)}
                                </pre>
                              </div>
                            </div>
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
