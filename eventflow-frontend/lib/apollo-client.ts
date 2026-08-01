import { ApolloClient, InMemoryCache, HttpLink } from "@apollo/client";
import { setContext } from "@apollo/client/link/context";
import { onError } from "@apollo/client/link/error";

/**
 * GraphQL endpoint - connects to backend Spring Boot application
 */
const httpLink = new HttpLink({
  uri: process.env.NEXT_PUBLIC_GRAPHQL_ENDPOINT || "http://localhost:8080/graphql",
  credentials: "include",
});

/**
 * Authentication link - adds JWT token to requests
 */
const authLink = setContext((_, { headers }) => {
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
 */
const errorLink = onError(({ graphQLErrors, networkError }: any) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({ message, locations, path, extensions }: any) => {
      console.error(
        `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`
      );

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
 * Apollo Client instance - shared across the application
 */
export const apolloClient = new ApolloClient({
  link: errorLink.concat(authLink).concat(httpLink),
  cache: new InMemoryCache({
    typePolicies: {
      Query: {
        fields: {
          notifications: {
            keyArgs: ["filter"],
            merge(existing, incoming, { args }) {
              if (!existing) return incoming;
              if (!args?.after) return incoming;
              
              return {
                ...incoming,
                edges: [...existing.edges, ...incoming.edges],
              };
            },
          },
          auditLogs: {
            keyArgs: ["filter"],
            merge(existing, incoming, { args }) {
              if (!existing) return incoming;
              if (!args?.after) return incoming;
              
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
  // Reduce aggressive refetching - use cache first
  defaultOptions: {
    watchQuery: {
      fetchPolicy: 'cache-first',
    },
    query: {
      fetchPolicy: 'cache-first',
    },
  },
});
