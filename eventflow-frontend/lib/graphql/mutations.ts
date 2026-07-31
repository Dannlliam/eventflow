import { gql } from "@apollo/client";

/**
 * GraphQL Mutations for EventFlow Admin Dashboard
 * Per PRD Part 10 - UI Requirements & Admin Dashboard Specification
 */

// ============================================================
// TEMPLATE MUTATIONS
// ============================================================

export const UPSERT_TEMPLATE = gql`
  mutation UpsertTemplate($input: TemplateInput!) {
    upsertTemplate(input: $input) {
      id
      slug
      channel
      description
      createdAt
      updatedAt
    }
  }
`;

export const PUBLISH_TEMPLATE_VERSION = gql`
  mutation PublishTemplateVersion(
    $slug: String!
    $body: String!
    $subject: String
  ) {
    publishTemplateVersion(slug: $slug, body: $body, subject: $subject) {
      id
      templateSlug
      version
      subjectTemplate
      bodyTemplate
      isActive
      createdBy
      createdAt
    }
  }
`;

// ============================================================
// DLQ MUTATIONS
// ============================================================

export const REPLAY_DLQ_MESSAGE = gql`
  mutation ReplayDlqMessage($eventId: String!) {
    replayDlqMessage(eventId: $eventId)
  }
`;

export const REPLAY_DLQ_BATCH = gql`
  mutation ReplayDlqBatch($eventIds: [String!]!) {
    replayDlqBatch(eventIds: $eventIds) {
      succeeded
      failed
    }
  }
`;

// ============================================================
// PROVIDER MUTATIONS
// ============================================================

export const SAVE_PROVIDER_CONFIG = gql`
  mutation SaveProviderConfig($input: ProviderInput!) {
    saveProviderConfig(input: $input) {
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
// API KEY MUTATIONS
// ============================================================

export const GENERATE_API_KEY = gql`
  mutation GenerateApiKey($description: String!) {
    generateApiKey(description: $description) {
      id
      keyPrefix
      fullKey
      description
      createdAt
    }
  }
`;

export const DEACTIVATE_API_KEY = gql`
  mutation DeactivateApiKey($keyId: ID!) {
    deactivateApiKey(keyId: $keyId)
  }
`;
