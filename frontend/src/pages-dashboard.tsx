import {
  Activity as ActivityIcon,
  AlertTriangle,
  BadgeCheck,
  BookOpen,
  CheckCircle2,
  Clock3,
  GraduationCap,
  ListFilter,
  ShieldAlert,
  Users,
} from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { useAuth } from './auth'
import {
  Button,
  EmptyState,
  ErrorState,
  formatDate,
  formatDateTime,
  LinkButton,
  LoadingState,
  MetricCard,
  Pagination,
  StatusBadge,
} from './components'
import { useApiData } from './hooks'
import {
  Activity,
  ActivityDashboardItem,
  AdminDashboard,
  EmployeeDashboardItem,
  PageResponse,
  PersonalDashboard,
  Reference,
  Training,
  TrainingDashboardItem,
} from './types'

type DashboardView = 'geral' | 'treinamentos' | 'atividades' | 'colaboradores'
type CatalogOption = Reference & { unitId?: string }

const views: Array<{ id: DashboardView; label: string }> = [
  { id: 'geral', label: 'Visão geral' },
  { id: 'treinamentos', label: 'Treinamentos' },
  { id: 'atividades', label: 'Atividades' },
  { id: 'colaboradores', label: 'Colaboradores' },
]

const filterKeys = ['unitId', 'sectorId', 'jobId', 'activityId', 'trainingId', 'status', 'periodFrom', 'periodTo'] as const

export function buildDashboardPath(view: DashboardView, searchParams: URLSearchParams): string {
  const query = new URLSearchParams()
  filterKeys.forEach((key) => {
    const value = searchParams.get(key)
    if (value) query.set(key, value)
  })
  if (view !== 'geral') {
    query.set('page', searchParams.get('page') || '0')
    query.set('size', '20')
  }
  const route = view === 'geral' ? 'overview' : view === 'treinamentos' ? 'trainings' : view === 'atividades' ? 'activities' : 'employees'
  return `/admin/dashboard/${route}${query.size ? `?${query}` : ''}`
}

