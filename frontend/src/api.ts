import type {
  PageResponse,
  Ticket,
  TicketComment,
  TicketPayload,
  TicketPriority,
  TicketStats,
  TicketStatus,
} from './types';

const API_URL = (import.meta.env.VITE_API_URL ?? '/api').replace(/\/+$/, '');

export class ApiError extends Error {
  status: number;
  fieldErrors: Record<string, string>;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    });
  } catch {
    throw new ApiError(0, "Impossible de joindre l'API. Vérifiez que le serveur est démarré.");
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') ?? '';
  const body = contentType.includes('application/json') ? await response.json() : null;

  if (!response.ok) {
    const message =
      body?.detail || body?.message || `Erreur HTTP ${response.status} lors de l'appel à l'API.`;
    throw new ApiError(response.status, message, body?.errors ?? {});
  }

  return body as T;
}

export interface TicketFilters {
  status?: TicketStatus | '';
  priority?: TicketPriority | '';
  q?: string;
  page?: number;
  size?: number;
}

export const api = {
  listTickets(filters: TicketFilters): Promise<PageResponse<Ticket>> {
    const params = new URLSearchParams();
    if (filters.status) params.set('status', filters.status);
    if (filters.priority) params.set('priority', filters.priority);
    if (filters.q?.trim()) params.set('q', filters.q.trim());
    params.set('page', String(filters.page ?? 0));
    params.set('size', String(filters.size ?? 8));
    params.set('sort', 'createdAt,desc');
    return request<PageResponse<Ticket>>(`/tickets?${params.toString()}`);
  },

  getStats(): Promise<TicketStats> {
    return request<TicketStats>('/tickets/stats');
  },

  getTicket(id: number): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`);
  },

  createTicket(payload: TicketPayload): Promise<Ticket> {
    return request<Ticket>('/tickets', { method: 'POST', body: JSON.stringify(payload) });
  },

  updateTicket(id: number, payload: TicketPayload): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
  },

  changeStatus(id: number, status: TicketStatus): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status }),
    });
  },

  assignTicket(id: number, assignee: string): Promise<Ticket> {
    return request<Ticket>(`/tickets/${id}/assignee`, {
      method: 'PATCH',
      body: JSON.stringify({ assignee }),
    });
  },

  addComment(id: number, author: string, content: string): Promise<TicketComment> {
    return request<TicketComment>(`/tickets/${id}/comments`, {
      method: 'POST',
      body: JSON.stringify({ author, content }),
    });
  },

  deleteTicket(id: number): Promise<void> {
    return request<void>(`/tickets/${id}`, { method: 'DELETE' });
  },
};
