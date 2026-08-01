import type { Metadata } from "next";
import "./globals.css";
import { AuthProvider } from "@/contexts/auth-context";
import { ThemeProvider } from "@/contexts/theme-context";
import { ApolloWrapper } from "@/lib/apollo-provider";

export const metadata: Metadata = {
  title: "EventFlow - Notification Management Platform",
  description: "Enterprise-grade notification orchestration platform",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body>
        <ApolloWrapper>
          <ThemeProvider>
            <AuthProvider>
              {children}
            </AuthProvider>
          </ThemeProvider>
        </ApolloWrapper>
      </body>
    </html>
  );
}
