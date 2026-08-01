"use client";

import { useState } from "react";
import { useQuery } from "@apollo/client/react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Plus, GripVertical, CheckCircle, XCircle, Zap } from "lucide-react";
import { LIST_PROVIDERS } from "@/lib/graphql/queries";
import { LoadingCard } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Providers Page
 * Per PRD Part 10 - UI Section 5 (Channels & Providers)
 * 
 * Features:
 * - Channel tabs (EMAIL, SMS, PUSH, WEBHOOK)
 * - Drag-and-drop provider priority list
 * - Add provider dialog
 * - Test connection functionality
 * - Provider credentials management
 */
export default function ProvidersPage() {
  const [selectedChannel, setSelectedChannel] = useState("EMAIL");
  const [showAddDialog, setShowAddDialog] = useState(false);

  const { data, loading, error, refetch } = useQuery(LIST_PROVIDERS, {
    variables: {
      channel: selectedChannel,
    },
  });

  const handleTestConnection = async (providerId: string) => {
    console.log("Testing connection for provider:", providerId);
    // TODO: Call test connection API
  };

  const handleAddProvider = () => {
    setShowAddDialog(true);
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">Providers</h2>
            <p className="text-muted-foreground">
              Configure third-party integrations and failover priorities
            </p>
          </div>
          <Button onClick={handleAddProvider}>
            <Plus className="mr-2 h-4 w-4" />
            Add Provider
          </Button>
        </div>
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <LoadingCard key={i} />
          ))}
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">Providers</h2>
            <p className="text-muted-foreground">
              Configure third-party integrations and failover priorities
            </p>
          </div>
        </div>
        <ErrorState 
          title="Failed to load providers"
          message={error.message}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  const channelProviders = (data as any)?.providers || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Providers</h2>
          <p className="text-muted-foreground">
            Configure third-party integrations and failover priorities
          </p>
        </div>
        <Button onClick={handleAddProvider}>
          <Plus className="mr-2 h-4 w-4" />
          Add Provider
        </Button>
      </div>

      {/* Channel Tabs */}
      <div className="flex gap-2 border-b">
        {["EMAIL", "SMS", "PUSH", "WEBHOOK"].map((channel) => (
          <button
            key={channel}
            onClick={() => setSelectedChannel(channel)}
            className={`px-4 py-2 text-sm font-medium border-b-2 transition-colors ${
              selectedChannel === channel
                ? "border-primary text-primary"
                : "border-transparent text-muted-foreground hover:text-foreground"
            }`}
          >
            {channel}
          </button>
        ))}
      </div>

      {/* Provider List */}
      {channelProviders.length === 0 ? (
        <Card className="flex flex-col items-center justify-center p-12">
          <Zap className="h-12 w-12 text-muted-foreground mb-4" />
          <h3 className="text-lg font-semibold mb-2">No providers configured</h3>
          <p className="text-muted-foreground text-center max-w-sm mb-4">
            Add your first {selectedChannel} provider to start sending notifications through this channel.
          </p>
          <Button onClick={handleAddProvider}>
            <Plus className="mr-2 h-4 w-4" />
            Add {selectedChannel} Provider
          </Button>
        </Card>
      ) : (
        <div className="space-y-3">
          {channelProviders.map((provider: any, index: number) => (
            <Card key={provider.id} className="hover:border-primary transition-colors">
              <CardHeader>
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <GripVertical className="h-5 w-5 text-muted-foreground cursor-move" />
                    <div>
                      <CardTitle className="text-base">{provider.name}</CardTitle>
                      <CardDescription>{provider.providerType}</CardDescription>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Badge variant={provider.enabled ? "success" : "secondary"}>
                      {provider.enabled ? "Enabled" : "Disabled"}
                    </Badge>
                    <span className="text-xs text-muted-foreground">
                      Priority #{provider.priority}
                    </span>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
                  <div className="flex flex-col sm:flex-row items-start sm:items-center gap-6 text-sm">
                    <div>
                      <span className="text-muted-foreground">Rate Limit: </span>
                      <span className="font-medium">{provider.rateLimit}/min</span>
                    </div>
                    <div>
                      <span className="text-muted-foreground">Updated: </span>
                      <span className="font-medium">
                        {new Date(provider.updatedAt).toLocaleDateString()}
                      </span>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleTestConnection(provider.id)}
                    >
                      <Zap className="mr-2 h-4 w-4" />
                      Test
                    </Button>
                    <Button variant="outline" size="sm">
                      Edit
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Add Provider Dialog - TODO: Implement proper modal */}
      {showAddDialog && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Add {selectedChannel} Provider</CardTitle>
              <CardDescription>Configure a new provider for {selectedChannel} notifications</CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Provider Type</label>
                <select className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
                  <option value="">Select provider...</option>
                  {selectedChannel === "EMAIL" && (
                    <>
                      <option value="SENDGRID">SendGrid</option>
                      <option value="SES">Amazon SES</option>
                      <option value="MAILGUN">Mailgun</option>
                    </>
                  )}
                  {selectedChannel === "SMS" && (
                    <>
                      <option value="TWILIO">Twilio</option>
                      <option value="SNS">Amazon SNS</option>
                      <option value="PLIVO">Plivo</option>
                    </>
                  )}
                  {selectedChannel === "PUSH" && (
                    <>
                      <option value="FCM">Firebase Cloud Messaging</option>
                      <option value="APNS">Apple Push Notification</option>
                    </>
                  )}
                </select>
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Provider Name</label>
                <Input placeholder="e.g., SendGrid Production" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">API Key</label>
                <Input type="password" placeholder="Enter API key" />
              </div>
              <div className="space-y-2">
                <label className="text-sm font-medium">Rate Limit (requests/minute)</label>
                <Input type="number" placeholder="1000" defaultValue="1000" />
              </div>
              <div className="flex gap-2">
                <Button className="flex-1">Save Provider</Button>
                <Button variant="outline" onClick={() => setShowAddDialog(false)}>
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
