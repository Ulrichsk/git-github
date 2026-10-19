export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';

export interface TicketComment {
  id: number;
  author: string;
  content: string;
  createdAt: string;
}

export interface Ticket {
  id: number;
  reference: string | null;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: string | null;
  requesterName: string;
  requesterEmail: string;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
  commentCount: number;
  comments?: TicketComment[];
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface TicketStats {
  total: number;
  byStatus: Record<TicketStatus, number>;
  byPriority: Record<TicketPriority, number>;
  unassigned: number;
}

export interface TicketPayload {
  title: string;
  description: string;
  priority: TicketPriority;
  category: string;
  requesterName: string;
  requesterEmail: string;
  assignee: string;
}

export const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: 'Ouvert',
  IN_PROGRESS: 'En cours',
  RESOLVED: 'Résolu',
  CLOSED: 'Clôturé',
};

export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  LOW: 'Basse',
  MEDIUM: 'Moyenne',
  HIGH: 'Haute',
  CRITICAL: 'Critique',
};

export const STATUS_ORDER: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITY_ORDER: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

export const NEXT_ACTIONS: Record<TicketStatus, { status: TicketStatus; label: string }[]> = {
  OPEN: [{ status: 'IN_PROGRESS', label: 'Prendre en charge' }],
  IN_PROGRESS: [
    { status: 'RESOLVED', label: 'Marquer résolu' },
    { status: 'OPEN', label: 'Remettre en attente' },
  ],
  RESOLVED: [
    { status: 'CLOSED', label: 'Clôturer' },
    { status: 'IN_PROGRESS', label: 'Reprendre' },
  ],
  CLOSED: [],
};
