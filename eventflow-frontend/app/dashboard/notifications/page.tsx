"use client";

import { useState } from "react";
import { useQuery } from "@apollo/client/react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { StatusBadge } from "@/components/shared/status-badge";
import { Search, Filter } from "lucide-react";
import { NotificationStatus } from "@/types";
import { LIST_NOTIFICATIONS } from "@/lib/graphql/queries";
import { LoadingTable, LoadingSpinner } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Notifications Page
 * Per PRD Part 10 - UI Section 6 (Notifications)
 * 
 * Features:
 * - Filter bar (status, channel, recipient, date range)
 * - Data table with cursor pagination
 * - Detail drawer (slides in from right, 60% width)
 * - Notification timeline
 */
export default function NotificationsPage() {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedNotification, setSelectedNotification] = useState<string | null>(null);
  const [statusFilter, setStatusFilter] = useState<string | null>(null);

  const { data, loading, error, refetch, fetchMore } = useQuery(LIST_NOTIFICATIONS, {
    variables: {
      filter: statusFilter ? { status: statusFilter } : undefined,
      first: 20,
    },
    // Disable polling to reduce server load
    // pollInterval: 30000,
  });

  if (loading && !data) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Notifications</h2>
          <p className="text-muted-foreground">
            View and manage all notification deliveries
          </p>
        </div>
        <Card>
          <CardHeader>
            <CardTitle>All Notifications</CardTitle>
          </CardHeader>
          <CardContent>
            <LoadingTable rows={10} />
          </CardContent>
        </Card>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Notifications</h2>
          <p className="text-muted-foreground">
            View and manage all notification deliveries
          </p>
        </div>
        <ErrorState 
          title="Failed to load notifications"
          message={error.message}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  const notifications = (data as any)?.notifications?.edges?.map((edge: any) => edge.node) || [];
  const pageInfo = (data as any)?.notifications?.pageInfo;

  const filteredNotifications = notifications.filter((n: any) => {
    if (!searchTerm) return true;
    const search = searchTerm.toLowerCase();
    return (
      n.id.toLowerCase().includes(search) ||
      n.recipient.email?.toLowerCase().includes(search) ||
      n.recipient.phone?.includes(search)
    );
  });

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Notifications</h2>
        <p className="text-muted-foreground">
          View and manage all notification deliveries
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>All Notifications</CardTitle>
        </CardHeader>
        <CardContent>
          {/* Filter Bar */}
          <div className="flex flex-col sm:flex-row items-start sm:items-center gap-4 mb-6">
            <div className="flex-1 w-full relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by recipient or event ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <select
              value={statusFilter || ""}
              onChange={(e) => setStatusFilter(e.target.value || null)}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Statuses</option>
              <option value="QUEUED">Queued</option>
              <option value="PROCESSING">Processing</option>
              <option value="SENT">Sent</option>
              <option value="DELIVERED">Delivered</option>
              <option value="FAILED">Failed</option>
              <option value="RETRY_SCHEDULED">Retry Scheduled</option>
            </select>
          </div>

          {/* Data Table */}
          <div className="rounded-md border overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="whitespace-nowrap">Event ID</TableHead>
                  <TableHead className="whitespace-nowrap">Channel</TableHead>
                  <TableHead className="whitespace-nowrap">Recipient</TableHead>
                  <TableHead className="whitespace-nowrap">Status</TableHead>
                  <TableHead className="whitespace-nowrap">Attempts</TableHead>
                  <TableHead className="whitespace-nowrap">Created At</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredNotifications.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground">
                      No notifications found
                    </TableCell>
                  </TableRow>
                ) : (
                  filteredNotifications.map((notification: any) => (
                    <TableRow
                      key={notification.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => setSelectedNotification(notification.id)}
                    >
                      <TableCell className="font-mono text-sm whitespace-nowrap">
                        {notification.id}
                      </TableCell>
                      <TableCell>
                        <span className="font-medium">{notification.channel}</span>
                      </TableCell>
                      <TableCell className="text-muted-foreground max-w-[200px] truncate">
                        {notification.recipient.email || notification.recipient.phone || notification.recipient.deviceToken}
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={notification.status} />
                      </TableCell>
                      <TableCell>{notification.attemptCount}</TableCell>
                      <TableCell className="text-muted-foreground whitespace-nowrap">
                        {new Date(notification.createdAt).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="flex flex-col sm:flex-row items-center justify-between mt-4 gap-4">
            <p className="text-sm text-muted-foreground">
              Showing {filteredNotifications.length} notifications
            </p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled>
                Previous
              </Button>
              <Button 
                variant="outline" 
                size="sm"
                disabled={!pageInfo?.hasNextPage || loading}
                onClick={() => {
                  if (pageInfo?.endCursor) {
                    fetchMore({
                      variables: {
                        after: pageInfo.endCursor,
                      },
                    });
                  }
                }}
              >
                {loading ? "Loading..." : "Next"}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Detail Drawer - TODO: Implement slide-in drawer */}
      {selectedNotification && (
        <div className="fixed right-0 top-0 h-screen w-3/5 bg-card border-l shadow-lg p-6 overflow-y-auto">
          <div className="flex items-center justify-between mb-6">
            <h3 className="text-lg font-semibold">Notification Details</h3>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setSelectedNotification(null)}
            >
              Close
            </Button>
          </div>
          <div className="space-y-4">
            <div>
              <p className="text-sm font-medium">Event ID</p>
              <p className="text-sm text-muted-foreground font-mono">{selectedNotification}</p>
            </div>
            {/* TODO: Add full notification details and timeline */}
          </div>
        </div>
      )}
    </div>
  );
}
