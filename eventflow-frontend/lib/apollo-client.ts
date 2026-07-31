import { ApolloClient, InMemoryCache, HttpLink, from } from "@apollo/client";
import { setContext } from "@apollo/client/link/context";
import { onError } from "@apollo/client/link/error";

/**
 * GraphQL endpoint - connects to backend Spring Boot application
 * Per PRD Section 83 - Frontend Structure
 */
const httpLink = new HttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || "http://localhost:8080/graphql",
  credentials: "include", // Include cookies for session management
});

/**
 * Authentication link - adds JWT token to requests
 * Token retrieved from localStorage or cookie
 */
const authLink = setContext((_, { headers }) => {
  // Get token from localStorage (client-side only)
  const token = typeof window !== "undefined" 
    ? localStorage.getItem("eventflow_token") 
    : null;

  return {
    headers: {
      ...headers,
      authorization: token ? `Bearer ${token}` : "",
      "x-workspace-id": typeof window !== "undefined"
        ? localStorage.getItem("eventflow_workspace_id") || ""
        : "",
    },
  };
});

/**
 * Error link - handles GraphQL and network errors
 * Logs errors and redirects to login on authentication failures
 */
const errorLink = onError(({ graphQLErrors, networkError }) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message, locations, path, extensions }) => {
      console.error(
        `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`
      );

      // Handle authentication errors
      if (extensions?.code === "UNAUTHENTICATED") {
        if (typeof window !== "undefined") {
          localStorage.removeItem("eventflow_token");
          localStorage.removeItem("eventflow_workspace_id");
          window.location.href = "/login";
        }
      }
    });
  }

  if (networkError) {
    console.error(`[Network error]: ${networkError}`);
  }
});

/**
 * Apollo Client instance
 * Configured with authentication, error handling, and caching
 */
export const apolloClient = new ApolloClient({
  link: from([errorLink, authLink, httpLink]),
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          notifications: {
            // Cursor-based pagination policy
            keyArgs: ["filter"],
            merge(existing, incoming, { args }) {
              if (!existing) return incoming;
              if (!args?.after) return incoming; // First page
              
              return {
                ...incoming,
                edges: [...existing.edges, ...incoming.edges],
              };
            },
          },
        },
      },
    },
  }),
  defaultOptions: {
    watchQuery: {
      fetchPolicy: "cache-and-network",
      errorPolicy: "all",
    },
    query: {
      fetchPolicy: "network-only",
      errorPolicy: "all",
    },
    mutate: {
      errorPolicy: "all",
    },
  },
});

/**
 * Type-safe mutation helper
 */
export async function executeMutation<T = any>(
  mutation: any,
  variables?: Record<string, any>
): Promise<T> {
  const result = await apolloClient.mutate({
    mutation,
    variables,
  });

  if (result.errors) {
    throw new Error(result.errors[0].message);
  }

  return result.data;
}