export function EmployeeDashboardPage() {
  const dashboard = useApiData<PersonalDashboard>('/me/dashboard')

  if (dashboard.loading) return <LoadingState label="Carregando dashboard pessoal" />
  if (dashboard.error) return <ErrorState message={dashboard.error} retry={dashboard.reload} />
  if (!dashboard.data) return <EmptyState title="Dashboard indisponível" />

  const { counts, continueTraining } = dashboard.data
  return (
    <div className="space-y-6">
      <PageHeading
        title="Próximas ações"
        description="Acompanhe treinamentos, prazos e qualificações calculados a partir dos seus dados atuais."
      />
      {continueTraining ? (
        <section className="panel p-5 sm:p-7" aria-labelledby="continue-training-title">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0">
              <h2 id="continue-training-title" className="display break-words text-3xl font-bold">{continueTraining.trainingName}</h2>
              <p className="mt-2 text-sm text-muted-foreground">{Math.round(continueTraining.progressPercentage)}% concluído · retome de onde parou</p>
            </div>
            <StatusBadge value="IN_PROGRESS" />
          </div>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <LinkButton to={`/meu/atribuicoes/${continueTraining.assignmentId}`}>Continuar treinamento</LinkButton>
            <Link to="/meu/atribuicoes" className="text-sm font-semibold text-primary hover:underline">Ver todas as atribuições</Link>
          </div>
        </section>
      ) : (
        <EmptyState title="Nenhum treinamento para retomar" description="Você não possui treinamentos em andamento neste momento." icon={CheckCircle2} />
      )}

      <section className="grid grid-cols-2 gap-3 xl:grid-cols-4">
        <MetricCard icon={BookOpen} label="Pendentes" value={counts.pending} detail="a iniciar" />
        <MetricCard icon={Clock3} label="Em andamento" value={counts.inProgress} tone="warning" />
        <MetricCard icon={AlertTriangle} label="Vencendo" value={counts.expiringSoon} tone="warning" />
        <MetricCard icon={ShieldAlert} label="Vencidos" value={counts.expired} tone="danger" />
        <MetricCard icon={BadgeCheck} label="Concluídos" value={counts.completed} tone="neutral" />
        <MetricCard icon={ActivityIcon} label="Atividades liberadas" value={counts.availableActivities} />
        <MetricCard icon={ShieldAlert} label="Atividades bloqueadas" value={counts.blockedActivities} tone="danger" />
      </section>

      <section className="grid gap-5 xl:grid-cols-2">
        <SummaryList title="Pendências prioritárias" empty="Nenhum treinamento pendente." items={dashboard.data.pendingTrainings} />
        <SummaryList title="Vencimentos" empty="Nenhum vencimento próximo." items={dashboard.data.expiringTrainings} />
      </section>

      {dashboard.data.blockedActivities.length > 0 && (
        <section className="panel p-5" aria-labelledby="blocked-activities-title">
          <h2 id="blocked-activities-title" className="display text-2xl font-bold">Atividades bloqueadas</h2>
          <div className="mt-4 divide-y divide-border">
            {dashboard.data.blockedActivities.map((item) => (
              <div key={item.activityId} className="flex flex-wrap items-start justify-between gap-3 py-4">
                <div className="min-w-0"><strong className="break-words text-sm">{item.activityName}</strong><p className="mt-1 text-xs text-muted-foreground">{item.blockingTrainings.join(', ') || 'Consulte os requisitos pendentes.'}</p></div>
                <StatusBadge value={item.status} />
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  )
}

function SummaryList({ title, empty, items }: { title: string; empty: string; items: PersonalDashboard['pendingTrainings'] }) {
  return (
    <section className="panel p-5">
      <h2 className="display text-2xl font-bold">{title}</h2>
      {!items.length ? <p className="mt-4 text-sm text-muted-foreground">{empty}</p> : (
        <div className="mt-4 divide-y divide-border">
          {items.map((item) => (
            <Link key={item.assignmentId} to={`/meu/atribuicoes/${item.assignmentId}`} className="flex min-w-0 items-center justify-between gap-4 py-4 hover:text-primary">
              <span className="min-w-0"><strong className="block truncate text-sm">{item.trainingName}</strong><span className="mt-1 block text-xs text-muted-foreground">Prazo {formatDate(item.dueDate)} · {Math.round(item.progressPercentage)}%</span></span>
              <StatusBadge value={item.status} />
            </Link>
          ))}
        </div>
      )}
    </section>
  )
}

export function OperationsDashboardPage({ team = false }: { team?: boolean }) {
  const { role } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = searchParams.get('visao') as DashboardView | null
  const view: DashboardView = team || role !== 'ADMIN' ? 'geral' : views.some((item) => item.id === requested) ? requested! : 'geral'
  const path = team || role !== 'ADMIN' ? `/team/dashboard${filterQuery(searchParams)}` : buildDashboardPath(view, searchParams)
  const state = useApiData<AdminDashboard | PageResponse<TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem>>(path)

  const units = useApiData<PageResponse<CatalogOption>>(team ? null : '/units?size=100&status=ACTIVE&sort=name,asc')
  const sectors = useApiData<PageResponse<CatalogOption>>(team ? null : '/sectors?size=100&status=ACTIVE&sort=name,asc')
  const jobs = useApiData<PageResponse<CatalogOption>>(team ? null : '/jobs?size=100&status=ACTIVE&sort=name,asc')
  const activities = useApiData<PageResponse<Activity>>(team ? null : '/activities?size=100&status=ACTIVE&sort=name,asc')
  const trainings = useApiData<PageResponse<Training>>(team ? null : '/trainings?size=100&status=ACTIVE&sort=name,asc')

  function updateParam(key: string, value: string) {
    const next = new URLSearchParams(searchParams)
		if (value) next.set(key, value)
		else next.delete(key)
    if (key !== 'page') next.delete('page')
    setSearchParams(next, { replace: true })
  }

  function changeView(nextView: DashboardView) {
    const next = new URLSearchParams(searchParams)
    next.set('visao', nextView)
    next.delete('page')
    setSearchParams(next)
  }

  if (state.loading) return <LoadingState label="Carregando indicadores operacionais" />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState title="Indicadores indisponíveis" />

  return (
    <div className="space-y-6">
      <PageHeading
        title={team ? 'Panorama da equipe' : 'Riscos que pedem decisão'}
        description={team ? 'Indicadores limitados aos colaboradores do seu escopo autorizado.' : 'Explore o panorama consolidado e aprofunde por treinamento, atividade ou colaborador.'}
        action={<LinkButton to={team ? '/equipe/relatorios' : '/admin/relatorios'} variant="outline">Abrir relatórios</LinkButton>}
      />

      {!team && (
        <nav className="overflow-x-auto border-b border-border" role="tablist" aria-label="Visões do dashboard">
          <div className="flex min-w-max gap-1">
            {views.map((item) => (
              <button key={item.id} type="button" role="tab" aria-selected={view === item.id} aria-controls={`dashboard-panel-${item.id}`} onClick={() => changeView(item.id)} className={`min-h-11 border-b-2 px-4 text-sm font-semibold ${view === item.id ? 'border-primary text-primary' : 'border-transparent text-muted-foreground hover:text-foreground'}`}>
                {item.label}
              </button>
            ))}
          </div>
        </nav>
      )}

      {!team && (
        <DashboardFilters
          params={searchParams}
          update={updateParam}
          clear={() => setSearchParams(new URLSearchParams({ visao: view }), { replace: true })}
          units={units.data?.content || []}
          sectors={sectors.data?.content || []}
          jobs={jobs.data?.content || []}
          activities={activities.data?.content || []}
          trainings={trainings.data?.content || []}
        />
      )}

      <div id={`dashboard-panel-${view}`} role="tabpanel" aria-live="polite">
        {view === 'geral' ? <Overview data={state.data as AdminDashboard} team={team} /> : (
          <DashboardResults view={view} data={state.data as PageResponse<TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem>} onPage={(page) => updateParam('page', String(page))} />
        )}
      </div>
    </div>
  )
}

function filterQuery(params: URLSearchParams) {
  const next = new URLSearchParams()
  filterKeys.forEach((key) => { const value = params.get(key); if (value) next.set(key, value) })
  return next.size ? `?${next}` : ''
}

function PageHeading({ title, description, action }: { title: string; description: string; action?: React.ReactNode }) {
  return (
    <header className="mb-6 flex flex-col justify-between gap-4 border-b border-border pb-6 sm:flex-row sm:items-end">
      <div className="min-w-0"><h1 className="display break-words text-3xl font-bold leading-none sm:text-4xl">{title}</h1><p className="mt-3 max-w-3xl text-sm leading-6 text-muted-foreground">{description}</p></div>
      {action}
    </header>
  )
}

function DashboardFilters({ params, update, clear, units, sectors, jobs, activities, trainings }: {
  params: URLSearchParams
  update: (key: string, value: string) => void
  clear: () => void
  units: CatalogOption[]
  sectors: CatalogOption[]
  jobs: CatalogOption[]
  activities: Activity[]
  trainings: Training[]
}) {
  const selectedUnit = params.get('unitId') || ''
  const visibleSectors = sectors.filter((item) => !selectedUnit || !item.unitId || item.unitId === selectedUnit)
  return (
    <section className="panel p-4" aria-labelledby="dashboard-filters-title">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h2 id="dashboard-filters-title" className="flex items-center gap-2 text-sm font-bold"><ListFilter size={17} /> Filtros compartilhados</h2>
        <Button type="button" variant="ghost" onClick={clear}>Limpar filtros</Button>
      </div>
      <div className="mt-4 grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <FilterSelect label="Unidade" value={selectedUnit} onChange={(value) => update('unitId', value)} options={units} />
        <FilterSelect label="Setor" value={params.get('sectorId') || ''} onChange={(value) => update('sectorId', value)} options={visibleSectors} />
        <FilterSelect label="Cargo" value={params.get('jobId') || ''} onChange={(value) => update('jobId', value)} options={jobs} />
        <FilterSelect label="Atividade" value={params.get('activityId') || ''} onChange={(value) => update('activityId', value)} options={activities} />
        <FilterSelect label="Treinamento" value={params.get('trainingId') || ''} onChange={(value) => update('trainingId', value)} options={trainings} />
        <label className="text-xs font-semibold">Situação<select value={params.get('status') || ''} onChange={(event) => update('status', event.target.value)} className="mt-1 h-11 w-full rounded-md border border-border bg-card px-3 text-base sm:text-sm"><option value="">Todas</option>{['NOT_STARTED','IN_PROGRESS','AWAITING_ASSESSMENT','FAILED','COMPLETED','EXPIRING_SOON','EXPIRED','AVAILABLE','EXPIRING','BLOCKED'].map((value) => <option key={value} value={value}>{value.replaceAll('_', ' ')}</option>)}</select></label>
        <FilterDate label="Período inicial" value={params.get('periodFrom') || ''} onChange={(value) => update('periodFrom', value)} />
        <FilterDate label="Período final" value={params.get('periodTo') || ''} onChange={(value) => update('periodTo', value)} />
      </div>
    </section>
  )
}

function FilterSelect({ label, value, onChange, options }: { label: string; value: string; onChange: (value: string) => void; options: Array<{ id: string; name: string }> }) {
  return <label className="text-xs font-semibold">{label}<select value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 h-11 w-full rounded-md border border-border bg-card px-3 text-base sm:text-sm"><option value="">Todos</option>{options.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
}

function FilterDate({ label, value, onChange }: { label: string; value: string; onChange: (value: string) => void }) {
  return <label className="text-xs font-semibold">{label}<input type="date" value={value} onChange={(event) => onChange(event.target.value)} className="mt-1 h-11 w-full rounded-md border border-border bg-card px-3 text-base sm:text-sm" /></label>
}

function Overview({ data, team }: { data: AdminDashboard; team: boolean }) {
  const completionRate = data.assignedTrainings ? Math.round((data.completed / data.assignedTrainings) * 100) : 0
  return (
    <div className="space-y-5">
      <p className="text-xs text-muted-foreground">Atualizado em {formatDateTime(data.generatedAt)}.</p>
      <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <MetricCard icon={AlertTriangle} label="Vencem em 30 dias" value={data.expiringIn30Days} tone="warning" />
        <MetricCard icon={ShieldAlert} label="Vencidos" value={data.expired} tone="danger" />
        <MetricCard icon={Users} label="Colaboradores com pendências" value={data.employeesWithPendingItems} tone="warning" />
        <MetricCard icon={ShieldAlert} label="Com atividades bloqueadas" value={data.employeesWithBlockedActivities} tone="danger" />
        <MetricCard icon={Users} label="Colaboradores ativos" value={data.activeEmployees} />
        <MetricCard icon={GraduationCap} label="Taxa de conclusão" value={`${completionRate}%`} detail={`${data.completed} de ${data.assignedTrainings}`} />
      </section>
      <section className="grid gap-5 xl:grid-cols-[1.15fr_.85fr]">
        <div className="panel p-5"><h2 className="display text-2xl font-bold">Distribuição das atribuições</h2><div className="mt-6 space-y-4"><DataBar label="Não iniciadas" value={data.notStarted} total={data.assignedTrainings} /><DataBar label="Em andamento" value={data.inProgress} total={data.assignedTrainings} /><DataBar label="Concluídas" value={data.completed} total={data.assignedTrainings} /><DataBar label="Reprovadas" value={data.failed} total={data.assignedTrainings} danger /></div></div>
        <div className="border border-[#b8d9d6] bg-[#edf7f6] p-5"><BookOpen className="text-primary" size={24} /><h2 className="display mt-5 text-2xl font-bold">Catálogo e atribuições</h2><dl className="mt-5 grid grid-cols-2 gap-4"><div><dt className="text-xs text-muted-foreground">Treinamentos</dt><dd className="display mt-1 text-3xl font-bold">{data.registeredTrainings}</dd></div><div><dt className="text-xs text-muted-foreground">Atribuições</dt><dd className="display mt-1 text-3xl font-bold">{data.assignedTrainings}</dd></div></dl><Link to={team ? '/equipe/qualificacoes' : '/admin/expiracoes'} className="mt-6 inline-flex text-sm font-semibold text-primary hover:underline">Acompanhar situações críticas</Link></div>
      </section>
    </div>
  )
}

function DashboardResults({ view, data, onPage }: { view: Exclude<DashboardView, 'geral'>; data: PageResponse<TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem>; onPage: (page: number) => void }) {
  if (!data.content.length) return <EmptyState title="Nenhum resultado para os filtros" description="Ajuste ou limpe os filtros para ampliar a consulta." />
  return (
    <>
      <div className="panel overflow-hidden">
        <div className="hidden overflow-x-auto md:block"><DashboardTable view={view} items={data.content} /></div>
        <div className="divide-y divide-border md:hidden">{data.content.map((item) => <DashboardCard key={dashboardKey(item)} view={view} item={item} />)}</div>
      </div>
      <Pagination page={data.page} totalPages={data.totalPages} onChange={onPage} />
    </>
  )
}

function DashboardTable({ view, items }: { view: Exclude<DashboardView, 'geral'>; items: Array<TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem> }) {
  if (view === 'treinamentos') return <table className="w-full min-w-[920px] text-left text-sm"><thead className="bg-muted text-xs"><tr><Th>Treinamento</Th><Th>Atribuídos</Th><Th>Não iniciados</Th><Th>Em andamento</Th><Th>Concluídos</Th><Th>Reprovados</Th><Th>Vencidos</Th><Th>Taxa</Th><Th>Média</Th></tr></thead><tbody className="divide-y divide-border">{(items as TrainingDashboardItem[]).map((item) => <tr key={item.trainingId}><Td><strong>{item.trainingName}</strong><small className="block text-muted-foreground">{item.trainingCode}</small></Td><Td>{item.assigned}</Td><Td>{item.notStarted}</Td><Td>{item.inProgress}</Td><Td>{item.completed}</Td><Td>{item.latestAssessmentFailed}</Td><Td>{item.expired}</Td><Td>{item.completionRate}%</Td><Td>{item.averageLatestAssessment}%</Td></tr>)}</tbody></table>
  if (view === 'atividades') return <table className="w-full min-w-[820px] text-left text-sm"><thead className="bg-muted text-xs"><tr><Th>Atividade</Th><Th>Cargos</Th><Th>Requisitos</Th><Th>Liberados</Th><Th>Vencendo</Th><Th>Bloqueados</Th><Th>Bloqueadores</Th></tr></thead><tbody className="divide-y divide-border">{(items as ActivityDashboardItem[]).map((item) => <tr key={item.activityId}><Td><strong>{item.activityName}</strong></Td><Td>{item.relatedJobs}</Td><Td>{item.requirements}</Td><Td>{item.availableEmployees}</Td><Td>{item.expiringEmployees}</Td><Td>{item.blockedEmployees}</Td><Td>{item.mainBlockingTrainings.join(', ') || '—'}</Td></tr>)}</tbody></table>
  return <table className="w-full min-w-[1020px] text-left text-sm"><thead className="bg-muted text-xs"><tr><Th>Colaborador</Th><Th>Estrutura</Th><Th>Obrigatórios</Th><Th>Opcionais</Th><Th>Progresso</Th><Th>Média</Th><Th>Conclusões</Th><Th>Vencimentos</Th><Th>Atividades</Th></tr></thead><tbody className="divide-y divide-border">{(items as EmployeeDashboardItem[]).map((item) => <tr key={item.employeeId}><Td><strong>{item.employeeName}</strong><small className="block text-muted-foreground">{item.registration}</small></Td><Td>{item.unitName}<small className="block text-muted-foreground">{item.sectorName} · {item.jobName}</small></Td><Td>{item.mandatoryTrainings}</Td><Td>{item.optionalTrainings}</Td><Td>{item.averageProgress}%</Td><Td>{item.averageLatestAssessment}%</Td><Td>{item.completions}</Td><Td>{item.expirations}</Td><Td>{item.availableActivities} liberadas · {item.blockedActivities} bloqueadas</Td></tr>)}</tbody></table>
}

function DashboardCard({ view, item }: { view: Exclude<DashboardView, 'geral'>; item: TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem }) {
  if (view === 'treinamentos') { const value = item as TrainingDashboardItem; return <article className="p-5"><h3 className="display break-words text-xl font-bold">{value.trainingName}</h3><p className="mt-1 text-xs text-muted-foreground">{value.trainingCode}</p><dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><Stat label="Atribuídos" value={value.assigned} /><Stat label="Concluídos" value={value.completed} /><Stat label="Reprovados" value={value.latestAssessmentFailed} /><Stat label="Taxa" value={`${value.completionRate}%`} /></dl></article> }
  if (view === 'atividades') { const value = item as ActivityDashboardItem; return <article className="p-5"><h3 className="display break-words text-xl font-bold">{value.activityName}</h3><dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><Stat label="Requisitos" value={value.requirements} /><Stat label="Liberados" value={value.availableEmployees} /><Stat label="Vencendo" value={value.expiringEmployees} /><Stat label="Bloqueados" value={value.blockedEmployees} /></dl><p className="mt-4 text-xs text-muted-foreground">Bloqueadores: {value.mainBlockingTrainings.join(', ') || 'nenhum'}</p></article> }
  const value = item as EmployeeDashboardItem; return <article className="p-5"><h3 className="display break-words text-xl font-bold">{value.employeeName}</h3><p className="mt-1 text-xs text-muted-foreground">{value.registration} · {value.unitName} / {value.sectorName}</p><dl className="mt-4 grid grid-cols-2 gap-3 text-sm"><Stat label="Progresso" value={`${value.averageProgress}%`} /><Stat label="Média" value={`${value.averageLatestAssessment}%`} /><Stat label="Conclusões" value={value.completions} /><Stat label="Bloqueios" value={value.blockedActivities} /></dl></article>
}

function dashboardKey(item: TrainingDashboardItem | ActivityDashboardItem | EmployeeDashboardItem) { return 'trainingId' in item ? item.trainingId : 'activityId' in item ? item.activityId : item.employeeId }
function Th({ children }: { children: React.ReactNode }) { return <th scope="col" className="px-4 py-3 font-semibold">{children}</th> }
function Td({ children }: { children: React.ReactNode }) { return <td className="px-4 py-4 align-top tabular-nums">{children}</td> }
function Stat({ label, value }: { label: string; value: string | number }) { return <div><dt className="text-xs text-muted-foreground">{label}</dt><dd className="mt-1 font-semibold tabular-nums">{value}</dd></div> }

function DataBar({ label, value, total, danger = false }: { label: string; value: number; total: number; danger?: boolean }) {
  const percentage = total ? Math.min(100, Math.round((value / total) * 100)) : 0
  return <div><div className="mb-2 flex justify-between text-xs font-semibold"><span>{label}</span><span className="tabular-nums">{value} · {percentage}%</span></div><div className="h-2 bg-muted" role="progressbar" aria-label={label} aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100}><div className={`h-full ${danger ? 'bg-destructive' : 'bg-primary'}`} style={{ width: `${percentage}%` }} /></div></div>
}
