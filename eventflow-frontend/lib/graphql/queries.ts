import { gql } from "@apollo/client";

/**
 * GraphQL Queries for EventFlow Admin Dashboard
 * Per PRD Part 10 - UI Requirements & Admin Dashboard Specification
 */

// ============================================================
// NOTIFICATION QUERIES
// ============================================================

export const GET_NOTIFICATION = gql`
  query GetNotification($id: ID!) {
    notification(id: $id) {
      id
      workspaceId
      channel
      recipient {
        email
        phone
        deviceToken
        webhookUrl
      }
      templateSlug
      payload
      metadata
      status
      attemptCount
      nextRetryAt
      sentAt
      deliveredAt
      createdAt
      updatedAt
      events {
        id
        eventType
        status
        provider
        errorMessage
        metadata
        createdAt
      }
    }
  }
`;

export const LIST_NOTIFICATIONS = gql`
  query ListNotifications(
    $filter: NotificationFilter
    $first: Int!
    $after: String
  ) {
    notifications(filter: $filter, first: $first, after: $after) {
      edges {
        node {
          id
          workspaceId
          channel
          recipient {
            email
            phone
            deviceToken
            webhookUrl
          }
          templateSlug
          status
          attemptCount
          nextRetryAt
          sentAt
          deliveredAt
          createdAt
          updatedAt
        }
        cursor
      }
      pageInfo {
        hasNextPage
        endCursor
        totalCount
      }
      totalCount
    }
  }
`;

// ============================================================
// TEMPLATE QUERIES
// ============================================================

export const GET_TEMPLATE = gql`
  query GetTemplate($slug: String!) {
    template(slug: $slug) {
      id
      slug
      channel
      description
      versions {
        id
        templateSlug
        version
        subjectTemplate
        bodyTemplate
        isActive
        createdBy
        createdAt
      }
      createdAt
      updatedAt
    }
  }
`;

export const LIST_TEMPLATES = gql`
  query ListTemplates($filter: TemplateFilter) {
    templates(filter: $filter) {
      id
      slug
      channel
      description
      versions {
        id
        version
        isActive
      }
      createdAt
      updatedAt
    }
  }
`;

// ============================================================
// PROVIDER QUERIES
// ============================================================

export const LIST_PROVIDERS = gql`
  query ListProviders($channel: String) {
    providers(channel: $channel) {
      id
      workspaceId
      channel
      providerType
      name
      priority
      rateLimit
      enabled
      createdAt
      updatedAt
    }
  }
`;

// ============================================================
// ANALYTICS QUERIES
// ============================================================

export const GET_ANALYTICS = gql`
  query GetAnalytics(
    $startDate: String!
    $endDate: String!
    $channel: String
    $workspaceId: String
  ) {
    analytics(
      startDate: $startDate
      endDate: $endDate
      channel: $channel
      workspaceId: $workspaceId
    ) {
      totalSent
      deliveryRate
      avgProcessingLatency
      dlqCount
      channelBreakdown {
        channel
        count
        successRate
      }
      providerLatency {
        provider
        p50Latency
        p99Latency
        successRate
      }
      dailyStats {
        date
        sent
        delivered
        failed
      }
    }
  }
`;

// ============================================================
// AUDIT LOG QUERIES
// ============================================================

export const LIST_AUDIT_LOGS = gql`
  query ListAuditLogs(
    $filter: AuditLogFilter
    $first: Int!
    $after: String
  ) {
    auditLogs(filter: $filter, first: $first, after: $after) {
      edges {
        node {
          id
          workspaceId
          userId
          userName
          action
          entityType
          entityId
          changes
          ipAddress
          createdAt
        }
        cursor
      }
      pageInfo {
        hasNextPage
        endCursor
        totalCount
      }
      totalCount
    }
  }
`;

// ============================================================
// WORKSPACE QUERIES
// ============================================================

export const GET_WORKSPACE_CONFIG = gql`
  query GetWorkspaceConfig {
    workspaceConfig {
      id
      name
      timezone
      createdAt
    }
  }
`;
