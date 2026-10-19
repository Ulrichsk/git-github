import { useEffect, useState, type FormEvent } from 'react';
import type { Ticket, TicketStatus } from '../types';
import { NEXT_ACTIONS } from '../types';
import { CloseIcon, CommentIcon, EditIcon, TrashIcon } from './Icons';
import { PriorityBadge, StatusBadge } from './Badge';

interface Props {
  ticket: Ticket | null;
  loading: boolean;
  busy: boolean;
  onClose: () => void;
  onStatus: (status: TicketStatus) => void;
  onAssign: (assignee: string) => void;
  onComment: (author: string, content: string) => Promise<void>;
  onEdit: () => void;
  onDelete: () => void;
}

const dateFormatter = new Intl.DateTimeFormat('fr-FR', {
  dateStyle: 'medium',
  timeStyle: 'short',
});

const formatDate = (value: string) => dateFormatter.format(new Date(value));

export default function TicketDetail({
  ticket,
  loading,
  busy,
  onClose,
  onStatus,
  onAssign,
  onComment,
  onEdit,
  onDelete,
}: Props) {
  const [assignee, setAssignee] = useState('');
  const [author, setAuthor] = useState('');
  const [content, setContent] = useState('');
  const [commentError, setCommentError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    setAssignee(ticket?.assignee ?? '');
    setAuthor('');
    setContent('');
    setCommentError(null);
  }, [ticket?.id]);

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  if (!ticket && !loading) return null;

  const handleComment = async (event: FormEvent) => {
    event.preventDefault();
    setCommentError(null);
    if (author.trim().length < 2) {
      setCommentError("Indiquez votre nom (2 caractères minimum).");
      return;
    }
    if (content.trim().length < 2) {
      setCommentError('Le commentaire ne peut pas être vide.');
      return;
    }
    setSending(true);
    try {
      await onComment(author.trim(), content.trim());
      setContent('');
    } catch (error) {
      setCommentError(error instanceof Error ? error.message : 'Erreur lors de l’envoi.');
    } finally {
      setSending(false);
    }
  };

  const closed = ticket?.status === 'CLOSED';

  return (
    <div className="overlay drawer-overlay" onClick={onClose} role="presentation">
      <aside
        className="drawer"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ticket-detail-title"
        onClick={(event) => event.stopPropagation()}
      >
        {loading || !ticket ? (
          <div className="drawer-loading">
            <span className="spinner" aria-hidden="true" />
            Chargement du ticket...
          </div>
        ) : (
          <>
            <header className="drawer-header">
              <div className="drawer-heading">
                <div className="drawer-reference">
                  <span>{ticket.reference ?? `#${ticket.id}`}</span>
                  <StatusBadge status={ticket.status} />
                  <PriorityBadge priority={ticket.priority} />
                </div>
                <h2 id="ticket-detail-title">{ticket.title}</h2>
                <p className="drawer-subtitle">
                  Créé le {formatDate(ticket.createdAt)} · Mis à jour le {formatDate(ticket.updatedAt)}
                </p>
              </div>
              <button className="icon-button" onClick={onClose} aria-label="Fermer" type="button">
                <CloseIcon />
              </button>
            </header>

            <div className="drawer-body">
              <section className="detail-section">
                <h3>Description</h3>
                <p className="description">{ticket.description}</p>
              </section>

              <section className="meta-grid">
                <div>
                  <span className="meta-label">Demandeur</span>
                  <strong>{ticket.requesterName}</strong>
                  <a href={`mailto:${ticket.requesterEmail}`}>{ticket.requesterEmail}</a>
                </div>
                <div>
                  <span className="meta-label">Catégorie</span>
                  <strong>{ticket.category || 'Non catégorisé'}</strong>
                </div>
                <div>
                  <span className="meta-label">Assigné à</span>
                  <strong>{ticket.assignee || 'Non assigné'}</strong>
                </div>
                <div>
                  <span className="meta-label">Commentaires</span>
                  <strong>{ticket.commentCount}</strong>
                </div>
              </section>

              <section className="detail-section">
                <h3>Cycle de vie</h3>
                <div className="action-row">
                  {NEXT_ACTIONS[ticket.status].map((action) => (
                    <button
                      key={action.status}
                      className={`button ${action.status === 'CLOSED' ? 'primary' : 'soft'}`}
                      disabled={busy}
                      onClick={() => onStatus(action.status)}
                      type="button"
                    >
                      {action.label}
                    </button>
                  ))}
                  {ticket.status === 'RESOLVED' && !ticket.assignee && (
                    <span className="inline-hint">Assignez le ticket avant de le clôturer.</span>
                  )}
                  {closed && <span className="inline-hint">Ce ticket est clôturé, il n’est plus modifiable.</span>}
                </div>

                {!closed && (
                  <form
                    className="assign-form"
                    onSubmit={(event) => {
                      event.preventDefault();
                      if (assignee.trim()) onAssign(assignee.trim());
                    }}
                  >
                    <input
                      value={assignee}
                      onChange={(event) => setAssignee(event.target.value)}
                      placeholder="Assigner à un technicien"
                      maxLength={120}
                      aria-label="Nom du technicien"
                    />
                    <button className="button soft" type="submit" disabled={busy || !assignee.trim()}>
                      Assigner
                    </button>
                  </form>
                )}
              </section>

              <section className="detail-section">
                <h3>
                  Commentaires <span className="count-pill">{ticket.comments?.length ?? 0}</span>
                </h3>
                <div className="timeline">
                  {(!ticket.comments || ticket.comments.length === 0) && (
                    <p className="muted">Aucun commentaire pour l’instant.</p>
                  )}
                  {ticket.comments?.map((comment) => (
                    <article className="comment" key={comment.id}>
                      <div className="comment-avatar" aria-hidden="true">
                        {comment.author.slice(0, 1).toUpperCase()}
                      </div>
                      <div>
                        <div className="comment-meta">
                          <strong>{comment.author}</strong>
                          <time dateTime={comment.createdAt}>{formatDate(comment.createdAt)}</time>
                        </div>
                        <p>{comment.content}</p>
                      </div>
                    </article>
                  ))}
                </div>

                {!closed && (
                  <form className="comment-form" onSubmit={handleComment}>
                    <input
                      value={author}
                      onChange={(event) => setAuthor(event.target.value)}
                      placeholder="Votre nom"
                      maxLength={120}
                      aria-label="Auteur du commentaire"
                    />
                    <textarea
                      value={content}
                      onChange={(event) => setContent(event.target.value)}
                      placeholder="Ajouter un suivi, une information ou une résolution..."
                      rows={3}
                      maxLength={2000}
                      aria-label="Contenu du commentaire"
                    />
                    {commentError && <small className="field-error">{commentError}</small>}
                    <button className="button primary" type="submit" disabled={sending}>
                      <CommentIcon width={16} height={16} />
                      {sending ? 'Envoi...' : 'Commenter'}
                    </button>
                  </form>
                )}
              </section>
            </div>

            <footer className="drawer-footer">
              <button
                className="button ghost"
                type="button"
                onClick={onEdit}
                disabled={closed || busy}
              >
                <EditIcon width={16} height={16} /> Modifier
              </button>
              {closed && (
                <button className="button danger" type="button" onClick={onDelete} disabled={busy}>
                  <TrashIcon width={16} height={16} /> Supprimer
                </button>
              )}
            </footer>
          </>
        )}
      </aside>
    </div>
  );
}
