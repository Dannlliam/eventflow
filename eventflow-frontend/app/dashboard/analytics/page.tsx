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
import { Download, Calendar } from "lucide-react";
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from "recharts";
import { GET_ANALYTICS } from "@/lib/graphql/queries";
import { LoadingCard, LoadingTable } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Analytics Page
 * Per PRD Part 10 - UI Section 2 (Analytics)
 * 
 * Features:
 * - Filter bar (date range, channel, provider)
 * - Delivery breakdown donut chart
 * - Provider latency table (p50/p99)
 * - Export CSV functionality
 * - 5-minute Redis cache
 */
export default function AnalyticsPage() {
  const [dateRange, setDateRange] = useState("last-7-days");
  const [selectedChannel, setSelectedChannel] = useState("");

  // Calculate date range
  const getDateRange = () => {
    const endDate = new Date();
    let startDate = new Date();
    
    switch (dateRange) {
      case "today":
        startDate = new Date();
        break;
      case "last-7-days":
        startDate.setDate(endDate.getDate() - 7);
        break;
      case "last-30-days":
        startDate.setDate(endDate.getDate() - 30);
        break;
      case "last-90-days":
        startDate.setDate(endDate.getDate() - 90);
        break;
      default:
        startDate.setDate(endDate.getDate() - 7);
    }
    
    return {
      startDate: startDate.toISOString().split('T')[0],
      endDate: endDate.toISOString().split('T')[0]
    };
  };

  const { startDate, endDate } = getDateRange();

  const { loading, error, data, refetch } = useQuery(GET_ANALYTICS, {
    variables: {
      startDate,
      endDate,
      channel: selectedChannel || undefined,
    },
    // Disable polling to reduce server load
    // pollInterval: 300000,
  });

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">Analytics</h2>
            <p className="text-muted-foreground">
              Deep-dive metrics and performance insights
            </p>
          </div>
        </div>
        <LoadingCard />
        <div className="grid gap-4 md:grid-cols-3">
          <LoadingCard />
          <LoadingCard />
          <LoadingCard />
        </div>
        <LoadingCard />
        <LoadingTable />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Analytics</h2>
          <p className="text-muted-foreground">
            Deep-dive metrics and performance insights
          </p>
        </div>
        <ErrorState message={error.message} onRetry={() => refetch()} />
      </div>
    );
  }

  const analyticsData = (data as any)?.analytics || {};
  const totalSent = analyticsData.totalSent || 0;
  const totalDelivered = analyticsData.totalDelivered || 0;
  const totalFailed = analyticsData.totalFailed || 0;
  const totalDlq = analyticsData.totalDlq || 0;
  const deliveryRate = analyticsData.deliveryRate || 0;

  // Data for donut chart
  const deliveryData = [
    { name: "Delivered", value: totalDelivered, color: "#10b981" },
    { name: "Failed", value: totalFailed, color: "#ef4444" },
    { name: "DLQ", value: totalDlq, color: "#f59e0b" },
  ].filter(item => item.value > 0);

  const COLORS = ["#10b981", "#ef4444", "#f59e0b"];

  const handleExportCSV = () => {
    const csv = [
      ["Metric", "Value"],
      ["Total Sent", totalSent],
      ["Total Delivered", totalDelivered],
      ["Total Failed", totalFailed],
      ["Delivery Rate", `${deliveryRate}%`],
      [""],
      ["Channel", "Count", "Percentage"],
      ...(analyticsData.channelBreakdown || []).map((ch: any) => [
        ch.channel,
        ch.count,
        `${ch.percentage}%`
      ])
    ].map(row => row.join(',')).join('\n');
    
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `analytics-${startDate}-to-${endDate}.csv`;
    a.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Analytics</h2>
          <p className="text-muted-foreground">
            Deep-dive metrics and performance insights
          </p>
        </div>
        <Button onClick={handleExportCSV}>
          <Download className="mr-2 h-4 w-4" />
          Export CSV
        </Button>
      </div>

      {/* Filter Bar */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2">
              <Calendar className="h-4 w-4 text-muted-foreground" />
              <select
                value={dateRange}
                onChange={(e) => setDateRange(e.target.value)}
                className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
              >
                <option value="today">Today</option>
                <option value="last-7-days">Last 7 Days</option>
                <option value="last-30-days">Last 30 Days</option>
                <option value="last-90-days">Last 90 Days</option>
                <option value="custom">Custom Range</option>
              </select>
            </div>
            <select 
              value={selectedChannel}
              onChange={(e) => setSelectedChannel(e.target.value)}
              className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm"
            >
              <option value="">All Channels</option>
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
              <option value="PUSH">Push</option>
              <option value="WEBHOOK">Webhook</option>
            </select>
          </div>
        </CardContent>
      </Card>

      {/* Summary Stats */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Total Sent</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalSent.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">in selected period</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Delivery Rate</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{deliveryRate.toFixed(1)}%</div>
            <p className="text-xs text-muted-foreground">successfully delivered</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Total Failed</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{totalFailed.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">failed deliveries</p>
          </CardContent>
        </Card>
      </div>

      {/* Delivery Breakdown Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Delivery Breakdown by Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={deliveryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={60}
                  outerRadius={100}
                  paddingAngle={5}
                  dataKey="value"
                  label={({ name, percent }) => `${name} ${((percent || 0) * 100).toFixed(0)}%`}
                >
                  {deliveryData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip 
                  contentStyle={{ 
                    backgroundColor: 'hsl(var(--card))', 
                    border: '1px solid hsl(var(--border))',
                    borderRadius: '6px'
                  }}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* Channel Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle>Channel Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          {(analyticsData.channelBreakdown || []).length === 0 ? (
            <div className="text-center py-8 text-muted-foreground">
              No channel data available for the selected period
            </div>
          ) : (
            <div className="space-y-4">
              {(analyticsData.channelBreakdown || []).map((channel: any) => (
                <div key={channel.channel} className="flex items-center justify-between">
                  <div className="flex items-center gap-4">
                    <div className="w-20 font-medium">{channel.channel}</div>
                    <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden min-w-[200px]">
                      <div
                        className="h-full bg-primary"
                        style={{ width: `${channel.percentage}%` }}
                      />
                    </div>
                  </div>
                  <div className="flex items-center gap-4 text-sm">
                    <span className="text-muted-foreground">
                      {channel.count.toLocaleString()}
                    </span>
                    <span className="font-medium">{channel.percentage.toFixed(1)}%</span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Top Errors */}
      {(analyticsData.topErrors || []).length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle>Top Error Messages</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="rounded-md border">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Error Message</TableHead>
                    <TableHead className="text-right">Count</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {(analyticsData.topErrors || []).slice(0, 5).map((error: any, index: number) => (
                    <TableRow key={index}>
                      <TableCell className="font-mono text-sm max-w-md truncate">
                        {error.errorMessage}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {error.count.toLocaleString()}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
