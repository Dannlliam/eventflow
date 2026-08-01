"use client";

import { useQuery } from "@apollo/client/react";
import { useMemo } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { TrendingUp, Send, CheckCircle, Clock, AlertCircle } from "lucide-react";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from "recharts";
import { GET_ANALYTICS } from "@/lib/graphql/queries";
import { LoadingKPI, LoadingCard, LoadingSpinner } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Dashboard Overview Page
 * Per PRD Part 10 - UI Section 1 (Dashboard Overview)
 * 
 * Features:
 * - 4 KPI cards (Total Sent, Delivery Rate, Avg Latency, DLQ Count)
 * - Volume chart (stacked area chart by channel)
 * - Recent activity feed
 */
export default function DashboardPage() {
  // Memoize date range to prevent infinite refetching
  const dateRange = useMemo(() => {
    const endDate = new Date();
    const startDate = new Date(endDate.getTime() - 30 * 24 * 60 * 60 * 1000);
    return {
      startDate: startDate.toISOString().split('T')[0], // YYYY-MM-DD format
      endDate: endDate.toISOString().split('T')[0],
    };
  }, []); // Empty dependency array means this only runs once

  const { data, loading, error, refetch } = useQuery(GET_ANALYTICS, {
    variables: dateRange,
    // Disable polling to reduce server load - can be re-enabled once backend has real data
    // pollInterval: 60000,
  });

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
          <p className="text-muted-foreground">
            Overview of your notification delivery performance
          </p>
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <LoadingKPI key={i} />
          ))}
        </div>
        <LoadingCard />
        <LoadingCard />
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
          <p className="text-muted-foreground">
            Overview of your notification delivery performance
          </p>
        </div>
        <ErrorState 
          title="Failed to load dashboard data"
          message={error.message}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  const analytics = (data as any)?.analytics;
  const kpiData = {
    totalSent: analytics?.totalSent || 0,
    deliveryRate: analytics?.deliveryRate || 0,
    avgLatency: analytics?.avgProcessingLatency || 0,
    dlqCount: analytics?.dlqCount || 0,
  };

  // Transform daily stats for chart
  const volumeData = analytics?.dailyStats?.map((stat: any) => ({
    date: new Date(stat.date).toLocaleDateString('en-US', { month: 'short', day: 'numeric' }),
    EMAIL: stat.sent || 0,
    SMS: 0, // TODO: Backend should provide per-channel data
    PUSH: 0,
    WEBHOOK: 0,
  })) || [];

  // Mock recent activity - TODO: Add to backend
  const recentActivity = [
    {
      id: "1",
      type: "EMAIL",
      recipient: "user@example.com",
      status: "DELIVERED",
      timestamp: new Date().toISOString(),
    },
    {
      id: "2",
      type: "SMS",
      recipient: "+1234567890",
      status: "SENT",
      timestamp: new Date().toISOString(),
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Dashboard</h2>
        <p className="text-muted-foreground">
          Overview of your notification delivery performance
        </p>
      </div>

      {/* KPI Cards */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Sent</CardTitle>
            <Send className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{kpiData.totalSent.toLocaleString()}</div>
            <p className="text-xs text-muted-foreground">
              <TrendingUp className="inline h-3 w-3 mr-1" />
              +12% from last week
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Delivery Rate</CardTitle>
            <CheckCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{kpiData.deliveryRate}%</div>
            <p className="text-xs text-muted-foreground">
              <TrendingUp className="inline h-3 w-3 mr-1" />
              +0.5% from last week
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Avg Latency</CardTitle>
            <Clock className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{kpiData.avgLatency}ms</div>
            <p className="text-xs text-muted-foreground">
              -15ms from last week
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">DLQ Count</CardTitle>
            <AlertCircle className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{kpiData.dlqCount}</div>
            <p className="text-xs text-muted-foreground">
              -8 from last week
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Volume Chart */}
      <Card>
        <CardHeader>
          <CardTitle>Notification Volume (Last 30 Days)</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="h-[300px]">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={volumeData}>
                <defs>
                  <linearGradient id="colorEMAIL" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorSMS" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorPUSH" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#f59e0b" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#f59e0b" stopOpacity={0}/>
                  </linearGradient>
                  <linearGradient id="colorWEBHOOK" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.8}/>
                    <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0}/>
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" className="stroke-muted" />
                <XAxis 
                  dataKey="date" 
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
                <Area 
                  type="monotone" 
                  dataKey="EMAIL" 
                  stackId="1"
                  stroke="#6366f1" 
                  fillOpacity={1} 
                  fill="url(#colorEMAIL)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="SMS" 
                  stackId="1"
                  stroke="#10b981" 
                  fillOpacity={1} 
                  fill="url(#colorSMS)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="PUSH" 
                  stackId="1"
                  stroke="#f59e0b" 
                  fillOpacity={1} 
                  fill="url(#colorPUSH)" 
                />
                <Area 
                  type="monotone" 
                  dataKey="WEBHOOK" 
                  stackId="1"
                  stroke="#8b5cf6" 
                  fillOpacity={1} 
                  fill="url(#colorWEBHOOK)" 
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </CardContent>
      </Card>

      {/* Recent Activity */}
      <Card>
        <CardHeader>
          <CardTitle>Recent Activity</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {recentActivity.map((activity) => (
              <div key={activity.id} className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="h-2 w-2 rounded-full bg-primary" />
                  <div>
                    <p className="text-sm font-medium">{activity.type}</p>
                    <p className="text-xs text-muted-foreground">{activity.recipient}</p>
                  </div>
                </div>
                <div className="text-right">
                  <p className="text-sm">{activity.status}</p>
                  <p className="text-xs text-muted-foreground">Just now</p>
                </div>
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
