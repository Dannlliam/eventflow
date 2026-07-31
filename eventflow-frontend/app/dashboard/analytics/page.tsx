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
import { Download, Calendar } from "lucide-react";

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

  // TODO: Fetch from GraphQL analytics query
  const analyticsData = {
    totalSent: 125430,
    deliveryRate: 98.5,
    avgLatency: 245,
    channelBreakdown: [
      { channel: "EMAIL", count: 75000, successRate: 99.2 },
      { channel: "SMS", count: 35000, successRate: 98.0 },
      { channel: "PUSH", count: 12000, successRate: 97.5 },
      { channel: "WEBHOOK", count: 3430, successRate: 96.8 },
    ],
    providerLatency: [
      { provider: "SendGrid", p50: 120, p99: 450, successRate: 99.3 },
      { provider: "Twilio", p50: 200, p99: 580, successRate: 98.5 },
      { provider: "FCM", p50: 150, p99: 520, successRate: 97.8 },
    ],
  };

  const handleExportCSV = () => {
    // TODO: Implement CSV export
    console.log("Exporting analytics data to CSV...");
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
            <select className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
              <option value="">All Channels</option>
              <option value="EMAIL">Email</option>
              <option value="SMS">SMS</option>
              <option value="PUSH">Push</option>
              <option value="WEBHOOK">Webhook</option>
            </select>
            <select className="h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
              <option value="">All Providers</option>
              <option value="sendgrid">SendGrid</option>
              <option value="twilio">Twilio</option>
              <option value="fcm">FCM</option>
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
            <div className="text-2xl font-bold">{analyticsData.totalSent.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">in selected period</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Delivery Rate</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{analyticsData.deliveryRate}%</div>
            <p className="text-xs text-muted-foreground">successfully delivered</p>
          </CardContent>
        </Card>
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium">Avg Latency</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{analyticsData.avgLatency}ms</div>
            <p className="text-xs text-muted-foreground">end-to-end processing</p>
          </CardContent>
        </Card>
      </div>

      {/* Delivery Breakdown Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Delivery Breakdown by Status</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px] flex items-center justify-center text-muted-foreground">
            {/* TODO: Integrate Recharts donut chart */}
            Donut chart will be rendered here (Recharts PieChart)
          </div>
        </CardContent>
      </Card>

      {/* Channel Breakdown */}
      <Card>
        <CardHeader>
          <CardTitle>Channel Breakdown</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {analyticsData.channelBreakdown.map((channel) => (
              <div key={channel.channel} className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                  <div className="w-20 font-medium">{channel.channel}</div>
                  <div className="flex-1 h-2 bg-muted rounded-full overflow-hidden">
                    <div
                      className="h-full bg-primary"
                      style={{ width: `${channel.successRate}%` }}
                    />
                  </div>
                </div>
                <div className="flex items-center gap-4 text-sm">
                  <span className="text-muted-foreground">
                    {channel.count.toLocaleString()}
                  </span>
                  <span className="font-medium">{channel.successRate}%</span>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* Provider Latency Table */}
      <Card>
        <CardHeader>
          <CardTitle>Provider Latency & Performance</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Provider</TableHead>
                  <TableHead>p50 Latency</TableHead>
                  <TableHead>p99 Latency</TableHead>
                  <TableHead>Success Rate</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {analyticsData.providerLatency.map((provider) => (
                  <TableRow key={provider.provider}>
                    <TableCell className="font-medium">{provider.provider}</TableCell>
                    <TableCell>{provider.p50}ms</TableCell>
                    <TableCell>{provider.p99}ms</TableCell>
                    <TableCell>
                      <span className="font-medium text-success">
                        {provider.successRate}%
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
