"use client";

import { useState, useEffect } from "react";
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
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { LoadingCard, LoadingTable } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

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
  const [topics, setTopics] = useState<any[]>([]);
  const [lagHistory, setLagHistory] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMetrics = async () => {
      try {
        setLoading(true);
        const [topicsRes, lagHistoryRes] = await Promise.all([
          fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/kafka/metrics/topics`),
          fetch(`${process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'}/api/v1/kafka/metrics/lag-history`)
        ]);

        if (!topicsRes.ok || !lagHistoryRes.ok) {
          throw new Error('Failed to fetch Kafka metrics');
        }

        const topicsData = await topicsRes.json();
        const lagHistoryData = await lagHistoryRes.json();

        setTopics(topicsData);
        setLagHistory(lagHistoryData);
        setError(null);
      } catch (err) {
        console.error('Error fetching Kafka metrics:', err);
        setError(err instanceof Error ? err.message : 'Failed to load Kafka metrics');
      } finally {
        setLoading(false);
      }
    };

    fetchMetrics();
    const interval = setInterval(fetchMetrics, 30000); // Refresh every 30 seconds

    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Queues</h2>
          <p className="text-muted-foreground">
            Monitor Kafka topics and consumer lag
          </p>
        </div>
        <div className="grid gap-4 md:grid-cols-4">
          <LoadingCard />
          <LoadingCard />
          <LoadingCard />
          <LoadingCard />
        </div>
        <LoadingTable />
        <LoadingCard />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Queues</h2>
          <p className="text-muted-foreground">
            Monitor Kafka topics and consumer lag
          </p>
        </div>
        <ErrorState 
          message={error} 
          onRetry={() => window.location.reload()} 
        />
      </div>
    );
  }

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
                      {topic.messagesPerSec.toFixed(1)}
                    </TableCell>
                    <TableCell>{getLagBadge(topic.consumerLag)}</TableCell>
                  </TableRow>
                ))}
                {topics.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center py-12">
                      <div className="flex flex-col items-center gap-2">
                        <h3 className="text-lg font-semibold">No Kafka topics found</h3>
                        <p className="text-sm text-muted-foreground max-w-sm">
                          Kafka metrics endpoint is not yet implemented. Topics will appear here once configured.
                        </p>
                      </div>
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Lag Chart */}
      {lagHistory.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Consumer Lag (Last 24 Hours)</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={lagHistory}>
                  <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                  <XAxis 
                    dataKey="time" 
                    className="text-xs"
                    tick={{ fill: 'hsl(var(--muted-foreground))' }}
                  />
                  <YAxis 
                    className="text-xs"
                    tick={{ fill: 'hsl(var(--muted-foreground))' }}
                  />
                  <Tooltip 
                    contentStyle={{ 
                      backgroundColor: 'hsl(var(--card))', 
                      border: '1px solid hsl(var(--border))',
                      borderRadius: '6px'
                    }}
                  />
                  <Legend />
                  <Line 
                    type="monotone" 
                    dataKey="created" 
                    stroke="#10b981" 
                    strokeWidth={2}
                    name="notification.created"
                    dot={false}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="retry1" 
                    stroke="#f59e0b" 
                    strokeWidth={2}
                    name="notification.retry-1"
                    dot={false}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="retry2" 
                    stroke="#ef4444" 
                    strokeWidth={2}
                    name="notification.retry-2"
                    dot={false}
                  />
                  <Line 
                    type="monotone" 
                    dataKey="dispatch" 
                    stroke="#6366f1" 
                    strokeWidth={2}
                    name="dispatch.requested"
                    dot={false}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
