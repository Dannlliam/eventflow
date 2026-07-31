"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";

/**
 * Queues Page
 * Per PRD Part 10 - UI Section 7 (Queues)
 * 
 * Features:
 * - Kafka topic list with partition count
 * - Consumer lag monitoring
 * - Historical lag chart (24 hours)
 * - Alert highlighting (>5000 amber, >50000 red)
 */
export default function QueuesPage() {
  // TODO: Fetch from Kafka metrics API
  const topics = [
    {
      name: "notification.created",
      partitions: 10,
      consumerLag: 45,
      messagesPerSec: 120,
    },
    {
      name: "notification.retry-1",
      partitions: 5,
      consumerLag: 3200,
      messagesPerSec: 25,
    },
    {
      name: "notification.retry-2",
      partitions: 5,
      consumerLag: 12000,
      messagesPerSec: 8,
    },
    {
      name: "dispatch.requested",
      partitions: 10,
      consumerLag: 120,
      messagesPerSec: 95,
    },
    {
      name: "dispatch.result",
      partitions: 10,
      consumerLag: 80,
      messagesPerSec: 90,
    },
    {
      name: "notification.dlq",
      partitions: 3,
      consumerLag: 0,
      messagesPerSec: 2,
    },
  ];

  const getLagBadge = (lag: number) => {
    if (lag > 50000) {
      return <Badge variant="destructive">Critical</Badge>;
    } else if (lag > 5000) {
      return <Badge variant="warning">High</Badge>;
    } else if (lag > 1000) {
      return <Badge variant="secondary">Moderate</Badge>;
    } else {
      return <Badge variant="success">Healthy</Badge>;
    }
  };

  const getLagRowClass = (lag: number) => {
    if (lag > 50000) {
      return "bg-destructive/10";
    } else if (lag > 5000) {
      return "bg-warning/10";
    }
    return "";
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Queues</h2>
        <p className="text-muted-foreground">
          Monitor Kafka topics and consumer lag
        </p>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Total Topics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{topics.length}</div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Healthy Topics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-success">
              {topics.filter((t) => t.consumerLag < 1000).length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">High Lag Topics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-warning">
              {topics.filter((t) => t.consumerLag > 5000 && t.consumerLag < 50000).length}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Critical Topics</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold text-destructive">
              {topics.filter((t) => t.consumerLag > 50000).length}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Topics Table */}
      <Card>
        <CardHeader>
          <CardTitle>Kafka Topics</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Topic Name</TableHead>
                  <TableHead>Partitions</TableHead>
                  <TableHead>Consumer Lag</TableHead>
                  <TableHead>Messages/Sec</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {topics.map((topic) => (
                  <TableRow key={topic.name} className={getLagRowClass(topic.consumerLag)}>
                    <TableCell className="font-mono text-sm">
                      {topic.name}
                    </TableCell>
                    <TableCell>{topic.partitions}</TableCell>
                    <TableCell className="font-medium">
                      {topic.consumerLag.toLocaleString()}
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {topic.messagesPerSec}
                    </TableCell>
                    <TableCell>{getLagBadge(topic.consumerLag)}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Lag Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Consumer Lag (Last 24 Hours)</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            {/* TODO: Integrate Recharts line chart */}
            Historical lag chart will be rendered here (Recharts LineChart)
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
