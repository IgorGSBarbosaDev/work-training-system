import { ReactNode } from 'react'
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  CheckCircle2,
  Clock3,
  Inbox,
  LoaderCircle,
  LucideIcon,
  RefreshCw,
  SearchX,
  ShieldAlert,
} from 'lucide-react'
import { Link } from 'react-router-dom'

export function Button({
  children,
  variant = 'primary',
  className = '',
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'outline' | 'danger' | 'ghost'
}) {
  const variants = {
    primary: 'border-primary bg-primary text-white hover:bg-[#0b5962]',
    outline: 'border-border bg-card text-foreground hover:border-primary hover:text-primary',
    danger: 'border-destructive bg-destructive text-white hover:bg-[#8f352c]',
    ghost: 'border-transparent bg-transparent text-foreground hover:bg-muted',
  }
  return (
    <button
      {...props}
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-md border px-4 py-2 text-sm font-semibold transition disabled:cursor-not-allowed disabled:opacity-55 ${variants[variant]} ${className}`}
    >
      {children}
    </button>
  )
}

export function LinkButton({
  to,
  children,
  variant = 'primary',
  className = '',
}: {
  to: string
  children: ReactNode
  variant?: 'primary' | 'outline'
  className?: string
}) {
  return (
    <Link
      to={to}
      className={`inline-flex min-h-10 items-center justify-center gap-2 rounded-md border px-4 py-2 text-sm font-semibold transition ${
        variant === 'primary'
          ? 'border-primary bg-primary text-white hover:bg-[#0b5962]'
          : 'border-border bg-card hover:border-primary hover:text-primary'
      } ${className}`}
    >
      {children}
    </Link>
  )
}

const positive = ['ACTIVE', 'AVAILABLE', 'COMPLETED', 'APPROVED', 'VALID', 'SENT', 'READ']
const warning = ['EXPIRING', 'EXPIRING_SOON', 'IN_PROGRESS', 'AWAITING_ASSESSMENT', 'PENDING', 'PROCESSING']
const negative = ['BLOCKED', 'EXPIRED', 'FAILED', 'REJECTED', 'REVOKED', 'OVERDUE', 'INACTIVE']

export function labelForStatus(value: string): string {
  const labels: Record<string, string> = {
    ACTIVE: 'Ativo',
    INACTIVE: 'Inativo',
    AVAILABLE: 'Liberada',
    EXPIRING: 'Vencendo',
    EXPIRING_SOON: 'Vencendo',
    BLOCKED: 'Bloqueada',
    NOT_ASSIGNED: 'Não atribuída',
    NOT_STARTED: 'Não iniciado',
    IN_PROGRESS: 'Em andamento',
    AWAITING_ASSESSMENT: 'Aguardando avaliação',
    APPROVED: 'Aprovado',
    FAILED: 'Reprovado',
    COMPLETED: 'Concluído',
    EXPIRED: 'Vencido',
    CANCELLED: 'Cancelado',
    WAIVED: 'Dispensado',
    VALID: 'Válido',
    REVOKED: 'Revogado',
    PENDING: 'Pendente',
    PROCESSING: 'Processando',
    SENT: 'Enviado',
  }
  return labels[value] || value.replaceAll('_', ' ').toLocaleLowerCase('pt-BR')
}

export function StatusBadge({ value }: { value: string }) {
  const Icon = positive.includes(value)
    ? CheckCircle2
    : warning.includes(value)
      ? Clock3
      : negative.includes(value)
        ? ShieldAlert
        : AlertCircle
  const tone = positive.includes(value)
    ? 'border-[#b7d8c8] bg-[#edf7f0] text-success'
    : warning.includes(value)
      ? 'border-[#e8d29f] bg-[#fff8e7] text-warning'
      : negative.includes(value)
        ? 'border-[#e8beb8] bg-[#fff1ef] text-destructive'
        : 'border-border bg-muted text-muted-foreground'

  return (
    <span
      className={`inline-flex items-center gap-1.5 rounded-md border px-2 py-1 font-mono text-[10px] font-semibold uppercase tracking-wide ${tone}`}
    >
      <Icon size={12} aria-hidden="true" />
      {labelForStatus(value)}
    </span>
  )
}

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <header className="mb-6 flex flex-col justify-between gap-4 border-b border-border pb-6 sm:flex-row sm:items-end">
      <div>
        {eyebrow && <p className="eyebrow text-primary">{eyebrow}</p>}
        <h1 className="display mt-2 text-3xl font-bold leading-none sm:text-4xl">{title}</h1>
        {description && <p className="mt-2 max-w-3xl text-sm leading-6 text-muted-foreground">{description}</p>}
      </div>
      {action}
    </header>
  )
}

export function LoadingState({ label = 'Carregando dados' }: { label?: string }) {
  return (
    <div className="panel space-y-4 p-6" role="status" aria-live="polite">
      <span className="sr-only">{label}</span>
      <div className="skeleton h-5 w-44 rounded" />
      <div className="skeleton h-20 w-full rounded" />
      <div className="skeleton h-20 w-full rounded" />
    </div>
  )
}

export function ErrorState({ message, retry }: { message: string; retry?: () => void }) {
  return (
    <div className="panel border-l-4 border-l-destructive p-6" role="alert">
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 shrink-0 text-destructive" size={20} />
        <div className="flex-1">
          <h2 className="display text-xl font-bold">Não foi possível carregar esta área</h2>
          <p className="mt-1 text-sm text-muted-foreground">{message}</p>
          {retry && (
            <Button variant="outline" className="mt-4" onClick={retry}>
              <RefreshCw size={15} /> Tentar novamente
            </Button>
          )}
        </div>
      </div>
    </div>
  )
}

export function EmptyState({
  title = 'Nenhum registro encontrado',
  description = 'Não há dados para exibir com os filtros atuais.',
  icon: Icon = Inbox,
  action,
}: {
  title?: string
  description?: string
  icon?: LucideIcon
  action?: ReactNode
}) {
  return (
    <div className="panel grid min-h-56 place-items-center p-8 text-center">
      <div>
        <span className="mx-auto grid size-12 place-items-center rounded-md bg-muted text-primary">
          <Icon size={22} />
        </span>
        <h2 className="display mt-4 text-2xl font-bold">{title}</h2>
        <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">{description}</p>
        {action && <div className="mt-5">{action}</div>}
      </div>
    </div>
  )
}

export function MetricCard({
  icon: Icon,
  label,
  value,
  detail,
  tone = 'primary',
}: {
  icon: LucideIcon
  label: string
  value: string | number
  detail?: string
  tone?: 'primary' | 'warning' | 'danger' | 'neutral'
}) {
  const border = {
    primary: 'border-l-primary',
    warning: 'border-l-warning',
    danger: 'border-l-destructive',
    neutral: 'border-l-[#71848b]',
  }[tone]
  return (
    <article className={`panel min-h-28 border-l-4 p-4 ${border}`}>
      <div className="flex justify-between text-xs font-semibold text-muted-foreground">
        <span>{label}</span>
        <Icon size={17} />
      </div>
      <p className="display mt-3 text-3xl font-bold leading-none">{value}</p>
      {detail && <p className="mt-2 text-xs text-muted-foreground">{detail}</p>}
    </article>
  )
}

export function BackLink({ to, children }: { to: string; children: ReactNode }) {
  return (
    <Link to={to} className="mb-5 inline-flex items-center gap-2 text-sm font-semibold text-primary hover:underline">
      <ArrowLeft size={16} /> {children}
    </Link>
  )
}

export function SearchField({
  value,
  onChange,
  placeholder,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
}) {
  return (
    <label className="relative block max-w-xl">
      <span className="sr-only">{placeholder}</span>
      <SearchX className="absolute left-3 top-3 text-muted-foreground" size={16} />
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        className="h-10 w-full rounded-md border border-border bg-card pl-9 pr-3 text-sm focus:border-primary"
      />
    </label>
  )
}

export function Pagination({
  page,
  totalPages,
  onChange,
}: {
  page: number
  totalPages: number
  onChange: (page: number) => void
}) {
  if (totalPages <= 1) return null
  return (
    <nav className="mt-5 flex items-center justify-between" aria-label="Paginação">
      <Button variant="outline" disabled={page <= 0} onClick={() => onChange(page - 1)}>
        <ArrowLeft size={15} /> Anterior
      </Button>
      <span className="text-xs text-muted-foreground">
        Página {page + 1} de {totalPages}
      </span>
      <Button variant="outline" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
        Próxima <ArrowRight size={15} />
      </Button>
    </nav>
  )
}

export function InlineLoading({ label = 'Processando' }: { label?: string }) {
  return (
    <span className="inline-flex items-center gap-2">
      <LoaderCircle className="animate-spin" size={16} />
      {label}
    </span>
  )
}

export function formatDate(value?: string | null): string {
  if (!value) return 'Sem data'
  const normalized = value.length === 10 ? `${value}T12:00:00` : value
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium' }).format(new Date(normalized))
}

export function formatDateTime(value?: string | null): string {
  if (!value) return 'Sem registro'
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value))
}
