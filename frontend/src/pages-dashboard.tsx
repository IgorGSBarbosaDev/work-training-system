import {
  AlertTriangle,
  BadgeCheck,
  BookOpen,
  CheckCircle2,
  Clock3,
  GraduationCap,
  ShieldAlert,
  Users,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from './auth'
import {
  EmptyState,
  ErrorState,
  formatDate,
  formatDateTime,
  LinkButton,
  LoadingState,
  MetricCard,
  PageHeader,
  StatusBadge,
} from './components'
import { useApiData } from './hooks'
import { AdminDashboard, Assignment, PageResponse, PersonalDashboard } from './types'

export function EmployeeDashboardPage() {
  const dashboard = useApiData<PersonalDashboard>('/me/dashboard')
  const assignments = useApiData<PageResponse<Assignment>>('/me/training-assignments?size=4&sort=dueDate,asc')

  if (dashboard.loading) return <LoadingState label="Carregando dashboard pessoal" />
  if (dashboard.error) return <ErrorState message={dashboard.error} retry={dashboard.reload} />
  if (!dashboard.data) return <EmptyState title="Dashboard indisponível" />

  const counts = dashboard.data.counts
  const next = assignments.data?.content.find((item) =>
    ['IN_PROGRESS', 'NOT_STARTED', 'AWAITING_ASSESSMENT'].includes(item.status),
  )

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Seu plano de capacitação"
        title="Próximas ações"
        description="Acompanhe treinamentos, prazos e qualificações calculados a partir dos seus dados atuais."
      />
      {assignments.loading ? (
        <LoadingState label="Carregando próxima atribuição" />
      ) : assignments.error ? (
        <ErrorState message={assignments.error} retry={assignments.reload} />
      ) : next ? (
        <section className="panel border-l-4 border-l-primary p-5 sm:p-7">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="eyebrow text-primary">Próxima ação recomendada</p>
              <h2 className="display mt-2 text-3xl font-bold">{next.training.name}</h2>
              <p className="mt-2 text-sm text-muted-foreground">
                Versão {next.trainingVersion} · prazo {formatDate(next.dueDate)}
              </p>
            </div>
            <StatusBadge value={next.status} />
          </div>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <LinkButton to={`/meu/atribuicoes/${next.id}`}>
              {next.status === 'NOT_STARTED' ? 'Ver treinamento' : 'Continuar treinamento'}
            </LinkButton>
            <Link to="/meu/atribuicoes" className="text-sm font-semibold text-primary hover:underline">
              Ver todas as atribuições
            </Link>
          </div>
        </section>
      ) : (
        <EmptyState
          title="Nenhuma ação pendente"
          description="Você não possui treinamentos pendentes ou em andamento neste momento."
          icon={CheckCircle2}
        />
      )}

      <section className="grid grid-cols-2 gap-3 xl:grid-cols-5">
        <MetricCard icon={BookOpen} label="Pendentes" value={counts.pending} detail="a iniciar" />
        <MetricCard icon={Clock3} label="Em andamento" value={counts.inProgress} tone="warning" />
        <MetricCard icon={AlertTriangle} label="Vencendo" value={counts.expiringSoon} tone="warning" />
        <MetricCard icon={ShieldAlert} label="Vencidos" value={counts.expired} tone="danger" />
        <MetricCard icon={BadgeCheck} label="Concluídos" value={counts.completed} tone="neutral" />
      </section>

      <section className="grid gap-4 lg:grid-cols-2">
        <Link
          to="/meu/qualificacoes"
          className="panel p-5 transition hover:border-primary hover:shadow-lg"
        >
          <ShieldAlert className="text-primary" size={24} />
          <h2 className="display mt-6 text-2xl font-bold">Minhas qualificações</h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Consulte atividades liberadas, vencendo ou bloqueadas e os treinamentos responsáveis.
          </p>
        </Link>
        <Link
          to="/meu/qr-code"
          className="border border-[#b8d9d6] bg-[#edf7f6] p-5 transition hover:border-primary"
        >
          <BadgeCheck className="text-primary" size={24} />
          <h2 className="display mt-6 text-2xl font-bold">Meu QR Code</h2>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Apresente seu código para validação operacional por pessoas autorizadas.
          </p>
        </Link>
      </section>
    </div>
  )
}

