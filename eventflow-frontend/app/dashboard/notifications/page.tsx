"use client";

import { useState } from "react";
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

  // TODO: Replace with real GraphQL data
  const mockNotifications = [
    {
      id: "evt_12345",
      channel: "EMAIL",
      recipient: "user@example.com",
      status: NotificationStatus.DELIVERED,
      attemptCount: 1,
      createdAt: new Date().toISOString(),
    },
    {
      id: "evt_12346",
      channel: "SMS",
      recipient: "+1234567890",
      status: NotificationStatus.SENT,
      attemptCount: 1,
      createdAt: new Date().toISOString(),
    },
    {
      id: "evt_12347",
      channel: "PUSH",
      recipient: "device-token-123",
      status: NotificationStatus.FAILED,
      attemptCount: 3,
      createdAt: new Date().toISOString(),
    },
  ];

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
          <div className="flex items-center gap-4 mb-6">
            <div className="flex-1 relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search by recipient or event ID..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Button variant="outline">
              <Filter className="mr-2 h-4 w-4" />
              Filters
            </Button>
          </div>

          {/* Data Table */}
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Event ID</TableHead>
                  <TableHead>Channel</TableHead>
                  <TableHead>Recipient</TableHead>
                  <TableHead>Status</TableHead>
                  <TableHead>Attempts</TableHead>
                  <TableHead>Created At</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {mockNotifications.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={6} className="text-center text-muted-foreground">
                      No notifications found
                    </TableCell>
                  </TableRow>
                ) : (
                  mockNotifications.map((notification) => (
                    <TableRow
                      key={notification.id}
                      className="cursor-pointer"
                      onClick={() => setSelectedNotification(notification.id)}
                    >
                      <TableCell className="font-mono text-sm">
                        {notification.id}
                      </TableCell>
                      <TableCell>
                        <span className="font-medium">{notification.channel}</span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {notification.recipient}
                      </TableCell>
                      <TableCell>
                        <StatusBadge status={notification.status} />
                      </TableCell>
                      <TableCell>{notification.attemptCount}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(notification.createdAt).toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* Pagination */}
          <div className="flex items-center justify-between mt-4">
            <p className="text-sm text-muted-foreground">
              Showing {mockNotifications.length} of {mockNotifications.length} notifications
            </p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" disabled>
                Previous
              </Button>
              <Button variant="outline" size="sm">
                Next
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
