"use client";

import { useState } from "react";
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
import { AlertCircle, RotateCw, Trash2, CheckSquare, Square } from "lucide-react";

/**
 * Dead Letter Queue (DLQ) Page
 * Per PRD Part 10 - UI Section 9 (Dead Letter Queue)
 * 
 * Features:
 * - DLQ table (event ID, topic, failure reason, failed at)
 * - Detail modal with JSON payload viewer
 * - Replay action with confirmation
 * - Discard action with eventId confirmation input
 * - Batch replay for multiple messages
 */
export default function DlqPage() {
  const [selectedMessage, setSelectedMessage] = useState<string | null>(null);
  const [selectedMessages, setSelectedMessages] = useState<Set<string>>(new Set());

  // TODO: Fetch DLQ messages from GraphQL
  const dlqMessages = [
    {
      id: "evt_failed_001",
      originalTopic: "notification.created",
      failureReason: "Provider returned 500: Internal Server Error",
      failedAt: new Date().toISOString(),
      attemptCount: 5,
    },
    {
      id: "evt_failed_002",
      originalTopic: "notification.retry-3",
      failureReason: "Invalid phone number format",
      failedAt: new Date().toISOString(),
      attemptCount: 3,
    },
  ];

  const toggleMessageSelection = (eventId: string) => {
    const newSelection = new Set(selectedMessages);
    if (newSelection.has(eventId)) {
      newSelection.delete(eventId);
    } else {
      newSelection.add(eventId);
    }
    setSelectedMessages(newSelection);
  };

  const toggleSelectAll = () => {
    if (selectedMessages.size === dlqMessages.length) {
      setSelectedMessages(new Set());
    } else {
      setSelectedMessages(new Set(dlqMessages.map(m => m.id)));
    }
  };

  const handleReplay = async (eventId: string) => {
    if (confirm(`Are you sure you want to replay message ${eventId}? This will re-queue it for processing.`)) {
      // TODO: Call replayDlqMessage GraphQL mutation
      console.log("Replaying message:", eventId);
    }
  };

  const handleBatchReplay = async () => {
    const count = selectedMessages.size;
    if (count === 0) return;
    
    if (confirm(`Are you sure you want to replay ${count} message(s)? This will re-queue them for processing.`)) {
      // TODO: Call replayDlqBatch GraphQL mutation
      console.log("Replaying messages:", Array.from(selectedMessages));
      setSelectedMessages(new Set());
    }
  };

  const handleDiscard = async (eventId: string) => {
    const confirmText = prompt(
      `This will permanently delete the message from the DLQ. This action cannot be undone.\n\nType the event ID "${eventId}" to confirm:`
    );
    
    if (confirmText === eventId) {
      // TODO: Call discardDlqMessage mutation
      console.log("Discarding message:", eventId);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Dead Letter Queue</h2>
          <p className="text-muted-foreground">
            Manage failed messages that exhausted all retry attempts
          </p>
        </div>
        {selectedMessages.size > 0 && (
          <Button onClick={handleBatchReplay}>
            <RotateCw className="h-4 w-4 mr-2" />
            Replay Selected ({selectedMessages.size})
          </Button>
        )}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Failed Messages</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-12">
                    <button onClick={toggleSelectAll} className="p-1">
                      {selectedMessages.size === dlqMessages.length ? (
                        <CheckSquare className="h-4 w-4" />
                      ) : (
                        <Square className="h-4 w-4" />
                      )}
                    </button>
                  </TableHead>
                  <TableHead>Event ID</TableHead>
                  <TableHead>Original Topic</TableHead>
                  <TableHead>Failure Reason</TableHead>
                  <TableHead>Attempts</TableHead>
                  <TableHead>Failed At</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {dlqMessages.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={7} className="text-center py-12">
                      <div className="flex flex-col items-center gap-2">
                        <AlertCircle className="h-12 w-12 text-muted-foreground" />
                        <h3 className="text-lg font-semibold">No messages in DLQ</h3>
                        <p className="text-sm text-muted-foreground max-w-sm">
                          All messages are being processed successfully. Failed messages will appear here.
                        </p>
                      </div>
                    </TableCell>
                  </TableRow>
                ) : (
                  dlqMessages.map((message) => (
                    <TableRow
                      key={message.id}
                      className="cursor-pointer"
                      onClick={() => setSelectedMessage(message.id)}
                    >
                      <TableCell onClick={(e) => e.stopPropagation()}>
                        <button
                          onClick={() => toggleMessageSelection(message.id)}
                          className="p-1"
                        >
                          {selectedMessages.has(message.id) ? (
                            <CheckSquare className="h-4 w-4" />
                          ) : (
                            <Square className="h-4 w-4" />
                          )}
                        </button>
                      </TableCell>
                      <TableCell className="font-mono text-sm">
                        {message.id}
                      </TableCell>
                      <TableCell>{message.originalTopic}</TableCell>
                      <TableCell className="max-w-md truncate text-muted-foreground">
                        {message.failureReason}
                      </TableCell>
                      <TableCell>{message.attemptCount}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(message.failedAt).toLocaleString()}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-2">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleReplay(message.id);
                            }}
                          >
                            <RotateCw className="h-4 w-4 mr-1" />
                            Replay
                          </Button>
                          <Button
                            variant="destructive"
                            size="sm"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleDiscard(message.id);
                            }}
                          >
                            <Trash2 className="h-4 w-4 mr-1" />
                            Discard
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Detail Modal - TODO: Implement proper modal with JSON viewer */}
      {selectedMessage && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-2xl max-h-[80vh] overflow-y-auto">
            <CardHeader>
              <CardTitle>Message Details</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div>
                  <p className="text-sm font-medium">Event ID</p>
                  <p className="text-sm text-muted-foreground font-mono">{selectedMessage}</p>
                </div>
                <div>
                  <p className="text-sm font-medium">Original Payload</p>
                  <pre className="text-xs bg-muted p-4 rounded-md overflow-x-auto">
                    {JSON.stringify({ eventId: selectedMessage, data: "..." }, null, 2)}
                  </pre>
                </div>
                <Button onClick={() => setSelectedMessage(null)} variant="outline" className="w-full">
                  Close
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
