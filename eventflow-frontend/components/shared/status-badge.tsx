import { Badge } from "@/components/ui/badge";
import { NotificationStatus } from "@/types";

interface StatusBadgeProps {
  status: NotificationStatus;
}

/**
 * Status badge component with color-coded statuses per PRD
 * - Success (Emerald): DELIVERED
 * - Warning (Amber): RETRY_SCHEDULED, QUEUED, PROCESSING
 * - Destructive (Red): FAILED, BOUNCED
 * - Default: DISPATCHED, SENT
 */
export function StatusBadge({ status }: StatusBadgeProps) {
  const variantMap: Record<NotificationStatus, "success" | "warning" | "destructive" | "default"> = {
    [NotificationStatus.DELIVERED]: "success",
    [NotificationStatus.RETRY_SCHEDULED]: "warning",
    [NotificationStatus.QUEUED]: "warning",
    [NotificationStatus.PROCESSING]: "warning",
    [NotificationStatus.FAILED]: "destructive",
    [NotificationStatus.BOUNCED]: "destructive",
    [NotificationStatus.DISPATCHED]: "default",
    [NotificationStatus.SENT]: "default",
  };

  return (
    <Badge variant={variantMap[status]}>
      {status.replace(/_/g, " ")}
    </Badge>
  );
}
