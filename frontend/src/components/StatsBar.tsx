import { STATUS_LABELS, STATUS_ORDER, type TicketStats } from '../types';

export default function StatsBar({ stats }: { stats: TicketStats | null }) {
  return (
    <section className="stats" aria-label="Statistiques des tickets">
      <article className="stat-card stat-total">
        <span className="stat-label">Total</span>
        <strong className="stat-value">{stats?.total ?? '—'}</strong>
        <span className="stat-hint">{stats ? `${stats.unassigned} non assigné(s)` : ' '}</span>
      </article>
      {STATUS_ORDER.map((status) => (
        <article key={status} className={`stat-card stat-${status.toLowerCase().replace('_', '-')}`}>
          <span className="stat-label">{STATUS_LABELS[status]}</span>
          <strong className="stat-value">{stats?.byStatus[status] ?? '—'}</strong>
          <span className="stat-hint">&nbsp;</span>
        </article>
      ))}
    </section>
  );
}