export function OperationsDashboardPage({ team = false }: { team?: boolean }) {
  const { role } = useAuth()
  const path = team || role !== 'ADMIN' ? '/team/dashboard' : '/admin/dashboard/overview'
  const state = useApiData<AdminDashboard>(path)

  if (state.loading) return <LoadingState label="Carregando indicadores operacionais" />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState title="Indicadores indisponíveis" />

  const data = state.data
  const completionRate = data.assignedTrainings
    ? Math.round((data.completed / data.assignedTrainings) * 100)
    : 0

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow={team ? 'Escopo autorizado da equipe' : 'Visão operacional consolidada'}
        title={team ? 'Panorama da equipe' : 'Riscos que pedem decisão'}
        description={`Indicadores gerados em ${formatDateTime(data.generatedAt)}. Os filtros e escopos são aplicados pelo backend.`}
        action={
          <LinkButton to={team ? '/equipe/relatorios' : '/admin/relatorios'} variant="outline">
            Abrir relatórios
          </LinkButton>
        }
      />
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          icon={AlertTriangle}
          label="Vencem em 30 dias"
          value={data.expiringIn30Days}
          detail="exigem acompanhamento"
          tone="warning"
        />
        <MetricCard icon={ShieldAlert} label="Vencidos" value={data.expired} detail="fora da validade" tone="danger" />
        <MetricCard icon={Users} label="Colaboradores ativos" value={data.activeEmployees} />
        <MetricCard
          icon={GraduationCap}
          label="Taxa de conclusão"
          value={`${completionRate}%`}
          detail={`${data.completed} de ${data.assignedTrainings} atribuições`}
          tone="neutral"
        />
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.15fr_.85fr]">
        <div className="panel p-5">
          <h2 className="display text-2xl font-bold">Distribuição das atribuições</h2>
          <p className="mt-1 text-sm text-muted-foreground">Dados atuais do serviço de reporting.</p>
          <div className="mt-6 space-y-4">
            <DataBar label="Não iniciadas" value={data.notStarted} total={data.assignedTrainings} />
            <DataBar label="Em andamento" value={data.inProgress} total={data.assignedTrainings} />
            <DataBar label="Concluídas" value={data.completed} total={data.assignedTrainings} />
            <DataBar label="Reprovadas" value={data.failed} total={data.assignedTrainings} danger />
          </div>
        </div>
        <div className="border border-[#b8d9d6] bg-[#edf7f6] p-5">
          <BookOpen className="text-primary" size={24} />
          <h2 className="display mt-6 text-2xl font-bold">Catálogo e atribuições</h2>
          <dl className="mt-5 grid grid-cols-2 gap-4">
            <div>
              <dt className="text-xs text-muted-foreground">Treinamentos cadastrados</dt>
              <dd className="display mt-1 text-3xl font-bold">{data.registeredTrainings}</dd>
            </div>
            <div>
              <dt className="text-xs text-muted-foreground">Atribuições totais</dt>
              <dd className="display mt-1 text-3xl font-bold">{data.assignedTrainings}</dd>
            </div>
          </dl>
          <Link
            to={team ? '/equipe/qualificacoes' : '/admin/expiracoes'}
            className="mt-6 inline-flex text-sm font-semibold text-primary hover:underline"
          >
            Acompanhar situações críticas
          </Link>
        </div>
      </section>
    </div>
  )
}

function DataBar({
  label,
  value,
  total,
  danger = false,
}: {
  label: string
  value: number
  total: number
  danger?: boolean
}) {
  const percentage = total ? Math.min(100, Math.round((value / total) * 100)) : 0
  return (
    <div>
      <div className="mb-2 flex justify-between text-xs font-semibold">
        <span>{label}</span>
        <span>{value} · {percentage}%</span>
      </div>
      <div className="h-2 bg-muted" role="progressbar" aria-label={label} aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100}>
        <div className={`h-full ${danger ? 'bg-destructive' : 'bg-primary'}`} style={{ width: `${percentage}%` }} />
      </div>
    </div>
  )
}
