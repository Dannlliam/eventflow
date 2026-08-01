"use client";

import { useState } from "react";
import { useQuery } from "@apollo/client/react";
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
import { LIST_RETRIES } from "@/lib/graphql/queries";
import { LoadingTable } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

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
  const [errorModal, setErrorModal] = useState<{ id: string; message: string } | null>(null);

  const { loading, error, data, refetch } = useQuery(LIST_RETRIES, {
    // Disable polling to reduce server load
    // pollInterval: 30000,
  });

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Retries</h2>
          <p className="text-muted-foreground">
            Monitor and manage notifications scheduled for retry
          </p>
        </div>
        <LoadingTable />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Retries</h2>
          <p className="text-muted-foreground">
            Monitor and manage notifications scheduled for retry
          </p>
        </div>
        <ErrorState message={error.message} onRetry={() => refetch()} />
      </div>
    );
  }

  const retries = (data as any)?.retries || [];

  const handleCancelRetry = (eventId: string) => {
    if (confirm(`Cancel retry for ${eventId}? The notification will be moved to the DLQ.`)) {
      console.log("Cancelling retry:", eventId);
      alert("Cancel retry mutation not yet implemented in backend");
      // TODO: Call cancelRetry mutation when backend is ready
    }
  };

  const handleViewError = (eventId: string, errorMessage: string) => {
    setErrorModal({ id: eventId, message: errorMessage });
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
                  {retries.map((retry: any) => (
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
                            onClick={() => handleViewError(retry.id, retry.errorMessage)}
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
            <CardTitle className="text-sm font-medium">Avg Attempts</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {retries.length > 0 
                ? (retries.reduce((sum: number, r: any) => sum + r.attemptCount, 0) / retries.length).toFixed(1)
                : "0"}
            </div>
            <p className="text-xs text-muted-foreground">attempts per notification</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Max Attempts</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {retries.length > 0 ? retries[0].maxAttempts : 5}
            </div>
            <p className="text-xs text-muted-foreground">before moving to DLQ</p>
          </CardContent>
        </Card>
      </div>

      {/* Error Modal */}
      {errorModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[80vh] overflow-y-auto">
            <CardHeader>
              <CardTitle>Error Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-sm font-medium mb-2">Event ID</p>
                <p className="text-sm text-muted-foreground font-mono">{errorModal.id}</p>
              </div>
              <div>
                <p className="text-sm font-medium mb-2">Error Message</p>
                <pre className="text-sm bg-muted p-4 rounded-md overflow-x-auto whitespace-pre-wrap">
                  {errorModal.message}
                </pre>
              </div>
              <Button onClick={() => setErrorModal(null)} variant="outline" className="w-full">
                Close
              </Button>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
