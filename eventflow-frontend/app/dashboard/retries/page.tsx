"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { AlertCircle, X } from "lucide-react";

/**
 * Retries Page
 * Per PRD Part 10 - UI Section 8 (Retries)
 * 
 * Features:
 * - Table with notification ID, channel, provider, attempt count, next retry
 * - Error trace modal
 * - Cancel retry action
 */
export default function RetriesPage() {
  // TODO: Fetch from GraphQL
  const retries = [
    {
      id: "evt_retry_001",
      channel: "SMS",
      provider: "Twilio",
      attemptCount: 2,
      maxAttempts: 5,
      nextRetryAt: new Date(Date.now() + 300000).toISOString(),
      errorTrace: "TwilioException: Rate limit exceeded (429)",
      recipient: "+1234567890",
    },
    {
      id: "evt_retry_002",
      channel: "EMAIL",
      provider: "SendGrid",
      attemptCount: 1,
      maxAttempts: 5,
      nextRetryAt: new Date(Date.now() + 60000).toISOString(),
      errorTrace: "SendGridException: Temporary server error (503)",
      recipient: "user@example.com",
    },
  ];

  const handleCancelRetry = (eventId: string) => {
    if (confirm(`Cancel retry for ${eventId}? The notification will be moved to the DLQ.`)) {
      console.log("Cancelling retry:", eventId);
      // TODO: Call cancelRetry mutation
    }
  };

  const handleViewError = (eventId: string, errorTrace: string) => {
    alert(`Error Trace for ${eventId}:\n\n${errorTrace}`);
    // TODO: Implement proper modal with formatted error trace
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Retries</h2>
        <p className="text-muted-foreground">
          Monitor and manage notifications scheduled for retry
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Scheduled Retries</CardTitle>
        </CardHeader>
        <CardContent>
          {retries.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12">
              <AlertCircle className="h-12 w-12 text-muted-foreground mb-4" />
              <h3 className="text-lg font-semibold mb-2">No retries scheduled</h3>
              <p className="text-sm text-muted-foreground">
                All notifications are being processed successfully
              </p>
            </div>
          ) : (
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Event ID</TableHead>
                    <TableHead>Channel</TableHead>
                    <TableHead>Provider</TableHead>
                    <TableHead>Recipient</TableHead>
                    <TableHead>Attempts</TableHead>
                    <TableHead>Next Retry</TableHead>
                    <TableHead className="text-right">Actions</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {retries.map((retry) => (
                    <TableRow key={retry.id}>
                      <TableCell className="font-mono text-sm">
                        {retry.id}
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">{retry.channel}</Badge>
                      </TableCell>
                      <TableCell>{retry.provider}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {retry.recipient}
                      </TableCell>
                      <TableCell>
                        <span className="font-medium">
                          {retry.attemptCount}/{retry.maxAttempts}
                        </span>
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(retry.nextRetryAt).toLocaleString()}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleViewError(retry.id, retry.errorTrace)}
                          >
                            View Error
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={() => handleCancelRetry(retry.id)}
                          >
                            <X className="h-4 w-4 mr-1" />
                            Cancel
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Retry Statistics */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Active Retries</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{retries.length}</div>
            <p className="text-xs text-muted-foreground">notifications pending retry</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Avg Retry Time</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">5m</div>
            <p className="text-xs text-muted-foreground">until next attempt</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Success Rate</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">87%</div>
            <p className="text-xs text-muted-foreground">retries eventually succeed</p>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
