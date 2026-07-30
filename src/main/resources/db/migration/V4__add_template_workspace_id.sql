-- Add workspace_id column to templates table for workspace-scoped templates
-- As specified in PRD Section 30 - Template Management / Workspace Isolation

ALTER TABLE templates ADD COLUMN IF NOT EXISTS workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';
ALTER TABLE template_versions ADD COLUMN IF NOT EXISTS workspace_id UUID NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000';

-- Create indexes for workspace-scoped queries
CREATE INDEX IF NOT EXISTS idx_templates_workspace_id ON templates(workspace_id);
CREATE INDEX IF NOT EXISTS idx_template_versions_workspace_id ON template_versions(workspace_id);

-- Create unique constraint for template slugs within a workspace
ALTER TABLE templates DROP CONSTRAINT IF EXISTS uq_template_slug_workspace;
ALTER TABLE templates ADD CONSTRAINT uq_template_slug_workspace UNIQUE (workspace_id, slug);

-- Add foreign key constraints (assuming a workspaces table exists or will be created)
-- These are commented out until the workspaces table migration is added
-- ALTER TABLE templates ADD CONSTRAINT fk_templates_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);
-- ALTER TABLE template_versions ADD CONSTRAINT fk_template_versions_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces(id);

COMMENT ON COLUMN templates.workspace_id IS 'Foreign key to the workspace that owns this template';
COMMENT ON COLUMN template_versions.workspace_id IS 'Foreign key to the workspace that owns this template version';
COMMENT ON INDEX idx_templates_workspace_id IS 'Optimizes workspace-scoped template queries';
COMMENT ON INDEX idx_template_versions_workspace_id IS 'Optimizes workspace-scoped template version queries';
COMMENT ON CONSTRAINT uq_template_slug_workspace ON templates IS 'Ensures unique template slugs within a workspace';