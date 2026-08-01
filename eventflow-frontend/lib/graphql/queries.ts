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
        providerResponse
        errorMessage
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
      activeVersion {
        id
        version
        channel
        subjectTemplate
        bodyTemplate
        isActive
        createdAt
      }
      versions {
        id
        version
        channel
        subjectTemplate
        bodyTemplate
        isActive
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
      activeVersion {
        id
        version
        isActive
      }
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
      name
      providerType
      channel
      isPrimary
      enabled
      rateLimit
      settings
      createdAt
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
      totalDelivered
      totalFailed
      totalDlq
      deliveryRate
      dailyStats {
        date
        channel
        sent
        delivered
        failed
        dlq
      }
      channelBreakdown {
        channel
        count
        percentage
      }
      topErrors {
        errorMessage
        count
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
          userId
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
    }
  }
`;

// ============================================================
// WORKSPACE QUERIES
// ============================================================

export const GET_WORKSPACE_CONFIG = gql`
  query GetWorkspaceConfig {
    workspaceConfig {
      apiKeys {
        id
        keyPrefix
        description
        active
        lastUsedAt
        createdAt
      }
      webhookSecrets {
        id
        label
        createdAt
      }
    }
  }
`;

export const LIST_USERS = gql`
  query ListUsers {
    users {
      id
      workspaceId
      email
      name
      role
      status
      lastLogin
      createdAt
    }
  }
`;

export const LIST_RETRIES = gql`
  query ListRetries {
    retries {
      id
      notificationId
      channel
      provider
      recipient
      attemptCount
      maxAttempts
      nextRetryAt
      errorMessage
      createdAt
    }
  }
`;

export const LIST_DLQ_MESSAGES = gql`
  query ListDlqMessages {
    dlqMessages {
      id
      originalTopic
      failureReason
      attemptCount
      failedAt
      payload
    }
  }
`;
