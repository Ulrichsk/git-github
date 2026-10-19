import { useCallback, useEffect, useMemo, useState } from 'react';
import { api, ApiError } from './api';
import type {
  PageResponse,
  Ticket,
  TicketPayload,
  TicketPriority,
  TicketStats,
  TicketStatus,
} from './types';
import { PRIORITY_LABELS, PRIORITY_ORDER, STATUS_LABELS, STATUS_ORDER } from './types';
import StatsBar from './components/StatsBar';
import TicketForm from './components/TicketForm';
import TicketDetail from './components/TicketDetail';
import { PriorityBadge, StatusBadge } from './components/Badge';
import {
  ChevronLeftIcon,
  ChevronRightIcon,
  CommentIcon,
  PlusIcon,
  SearchIcon,
} from './components/Icons';

interface Filters {
  status: TicketStatus | '';
  priority: TicketPriority | '';
  q: string;
  page: number;
  size: number;
}

interface Toast {
  type: 'success' | 'error';
  message: string;
}

const PAGE_SIZE = 8;

const dateFormatter = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium' });

const formatDate = (value: string) => dateFormatter.format(new Date(value));

function App() {
  const [filters, setFilters] = useState<Filters>({
    status: '',
    priority: '',
    q: '',
    page: 0,
    size: PAGE_SIZE,
  });
  const [searchInput, setSearchInput] = useState('');
  const [pageData, setPageData] = useState<PageResponse<Ticket> | null>(null);
  const [stats, setStats] = useState<TicketStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<Ticket | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);
  const [busy, setBusy] = useState(false);

  const [formOpen, setFormOpen] = useState(false);
  const [editing, setEditing] = useState<Ticket | null>(null);
  const [formSaving, setFormSaving] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  const [toast, setToast] = useState<Toast | null>(null);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setListError(null);
    try {
      const data = await api.listTickets(filters);
      setPageData(data);
    } catch (error) {
      setListError(error instanceof Error ? error.message : 'Erreur lors du chargement des tickets.');
    } finally {
      setLoading(false);
    }
  }, [filters]);

  const loadStats = useCallback(async () => {
    try {
      setStats(await api.getStats());
    } catch {
      // Les statistiques ne doivent pas bloquer la liste si l'appel échoue.
    }
  }, []);

  useEffect(() => {
    void loadTickets();
  }, [loadTickets]);

  useEffect(() => {
    void loadStats();
  }, [loadStats]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setFilters((current) =>
        current.q === searchInput ? current : { ...current, q: searchInput, page: 0 },
      );
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchInput]);

  useEffect(() => {
    if (selectedId === null) {
      setDetail(null);
      return;
    }
    let cancelled = false;
    setDetailLoading(true);
    api
      .getTicket(selectedId)
      .then((ticket) => {
        if (!cancelled) setDetail(ticket);
      })
      .catch((error) => {
        if (!cancelled) {
          setToast({
            type: 'error',
            message: error instanceof Error ? error.message : 'Ticket introuvable.',
          });
          setSelectedId(null);
        }
      })
      .finally(() => {
        if (!cancelled) setDetailLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  useEffect(() => {
    if (!toast) return;
    const timer = window.setTimeout(() => setToast(null), 4500);
    return () => window.clearTimeout(timer);
  }, [toast]);

  const refreshData = useCallback(async () => {
    await Promise.all([loadTickets(), loadStats()]);
  }, [loadTickets, loadStats]);

  const showError = (error: unknown) => {
    setToast({
      type: 'error',
      message: error instanceof Error ? error.message : 'Une erreur est survenue.',
    });
  };

  const openCreate = () => {
    setEditing(null);
    setFormError(null);
    setFieldErrors({});
    setFormOpen(true);
  };

  const openEdit = () => {
    if (!detail) return;
    setEditing(detail);
    setFormError(null);
    setFieldErrors({});
    setFormOpen(true);
  };

  const handleFormSubmit = async (payload: TicketPayload) => {
    setFormSaving(true);
    setFormError(null);
    setFieldErrors({});
    try {
      if (editing) {
        const updated = await api.updateTicket(editing.id, payload);
        setDetail(updated);
        setToast({ type: 'success', message: `${updated.reference ?? 'Le ticket'} a été mis à jour.` });
      } else {
        const created = await api.createTicket(payload);
        setToast({ type: 'success', message: `${created.reference ?? 'Le ticket'} a été créé.` });
      }
      setFormOpen(false);
      setEditing(null);
      await refreshData();
    } catch (error) {
      setFormError(error instanceof Error ? error.message : "Erreur lors de l'enregistrement.");
      if (error instanceof ApiError) setFieldErrors(error.fieldErrors);
    } finally {
      setFormSaving(false);
    }
  };

  const handleStatus = async (status: TicketStatus) => {
    if (!detail) return;
    setBusy(true);
    try {
      const updated = await api.changeStatus(detail.id, status);
      setDetail(updated);
      setToast({
        type: 'success',
        message: `Statut mis à jour : ${STATUS_LABELS[updated.status].toLowerCase()}.`,
      });
      await refreshData();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  };

  const handleAssign = async (assignee: string) => {
    if (!detail) return;
    setBusy(true);
    try {
      const updated = await api.assignTicket(detail.id, assignee);
      setDetail(updated);
      setToast({ type: 'success', message: `Ticket assigné à ${updated.assignee}.` });
      await refreshData();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  };

  const handleComment = async (author: string, content: string) => {
    if (!detail) return;
    await api.addComment(detail.id, author, content);
    const refreshed = await api.getTicket(detail.id);
    setDetail(refreshed);
    setToast({ type: 'success', message: 'Commentaire ajouté.' });
    await refreshData();
  };

  const handleDelete = async () => {
    if (!detail) return;
    if (!window.confirm(`Supprimer définitivement le ticket ${detail.reference ?? ''} ?`)) return;
    setBusy(true);
    try {
      await api.deleteTicket(detail.id);
      setSelectedId(null);
      setDetail(null);
      setToast({ type: 'success', message: 'Ticket supprimé.' });
      await refreshData();
    } catch (error) {
      showError(error);
    } finally {
      setBusy(false);
    }
  };

  const changeFilter = (patch: Partial<Filters>) => {
    setFilters((current) => ({ ...current, ...patch, page: 0 }));
  };

  const resetFilters = () => {
    setSearchInput('');
    setFilters({ status: '', priority: '', q: '', page: 0, size: PAGE_SIZE });
  };

  const hasFilters = Boolean(filters.q || filters.status || filters.priority);
  const tickets = pageData?.content ?? [];
  const activeTickets = stats ? stats.total - stats.byStatus.CLOSED : null;
  const resolutionRate = useMemo(() => {
    if (!stats || stats.total === 0) return null;
    return Math.round(((stats.byStatus.RESOLVED + stats.byStatus.CLOSED) / stats.total) * 100);
  }, [stats]);

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark" aria-hidden="true">
            SD
          </span>
          <div>
            <h1>SupportDesk</h1>
            <p>KFOKAM48 · Batch 2 · Module Ticket</p>
          </div>
        </div>
        <div className="header-side">
          <div className="header-kpi">
            <strong>{activeTickets ?? '—'}</strong>
            <span>tickets actifs</span>
          </div>
          <button className="button primary" onClick={openCreate} type="button">
            <PlusIcon width={16} height={16} /> Nouveau ticket
          </button>
        </div>
      </header>

      <main className="app-main">
        <section className="page-intro">
          <div>
            <p className="eyebrow">Support & assistance</p>
            <h2>Gestion des tickets</h2>
            <p className="muted">
              Centralisez les demandes, suivez leur cycle de vie et collaborez sur chaque
              incident jusqu’à la clôture.
              {resolutionRate !== null && ` Taux de résolution : ${resolutionRate} %.`}
            </p>
          </div>
        </section>

        <StatsBar stats={stats} />

        <section className="panel">
          <div className="toolbar">
            <div className="search-box">
              <SearchIcon width={17} height={17} />
              <input
                value={searchInput}
                onChange={(event) => setSearchInput(event.target.value)}
                placeholder="Rechercher un titre, une référence, un demandeur..."
                aria-label="Rechercher un ticket"
              />
            </div>

            <div className="filters">
              <select
                value={filters.status}
                onChange={(event) => changeFilter({ status: event.target.value as TicketStatus | '' })}
                aria-label="Filtrer par statut"
              >
                <option value="">Tous les statuts</option>
                {STATUS_ORDER.map((status) => (
                  <option key={status} value={status}>
                    {STATUS_LABELS[status]}
                  </option>
                ))}
              </select>

              <select
                value={filters.priority}
                onChange={(event) =>
                  changeFilter({ priority: event.target.value as TicketPriority | '' })
                }
                aria-label="Filtrer par priorité"
              >
                <option value="">Toutes les priorités</option>
                {PRIORITY_ORDER.map((priority) => (
                  <option key={priority} value={priority}>
                    {PRIORITY_LABELS[priority]}
                  </option>
                ))}
              </select>

              {hasFilters && (
                <button className="button ghost small" onClick={resetFilters} type="button">
                  Réinitialiser
                </button>
              )}
            </div>
          </div>

          {listError && (
            <div className="alert" role="alert">
              <span>{listError}</span>
              <button className="button ghost small" onClick={() => void refreshData()} type="button">
                Réessayer
              </button>
            </div>
          )}

          <div className="table-wrap">
            <table className="ticket-table">
              <thead>
                <tr>
                  <th>Référence</th>
                  <th>Ticket</th>
                  <th>Priorité</th>
                  <th>Statut</th>
                  <th>Assigné à</th>
                  <th>Créé le</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {loading &&
                  [0, 1, 2].map((row) => (
                    <tr key={`skeleton-${row}`} className="skeleton-row">
                      <td colSpan={7}>
                        <span className="skeleton" />
                      </td>
                    </tr>
                  ))}

                {!loading &&
                  tickets.map((ticket) => (
                    <tr
                      key={ticket.id}
                      className={selectedId === ticket.id ? 'selected' : ''}
                      onClick={() => setSelectedId(ticket.id)}
                      onKeyDown={(event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          setSelectedId(ticket.id);
                        }
                      }}
                      tabIndex={0}
                    >
                      <td className="cell-reference">{ticket.reference ?? `#${ticket.id}`}</td>
                      <td className="cell-ticket">
                        <strong>{ticket.title}</strong>
                        <span>
                          {ticket.category || 'Sans catégorie'} · {ticket.requesterName}
                          {ticket.commentCount > 0 && (
                            <span className="comment-count">
                              <CommentIcon width={13} height={13} /> {ticket.commentCount}
                            </span>
                          )}
                        </span>
                      </td>
                      <td>
                        <PriorityBadge priority={ticket.priority} />
                      </td>
                      <td>
                        <StatusBadge status={ticket.status} />
                      </td>
                      <td className={ticket.assignee ? '' : 'muted'}>
                        {ticket.assignee || 'Non assigné'}
                      </td>
                      <td className="cell-date">{formatDate(ticket.createdAt)}</td>
                      <td className="cell-action">
                        <ChevronRightIcon width={16} height={16} />
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>

          {!loading && !listError && tickets.length === 0 && (
            <div className="empty-state">
              <div className="empty-mark" aria-hidden="true">
                <CommentIcon width={26} height={26} />
              </div>
              <h3>Aucun ticket trouvé</h3>
              <p>
                {hasFilters
                  ? 'Aucun ticket ne correspond à ces critères. Ajustez les filtres ou réinitialisez la recherche.'
                  : 'Le support est calme : créez le premier ticket pour commencer le suivi.'}
              </p>
              {hasFilters ? (
                <button className="button soft" onClick={resetFilters} type="button">
                  Réinitialiser les filtres
                </button>
              ) : (
                <button className="button primary" onClick={openCreate} type="button">
                  <PlusIcon width={16} height={16} /> Créer un ticket
                </button>
              )}
            </div>
          )}

          {pageData && pageData.totalElements > 0 && (
            <footer className="pagination">
              <span>
                {pageData.totalElements} ticket{pageData.totalElements > 1 ? 's' : ''} · page{' '}
                {pageData.page + 1} / {Math.max(pageData.totalPages, 1)}
              </span>
              <div className="pagination-actions">
                <button
                  className="button ghost small"
                  disabled={pageData.first || loading}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page - 1 }))}
                  type="button"
                >
                  <ChevronLeftIcon width={15} height={15} /> Précédent
                </button>
                <button
                  className="button ghost small"
                  disabled={pageData.last || loading}
                  onClick={() => setFilters((current) => ({ ...current, page: current.page + 1 }))}
                  type="button"
                >
                  Suivant <ChevronRightIcon width={15} height={15} />
                </button>
              </div>
            </footer>
          )}
        </section>
      </main>

      <TicketForm
        open={formOpen}
        ticket={editing}
        saving={formSaving}
        error={formError}
        fieldErrors={fieldErrors}
        onClose={() => {
          setFormOpen(false);
          setEditing(null);
        }}
        onSubmit={(payload) => void handleFormSubmit(payload)}
      />

      <TicketDetail
        ticket={selectedId !== null ? detail : null}
        loading={detailLoading}
        busy={busy}
        onClose={() => setSelectedId(null)}
        onStatus={(status) => void handleStatus(status)}
        onAssign={(assignee) => void handleAssign(assignee)}
        onComment={handleComment}
        onEdit={openEdit}
        onDelete={() => void handleDelete()}
      />

      {toast && (
        <div className={`toast toast-${toast.type}`} role="status">
          <span>{toast.message}</span>
          <button onClick={() => setToast(null)} aria-label="Fermer la notification" type="button">
            ×
          </button>
        </div>
      )}
    </div>
  );
}

export default App;
