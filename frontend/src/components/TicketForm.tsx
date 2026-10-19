import { useEffect, useState, type FormEvent } from 'react';
import type { Ticket, TicketPayload, TicketPriority } from '../types';
import { PRIORITY_LABELS, PRIORITY_ORDER } from '../types';
import { CloseIcon } from './Icons';

interface Props {
  open: boolean;
  ticket: Ticket | null;
  saving: boolean;
  error: string | null;
  fieldErrors: Record<string, string>;
  onClose: () => void;
  onSubmit: (payload: TicketPayload) => void;
}

const emptyForm: TicketPayload = {
  title: '',
  description: '',
  priority: 'MEDIUM',
  category: '',
  requesterName: '',
  requesterEmail: '',
  assignee: '',
};

export default function TicketForm({
  open,
  ticket,
  saving,
  error,
  fieldErrors,
  onClose,
  onSubmit,
}: Props) {
  const [form, setForm] = useState<TicketPayload>(emptyForm);
  const [localErrors, setLocalErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!open) return;
    setLocalErrors({});
    setForm(
      ticket
        ? {
            title: ticket.title,
            description: ticket.description,
            priority: ticket.priority,
            category: ticket.category ?? '',
            requesterName: ticket.requesterName,
            requesterEmail: ticket.requesterEmail,
            assignee: ticket.assignee ?? '',
          }
        : emptyForm,
    );
  }, [open, ticket]);

  useEffect(() => {
    if (!open) return;
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    window.addEventListener('keydown', onKeyDown);
    return () => window.removeEventListener('keydown', onKeyDown);
  }, [open, onClose]);

  if (!open) return null;

  const update = (field: keyof TicketPayload, value: string) => {
    setForm((current) => ({ ...current, [field]: value }));
    setLocalErrors((current) => ({ ...current, [field]: '' }));
  };

  const validate = () => {
    const errors: Record<string, string> = {};
    if (form.title.trim().length < 5) errors.title = 'Le titre doit contenir au moins 5 caractères.';
    if (form.description.trim().length < 10) {
      errors.description = 'La description doit contenir au moins 10 caractères.';
    }
    if (!form.requesterName.trim()) errors.requesterName = 'Le nom du demandeur est obligatoire.';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.requesterEmail.trim())) {
      errors.requesterEmail = 'Adresse email invalide.';
    }
    setLocalErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    if (!validate()) return;
    onSubmit({
      ...form,
      title: form.title.trim(),
      description: form.description.trim(),
      requesterName: form.requesterName.trim(),
      requesterEmail: form.requesterEmail.trim(),
      category: form.category.trim(),
      assignee: form.assignee.trim(),
    });
  };

  const errorFor = (field: keyof TicketPayload) => localErrors[field] || fieldErrors[field];

  return (
    <div className="overlay" onClick={onClose} role="presentation">
      <div
        className="modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ticket-form-title"
        onClick={(event) => event.stopPropagation()}
      >
        <header className="modal-header">
          <div>
            <p className="eyebrow">{ticket ? `Modifier ${ticket.reference ?? ''}` : 'Nouveau ticket'}</p>
            <h2 id="ticket-form-title">{ticket ? 'Éditer le ticket' : 'Créer un ticket'}</h2>
          </div>
          <button className="icon-button" onClick={onClose} aria-label="Fermer" type="button">
            <CloseIcon />
          </button>
        </header>

        <form className="form-grid" onSubmit={handleSubmit} noValidate>
          <label className="field field-full">
            <span>Titre *</span>
            <input
              value={form.title}
              maxLength={120}
              onChange={(event) => update('title', event.target.value)}
              placeholder="Ex. Impossible de se connecter au portail"
              autoFocus
            />
            {errorFor('title') && <small className="field-error">{errorFor('title')}</small>}
          </label>

          <label className="field field-full">
            <span>Description *</span>
            <textarea
              rows={4}
              value={form.description}
              maxLength={4000}
              onChange={(event) => update('description', event.target.value)}
              placeholder="Décrivez le problème, les étapes de reproduction et l'impact..."
            />
            {errorFor('description') && (
              <small className="field-error">{errorFor('description')}</small>
            )}
          </label>

          <label className="field">
            <span>Priorité *</span>
            <select
              value={form.priority}
              onChange={(event) => update('priority', event.target.value as TicketPriority)}
            >
              {PRIORITY_ORDER.map((priority) => (
                <option key={priority} value={priority}>
                  {PRIORITY_LABELS[priority]}
                </option>
              ))}
            </select>
          </label>

          <label className="field">
            <span>Catégorie</span>
            <input
              value={form.category}
              maxLength={60}
              onChange={(event) => update('category', event.target.value)}
              placeholder="Ex. Réseau, Matériel, Accès"
            />
            {errorFor('category') && <small className="field-error">{errorFor('category')}</small>}
          </label>

          <label className="field">
            <span>Demandeur *</span>
            <input
              value={form.requesterName}
              maxLength={120}
              onChange={(event) => update('requesterName', event.target.value)}
              placeholder="Nom et prénom"
            />
            {errorFor('requesterName') && (
              <small className="field-error">{errorFor('requesterName')}</small>
            )}
          </label>

          <label className="field">
            <span>Email du demandeur *</span>
            <input
              type="email"
              value={form.requesterEmail}
              maxLength={180}
              onChange={(event) => update('requesterEmail', event.target.value)}
              placeholder="prenom.nom@kfokam48.cm"
            />
            {errorFor('requesterEmail') && (
              <small className="field-error">{errorFor('requesterEmail')}</small>
            )}
          </label>

          <label className="field field-full">
            <span>Assigné à</span>
            <input
              value={form.assignee}
              maxLength={120}
              onChange={(event) => update('assignee', event.target.value)}
              placeholder="Technicien en charge (optionnel)"
            />
            {errorFor('assignee') && <small className="field-error">{errorFor('assignee')}</small>}
          </label>

          {error && <p className="form-error field-full">{error}</p>}

          <footer className="modal-footer field-full">
            <button type="button" className="button ghost" onClick={onClose} disabled={saving}>
              Annuler
            </button>
            <button type="submit" className="button primary" disabled={saving}>
              {saving ? 'Enregistrement...' : ticket ? 'Enregistrer' : 'Créer le ticket'}
            </button>
          </footer>
        </form>
      </div>
    </div>
  );
}
