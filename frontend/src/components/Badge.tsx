import {
  PRIORITY_LABELS,
  STATUS_LABELS,
  type TicketPriority,
  type TicketStatus,
} from '../types';

export function StatusBadge({ status }: { status: TicketStatus }) {
  return (
    <span className={`badge status-${status.toLowerCase().replace('_', '-')}`}>
      <span className="badge-dot" />
      {STATUS_LABELS[status]}
    </span>
  );
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  return (
    <span className={`badge priority-${priority.toLowerCase()}`}>
      {PRIORITY_LABELS[priority]}
    </span>
  );
}
