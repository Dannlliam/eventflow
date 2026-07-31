/**
 * EventFlow TypeScript Type Definitions
 * Generated from backend GraphQL schema and domain models
 * Per PRD Part 10 - UI Requirements
 */

// ============================================================
// ENUMS
// ============================================================

export enum Channel {
  EMAIL = "EMAIL",
  SMS = "SMS",
  PUSH = "PUSH",
  WEBHOOK = "WEBHOOK",
}

export enum NotificationStatus {
  QUEUED = "QUEUED",
  PROCESSING = "PROCESSING",
  DISPATCHED = "DISPATCHED",
  SENT = "SENT",
  DELIVERED = "DELIVERED",
  FAILED = "FAILED",
  RETRY_SCHEDULED = "RETRY_SCHEDULED",
  BOUNCED = "BOUNCED",
}

export enum UserRole {
  WORKSPACE_ADMIN = "WORKSPACE_ADMIN",
  DEVELOPER = "DEVELOPER",
  ANALYST = "ANALYST",
}

// ============================================================
// NOTIFICATION TYPES
// ============================================================

export interface Notification {
  id: string;
  workspaceId: string;
  channel: Channel;
  recipient: RecipientInfo;
  templateSlug?: string | null;
  payload: Record<string, any>;
  metadata: Record<string, any>;
  status: NotificationStatus;
  attemptCount: number;
  nextRetryAt?: string | null;
  sentAt?: string | null;
  deliveredAt?: string | null;
  createdAt: string;
  updatedAt: string;
  events?: NotificationEvent[];
}

export interface RecipientInfo {
  email?: string | null;
  phone?: string | null;
  deviceToken?: string | null;
  webhookUrl?: string | null;
}

export interface NotificationEvent {
  id: string;
  notificationId: string;
  eventType: string;
  status: string;
  provider?: string | null;
  errorMessage?: string | null;
  metadata: Record<string, any>;
  createdAt: string;
}

export interface NotificationConnection {
  edges: NotificationEdge[];
  pageInfo: PageInfo;
  totalCount: number;
}

export interface NotificationEdge {
  node: Notification;
  cursor: string;
}

export interface PageInfo {
  hasNextPage: boolean;
  endCursor: string | null;
  totalCount: number;
}

// ============================================================
// TEMPLATE TYPES
// ============================================================

export interface Template {
  id: string;
  slug: string;
  channel: Channel;
  description: string;
  versions: TemplateVersion[];
  createdAt: string;
  updatedAt: string;
}

export interface TemplateVersion {
  id: string;
  templateSlug: string;
  version: number;
  subjectTemplate?: string | null;
  bodyTemplate: string;
  isActive: boolean;
  createdBy: string;
  createdAt: string;
}

// ============================================================
// PROVIDER TYPES
// ============================================================

export interface Provider {
  id: string;
  workspaceId: string;
  channel: Channel;
  providerType: string; // SENDGRID, TWILIO, FCM, etc.
  name: string;
  priority: number;
  credentials: Record<string, any>;
  rateLimit?: number | null;
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

// ============================================================
// USER & AUTH TYPES
// ============================================================

export interface User {
  id: string;
  workspaceId: string;
  email: string;
  name: string;
  role: UserRole;
  status: "ACTIVE" | "INVITED" | "SUSPENDED";
  lastLogin?: string | null;
  createdAt: string;
}

export interface ApiKey {
  id: string;
  workspaceId: string;
  keyPrefix: string;
  description: string;
  lastUsedAt?: string | null;
  expiresAt?: string | null;
  createdAt: string;
}

// ============================================================
// ANALYTICS TYPES
// ============================================================

export interface AnalyticsData {
  totalSent: number;
  deliveryRate: number;
  avgProcessingLatency: number;
  dlqCount: number;
  channelBreakdown: ChannelBreakdown[];
  providerLatency: ProviderLatency[];
  dailyStats: DailyStats[];
}

export interface ChannelBreakdown {
  channel: Channel;
  count: number;
  successRate: number;
}

export interface ProviderLatency {
  provider: string;
  p50Latency: number;
  p99Latency: number;
  successRate: number;
}

export interface DailyStats {
  date: string;
  sent: number;
  delivered: number;
  failed: number;
}

// ============================================================
// AUDIT LOG TYPES
// ============================================================

export interface AuditLog {
  id: string;
  workspaceId: string;
  userId: string;
  userName: string;
  action: string;
  entityType: string;
  entityId?: string | null;
  changes?: Record<string, any> | null;
  ipAddress: string;
  createdAt: string;
}

export interface AuditLogConnection {
  edges: AuditLogEdge[];
  pageInfo: PageInfo;
  totalCount: number;
}

export interface AuditLogEdge {
  node: AuditLog;
  cursor: string;
}

// ============================================================
// WORKSPACE TYPES
// ============================================================

export interface WorkspaceConfig {
  id: string;
  name: string;
  timezone: string;
  createdAt: string;
}

// ============================================================
// FILTER TYPES
// ============================================================

export interface NotificationFilter {
  status?: NotificationStatus | null;
  channel?: Channel | null;
  recipient?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

export interface TemplateFilter {
  channel?: Channel | null;
  search?: string | null;
}

export interface AuditLogFilter {
  userId?: string | null;
  action?: string | null;
  startDate?: string | null;
  endDate?: string | null;
}

// ============================================================
// INPUT TYPES (for mutations)
// ============================================================

export interface TemplateInput {
  slug: string;
  channel: Channel;
  description: string;
}

export interface ProviderInput {
  id?: string | null; // For updates
  channel: Channel;
  providerType: string;
  name: string;
  priority: number;
  credentials: Record<string, any>;
  rateLimit?: number | null;
  enabled: boolean;
}
