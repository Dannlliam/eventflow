"use client";

import { useState } from "react";
import { useQuery } from "@apollo/client/react";
import { useMutation } from "@apollo/client/react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Plus, Copy, Trash2 } from "lucide-react";
import { GET_WORKSPACE_CONFIG } from "@/lib/graphql/queries";
import { GENERATE_API_KEY, DEACTIVATE_API_KEY } from "@/lib/graphql/mutations";
import { LoadingCard, LoadingTable } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Settings Page
 * Per PRD Part 10 - UI Section 11 (Settings)
 * 
 * Features:
 * - Workspace configuration (name, timezone)
 * - API keys table (masked keys)
 * - Generate new key with one-time display modal
 * - Webhook configuration
 */
export default function SettingsPage() {
  const [showGenerateKeyDialog, setShowGenerateKeyDialog] = useState(false);
  const [generatedKey, setGeneratedKey] = useState<string | null>(null);
  const [keyDescription, setKeyDescription] = useState("");

  const { loading, error, data, refetch } = useQuery(GET_WORKSPACE_CONFIG);
  const [generateApiKey] = useMutation(GENERATE_API_KEY);
  const [deactivateApiKey] = useMutation(DEACTIVATE_API_KEY);

  if (loading) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
          <p className="text-muted-foreground">
            Manage workspace configuration and API access
          </p>
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
          <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
          <p className="text-muted-foreground">
            Manage workspace configuration and API access
          </p>
        </div>
        <ErrorState message={error.message} onRetry={() => refetch()} />
      </div>
    );
  }

  const workspaceConfig = (data as any)?.workspaceConfig || {};
  const apiKeys = workspaceConfig.apiKeys || [];

  const handleGenerateKey = async () => {
    if (!keyDescription.trim()) {
      alert("Please enter a description for the API key");
      return;
    }

    try {
      const result = await generateApiKey({
        variables: { description: keyDescription }
      });
      
      if ((result.data as any)?.generateApiKey) {
        setGeneratedKey((result.data as any).generateApiKey.fullKey);
        setKeyDescription("");
        refetch();
      }
    } catch (err) {
      console.error("Failed to generate API key:", err);
      alert("Failed to generate API key. Please try again.");
    }
  };

  const handleCopyKey = () => {
    if (generatedKey) {
      navigator.clipboard.writeText(generatedKey);
      alert("API key copied to clipboard!");
    }
  };

  const handleRevokeKey = async (keyId: string, keyPrefix: string) => {
    if (confirm(`Are you sure you want to revoke API key ${keyPrefix}***? This action cannot be undone.`)) {
      try {
        await deactivateApiKey({
          variables: { keyId }
        });
        refetch();
      } catch (err) {
        console.error("Failed to revoke API key:", err);
        alert("Failed to revoke API key. Please try again.");
      }
    }
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-3xl font-bold tracking-tight">Settings</h2>
        <p className="text-muted-foreground">
          Manage workspace configuration and API access
        </p>
      </div>

      {/* Workspace Configuration */}
      <Card>
        <CardHeader>
          <CardTitle>Workspace Configuration</CardTitle>
          <CardDescription>General settings for your EventFlow workspace</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">Workspace Name</label>
            <Input defaultValue={workspaceConfig.name || "My EventFlow Workspace"} />
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Default Timezone</label>
            <select className="w-full h-10 rounded-md border border-input bg-background px-3 py-2 text-sm">
              <option value="UTC">UTC</option>
              <option value="America/New_York">America/New York</option>
              <option value="America/Los_Angeles">America/Los Angeles</option>
              <option value="Europe/London">Europe/London</option>
              <option value="Asia/Tokyo">Asia/Tokyo</option>
            </select>
          </div>
          <Button onClick={() => alert("Update workspace mutation not yet implemented")}>
            Save Changes
          </Button>
        </CardContent>
      </Card>

      {/* API Keys */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle>API Keys</CardTitle>
              <CardDescription>
                Manage API keys for programmatic access
              </CardDescription>
            </div>
            <Button onClick={() => setShowGenerateKeyDialog(true)}>
              <Plus className="mr-2 h-4 w-4" />
              Generate New Key
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Key Prefix</TableHead>
                  <TableHead>Description</TableHead>
                  <TableHead>Last Used</TableHead>
                  <TableHead>Created</TableHead>
                  <TableHead className="text-right">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {apiKeys.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                      No API keys yet. Generate one to get started.
                    </TableCell>
                  </TableRow>
                ) : (
                  apiKeys.map((key: any) => (
                    <TableRow key={key.id}>
                      <TableCell className="font-mono text-sm">
                        {key.keyPrefix}***
                      </TableCell>
                      <TableCell>{key.description}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {key.lastUsedAt ? new Date(key.lastUsedAt).toLocaleDateString() : "Never"}
                      </TableCell>
                      <TableCell className="text-muted-foreground">
                        {new Date(key.createdAt).toLocaleDateString()}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button
                          variant="destructive"
                          size="sm"
                          onClick={() => handleRevokeKey(key.id, key.keyPrefix)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>

      {/* Webhook Configuration */}
      <Card>
        <CardHeader>
          <CardTitle>Webhook Configuration</CardTitle>
          <CardDescription>
            Configure webhook URLs for provider callbacks (e.g., SendGrid bounce webhook)
          </CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <label className="text-sm font-medium">SendGrid Webhook URL</label>
            <Input 
              value="https://api.eventflow.com/webhooks/sendgrid" 
              readOnly 
            />
            <p className="text-xs text-muted-foreground">
              Configure this URL in your SendGrid account for bounce notifications
            </p>
          </div>
          <div className="space-y-2">
            <label className="text-sm font-medium">Twilio Status Callback URL</label>
            <Input 
              value="https://api.eventflow.com/webhooks/twilio" 
              readOnly 
            />
            <p className="text-xs text-muted-foreground">
              Configure this URL in your Twilio account for delivery status updates
            </p>
          </div>
        </CardContent>
      </Card>

      {/* Generate API Key Dialog */}
      {showGenerateKeyDialog && !generatedKey && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>Generate New API Key</CardTitle>
              <CardDescription>
                Create a new API key for programmatic access
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Description</label>
                <Input 
                  placeholder="e.g., Production API Key" 
                  value={keyDescription}
                  onChange={(e) => setKeyDescription(e.target.value)}
                />
              </div>
              <div className="flex gap-2">
                <Button className="flex-1" onClick={handleGenerateKey}>
                  Generate Key
                </Button>
                <Button 
                  variant="outline" 
                  onClick={() => {
                    setShowGenerateKeyDialog(false);
                    setKeyDescription("");
                  }}
                >
                  Cancel
                </Button>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Display Generated Key (One-Time) */}
      {generatedKey && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <Card className="w-full max-w-md">
            <CardHeader>
              <CardTitle>API Key Generated</CardTitle>
              <CardDescription className="text-destructive">
                ⚠️ Save this key now! It will not be shown again.
              </CardDescription>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <label className="text-sm font-medium">Your API Key</label>
                <div className="flex gap-2">
                  <Input 
                    value={generatedKey} 
                    readOnly 
                    className="font-mono text-sm"
                  />
                  <Button size="icon" onClick={handleCopyKey}>
                    <Copy className="h-4 w-4" />
                  </Button>
                </div>
              </div>
              <div className="bg-warning/10 border border-warning rounded-md p-3">
                <p className="text-sm text-warning-foreground">
                  Store this key securely. You won't be able to see it again after closing this dialog.
                </p>
              </div>
              <Button 
                className="w-full" 
                onClick={() => {
                  setGeneratedKey(null);
                  setShowGenerateKeyDialog(false);
                }}
              >
                I've Saved My Key
              </Button>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
