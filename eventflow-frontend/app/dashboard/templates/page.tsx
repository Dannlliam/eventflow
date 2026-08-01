"use client";

import { useQuery } from "@apollo/client/react";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, Mail, MessageSquare, Bell, Webhook, FileText } from "lucide-react";
import { LIST_TEMPLATES } from "@/lib/graphql/queries";
import { LoadingCard } from "@/components/shared/loading";
import { ErrorState } from "@/components/shared/error-state";

/**
 * Templates Page
 * Per PRD Part 10 - UI Section 4 (Templates)
 * 
 * Features:
 * - Template list view (card grid)
 * - Create new template button
 * - Monaco editor split-pane (editor left, preview right)
 * - Version history side drawer
 */
export default function TemplatesPage() {
  const { data, loading, error, refetch } = useQuery(LIST_TEMPLATES);

  const getChannelIcon = (channel: string) => {
    switch (channel) {
      case "EMAIL":
        return <Mail className="h-5 w-5" />;
      case "SMS":
        return <MessageSquare className="h-5 w-5" />;
      case "PUSH":
        return <Bell className="h-5 w-5" />;
      case "WEBHOOK":
        return <Webhook className="h-5 w-5" />;
      default:
        return <FileText className="h-5 w-5" />;
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-3xl font-bold tracking-tight">Templates</h2>
            <p className="text-muted-foreground">
              Create and manage notification templates
            </p>
          </div>
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            New Template
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, i) => (
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
            <h2 className="text-3xl font-bold tracking-tight">Templates</h2>
            <p className="text-muted-foreground">
              Create and manage notification templates
            </p>
          </div>
        </div>
        <ErrorState 
          title="Failed to load templates"
          message={error.message}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  const templates = (data as any)?.templates || [];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-3xl font-bold tracking-tight">Templates</h2>
          <p className="text-muted-foreground">
            Create and manage notification templates
          </p>
        </div>
        <Button>
          <Plus className="mr-2 h-4 w-4" />
          New Template
        </Button>
      </div>

      {/* Template Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {templates.map((template: any) => (
          <Card key={template.id} className="cursor-pointer hover:border-primary transition-colors">
            <CardHeader>
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  {getChannelIcon(template.channel)}
                  <CardTitle className="text-base">{template.slug}</CardTitle>
                </div>
                <span className="text-xs text-muted-foreground">
                  v{template.versions?.length || 0}
                </span>
              </div>
              <CardDescription>{template.description || "No description"}</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">{template.channel}</span>
                <span className="text-muted-foreground">
                  Updated {new Date(template.updatedAt).toLocaleDateString()}
                </span>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Empty State */}
      {templates.length === 0 && (
        <Card className="flex flex-col items-center justify-center p-12">
          <FileText className="h-12 w-12 text-muted-foreground mb-4" />
          <h3 className="text-lg font-semibold mb-2">No templates yet</h3>
          <p className="text-muted-foreground text-center max-w-sm mb-4">
            Get started by creating your first notification template. Templates help you maintain consistent messaging across all channels.
          </p>
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            Create Your First Template
          </Button>
        </Card>
      )}
    </div>
  );
}
