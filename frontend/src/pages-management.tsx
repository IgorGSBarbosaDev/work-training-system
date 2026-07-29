import { FormEvent, useState } from 'react'
import {
  Activity as ActivityIcon,
  ArrowRight,
  BadgeCheck,
  Bell,
  BookOpen,
  BriefcaseBusiness,
  Building2,
  CheckCircle2,
  ClipboardList,
  Download,
  Filter,
  KeyRound,
  Mail,
  Plus,
  QrCode,
  RefreshCw,
  ShieldAlert,
  UserCog,
  Users,
} from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from './api'
import {
  BackLink,
  Button,
  EmptyState,
  ErrorState,
  formatDate,
  formatDateTime,
  InlineLoading,
  LinkButton,
  LoadingState,
  PageHeader,
  Pagination,
  SearchField,
  StatusBadge,
} from './components'
import { useApiData } from './hooks'
import {
  Activity,
  Assignment,
  Employee,
  Expiration,
  PageResponse,
  Qualification,
  Training,
} from './types'
import { apiErrorMessage } from './pages-auth'

type CatalogItem = { id: string; name: string; code?: string; status: string; description?: string }
type Unit = CatalogItem
type Sector = CatalogItem & { unitId: string }
type Job = CatalogItem

function withPage(path: string, page: number, search?: string): string {
  const separator = path.includes('?') ? '&' : '?'
  const query = new URLSearchParams({ page: String(page), size: '15' })
  if (search) query.set('search', search)
  return `${path}${separator}${query}`
}

export function EmployeesPage({ team = false }: { team?: boolean }) {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const path = team ? withPage('/team/employees', page) : withPage('/employees', page, search)
  const state = useApiData<PageResponse<Employee>>(path)

  return (
    <div>
      <PageHeader
        eyebrow={team ? 'Escopo autorizado' : 'Pessoas e estrutura'}
        title={team ? 'Colaboradores da equipe' : 'Colaboradores'}
        description={
          team
            ? 'A listagem respeita os grants de unidade, setor e colaborador definidos no backend.'
            : 'Cadastre, localize e acompanhe colaboradores sem perder o histórico de treinamentos.'
        }
        action={!team ? <LinkButton to="/admin/colaboradores/novo"><Plus size={16} /> Novo colaborador</LinkButton> : undefined}
      />
      {!team && (
        <div className="mb-5">
          <SearchField
            value={search}
            onChange={(value) => {
              setSearch(value)
              setPage(0)
            }}
            placeholder="Buscar por nome, matrícula ou e-mail"
          />
        </div>
      )}
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState icon={Users} title="Nenhum colaborador encontrado" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((employee) => (
              <Link
                key={employee.id}
                to={team ? `/equipe/colaboradores/${employee.id}` : `/admin/colaboradores/${employee.id}`}
                className="flex flex-wrap items-center gap-4 p-4 transition hover:bg-muted/40"
              >
                <span className="grid size-10 shrink-0 place-items-center rounded-md bg-primary/10 text-sm font-bold text-primary">
                  {initials(employee.name)}
                </span>
                <span className="min-w-52 flex-1">
                  <strong className="block text-sm">{employee.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">
                    {employee.registration} · {employee.job.name}
                  </span>
                </span>
                <span className="hidden text-right text-xs text-muted-foreground md:block">
                  {employee.sector.name}
                  <br />
                  {employee.unit.name}
                </span>
                <StatusBadge value={employee.status} />
                <ArrowRight className="text-muted-foreground" size={17} />
              </Link>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function EmployeeDetailPage({ team = false }: { team?: boolean }) {
  const { employeeId = '' } = useParams()
  const employee = useApiData<Employee>(employeeId ? `/employees/${employeeId}` : null)
  const qualifications = useApiData<PageResponse<Qualification>>(
    employeeId ? `/employees/${employeeId}/qualifications?size=50` : null,
  )

  if (employee.loading) return <LoadingState />
  if (employee.error) return <ErrorState message={employee.error} retry={employee.reload} />
  if (!employee.data) return <EmptyState title="Colaborador não encontrado" />

  return (
    <div>
      <BackLink to={team ? '/equipe/colaboradores' : '/admin/colaboradores'}>Colaboradores</BackLink>
      <PageHeader
        eyebrow={`${employee.data.registration} · ${employee.data.unit.name}`}
        title={employee.data.name}
        description={`${employee.data.job.name} · ${employee.data.sector.name}`}
        action={<StatusBadge value={employee.data.status} />}
      />
      <div className="grid gap-6 xl:grid-cols-[.72fr_1.28fr]">
        <aside className="panel p-5">
          <span className="grid size-20 place-items-center rounded-md bg-primary/10 text-2xl font-bold text-primary">
            {initials(employee.data.name)}
          </span>
          <dl className="mt-6 space-y-4">
            <Info label="E-mail" value={employee.data.email} />
            <Info label="Cargo" value={employee.data.job.name} />
            <Info label="Setor" value={employee.data.sector.name} />
            <Info label="Unidade" value={employee.data.unit.name} />
          </dl>
        </aside>
        <section className="panel">
          <header className="border-b border-border p-5">
            <h2 className="display text-2xl font-bold">Qualificações operacionais</h2>
            <p className="mt-1 text-sm text-muted-foreground">Situações calculadas no backend para este colaborador.</p>
          </header>
          {qualifications.loading ? (
            <div className="p-5 text-sm text-muted-foreground">Carregando qualificações…</div>
          ) : qualifications.error ? (
            <div className="p-5 text-sm text-destructive">{qualifications.error}</div>
          ) : !qualifications.data?.content.length ? (
            <div className="p-5 text-sm text-muted-foreground">Nenhuma qualificação registrada.</div>
          ) : (
            <div className="divide-y divide-border">
              {qualifications.data.content.map((qualification) => (
                <div key={qualification.id} className="flex flex-wrap items-start gap-4 p-5">
                  <span className="grid size-9 place-items-center bg-muted text-primary">
                    {qualification.status === 'AVAILABLE' ? <CheckCircle2 size={18} /> : <ShieldAlert size={18} />}
                  </span>
                  <div className="min-w-52 flex-1">
                    <strong className="text-sm">{qualification.activity.name}</strong>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {qualification.blockingReasons.map((reason) => reason.trainingName || reason.type).join(', ') ||
                        'Todos os requisitos registrados estão atendidos.'}
                    </p>
                  </div>
                  <StatusBadge value={qualification.status} />
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  )
}

export function CreateEmployeePage() {
  const units = useApiData<PageResponse<Unit>>('/units?size=100&status=ACTIVE&sort=name,asc')
  const sectors = useApiData<PageResponse<Sector>>('/sectors?size=100&status=ACTIVE&sort=name,asc')
  const jobs = useApiData<PageResponse<Job>>('/jobs?size=100&status=ACTIVE&sort=name,asc')
  const [form, setForm] = useState({
    name: '',
    registration: '',
    email: '',
    unitId: '',
    sectorId: '',
    jobId: '',
    status: 'ACTIVE',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const availableSectors = sectors.data?.content.filter((sector) => !form.unitId || sector.unitId === form.unitId) || []

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const employee = await api<Employee>('/employees', { method: 'POST', body: JSON.stringify(form) })
      toast.success('Colaborador cadastrado.')
      navigate(`/admin/colaboradores/${employee.id}`)
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl">
      <BackLink to="/admin/colaboradores">Colaboradores</BackLink>
      <PageHeader
        eyebrow="Novo cadastro"
        title="Cadastrar colaborador"
        description="Matrícula, e-mail e referências organizacionais serão validados pelo backend."
      />
      <form onSubmit={submit} className="panel">
        <div className="grid gap-5 p-5 sm:grid-cols-2 sm:p-7">
          <FormField label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} maxLength={150} />
          <FormField
            label="Matrícula"
            value={form.registration}
            onChange={(registration) => setForm({ ...form, registration })}
            maxLength={50}
          />
          <FormField
            label="E-mail corporativo"
            type="email"
            value={form.email}
            onChange={(email) => setForm({ ...form, email })}
            maxLength={254}
          />
          <SelectField
            label="Unidade"
            value={form.unitId}
            onChange={(unitId) => setForm({ ...form, unitId, sectorId: '' })}
            options={units.data?.content || []}
            loading={units.loading}
          />
          <SelectField
            label="Setor"
            value={form.sectorId}
            onChange={(sectorId) => setForm({ ...form, sectorId })}
            options={availableSectors}
            loading={sectors.loading}
          />
          <SelectField
            label="Cargo"
            value={form.jobId}
            onChange={(jobId) => setForm({ ...form, jobId })}
            options={jobs.data?.content || []}
            loading={jobs.loading}
          />
        </div>
        <FormFooter submitting={submitting} error={error} label="Cadastrar colaborador" />
      </form>
    </div>
  )
}

export function TrainingsPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const state = useApiData<PageResponse<Training>>(withPage('/trainings', page, search))

  return (
    <div>
      <PageHeader
        eyebrow="Catálogo e versões"
        title="Treinamentos"
        description="Gerencie treinamentos e suas versões preservando o histórico de conteúdo concluído."
        action={<LinkButton to="/admin/treinamentos/novo"><Plus size={16} /> Novo treinamento</LinkButton>}
      />
      <div className="mb-5">
        <SearchField value={search} onChange={(value) => { setSearch(value); setPage(0) }} placeholder="Buscar treinamento" />
      </div>
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState icon={BookOpen} title="Nenhum treinamento cadastrado" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((training) => (
              <Link key={training.id} to={`/admin/treinamentos/${training.id}`} className="flex flex-wrap items-center gap-4 p-4 hover:bg-muted/40">
                <span className="grid size-10 place-items-center bg-primary/10 text-primary"><BookOpen size={19} /></span>
                <span className="min-w-52 flex-1">
                  <strong className="block text-sm">{training.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">{training.code} · {training.category || 'Sem categoria'}</span>
                </span>
                {training.regulatoryStandard && <span className="rounded-md border border-border px-2 py-1 text-[10px] font-semibold">NR</span>}
                <StatusBadge value={training.status} />
                <ArrowRight size={17} className="text-muted-foreground" />
              </Link>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function CreateTrainingPage() {
  const [form, setForm] = useState({
    name: '',
    code: '',
    description: '',
    category: '',
    isRegulatoryStandard: false,
    status: 'ACTIVE',
    workloadMinutes: 60,
    validityType: 'MONTHS',
    validityValue: 24,
    passingScore: 70,
    maxAttempts: 3,
    retryIntervalMinutes: 0,
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const payload = {
        name: form.name,
        code: form.code,
        description: form.description,
        category: form.category,
        isRegulatoryStandard: form.isRegulatoryStandard,
        status: form.status,
        initialVersion: {
          workloadMinutes: form.workloadMinutes,
          validityType: form.validityType,
          validityValue: form.validityType === 'INDEFINITE' ? null : form.validityValue,
          passingScore: form.passingScore,
          maxAttempts: form.maxAttempts,
          retryIntervalMinutes: form.retryIntervalMinutes,
        },
      }
      const training = await api<Training>('/trainings', { method: 'POST', body: JSON.stringify(payload) })
      toast.success('Treinamento e versão inicial criados.')
      navigate(`/admin/treinamentos/${training.id}`)
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl">
      <BackLink to="/admin/treinamentos">Treinamentos</BackLink>
      <PageHeader
        eyebrow="Informações e versão inicial"
        title="Novo treinamento"
        description="A nota mínima não pode ser inferior a 70% e alterações publicadas devem gerar nova versão."
      />
      <form onSubmit={submit} className="panel">
        <div className="grid gap-5 p-5 sm:grid-cols-2 sm:p-7">
          <FormField label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} maxLength={150} />
          <FormField
            label="Código"
            value={form.code}
            onChange={(code) => setForm({ ...form, code: code.toUpperCase() })}
            maxLength={50}
            pattern="[A-Za-z0-9][A-Za-z0-9_-]*"
          />
          <FormField label="Categoria" value={form.category} onChange={(category) => setForm({ ...form, category })} required={false} />
          <FormField
            label="Carga horária (minutos)"
            type="number"
            value={String(form.workloadMinutes)}
            onChange={(value) => setForm({ ...form, workloadMinutes: Number(value) })}
            min={1}
          />
          <SelectNative
            label="Tipo de validade"
            value={form.validityType}
            onChange={(validityType) => setForm({ ...form, validityType })}
            options={[
              ['DAYS', 'Dias'],
              ['MONTHS', 'Meses'],
              ['INDEFINITE', 'Indeterminada'],
            ]}
          />
          {form.validityType !== 'INDEFINITE' && (
            <FormField
              label="Prazo de validade"
              type="number"
              value={String(form.validityValue)}
              onChange={(value) => setForm({ ...form, validityValue: Number(value) })}
              min={1}
            />
          )}
          <FormField
            label="Nota mínima (%)"
            type="number"
            value={String(form.passingScore)}
            onChange={(value) => setForm({ ...form, passingScore: Number(value) })}
            min={70}
            max={100}
          />
          <FormField
            label="Máximo de tentativas"
            type="number"
            value={String(form.maxAttempts)}
            onChange={(value) => setForm({ ...form, maxAttempts: Number(value) })}
            min={1}
          />
          <label className="flex items-center gap-3 border border-border p-4 sm:col-span-2">
            <input
              type="checkbox"
              checked={form.isRegulatoryStandard}
              onChange={(event) => setForm({ ...form, isRegulatoryStandard: event.target.checked })}
              className="size-4 accent-[#0f6973]"
            />
            <span>
              <strong className="block text-sm">Norma regulamentadora (NR)</strong>
              <span className="text-xs text-muted-foreground">Identifica o treinamento nas consultas de conformidade.</span>
            </span>
          </label>
          <label className="sm:col-span-2">
            <span className="mb-1.5 block text-sm font-semibold">Descrição</span>
            <textarea
              value={form.description}
              onChange={(event) => setForm({ ...form, description: event.target.value })}
              maxLength={2000}
              rows={4}
              className="w-full rounded-md border border-border bg-card p-3 text-sm"
            />
          </label>
        </div>
        <FormFooter submitting={submitting} error={error} label="Criar treinamento" />
      </form>
    </div>
  )
}

type TrainingVersion = {
  id: string
  versionNumber: number
  status: string
  workloadMinutes: number
  validityType: string
  validityValue?: number | null
  passingScore: number
}

export function TrainingDetailPage() {
  const { trainingId = '' } = useParams()
  const training = useApiData<Training>(trainingId ? `/trainings/${trainingId}` : null)
  const versions = useApiData<TrainingVersion[]>(trainingId ? `/trainings/${trainingId}/versions` : null)

  if (training.loading || versions.loading) return <LoadingState />
  if (training.error) return <ErrorState message={training.error} retry={training.reload} />
  if (versions.error) return <ErrorState message={versions.error} retry={versions.reload} />
  if (!training.data) return <EmptyState title="Treinamento não encontrado" />

  return (
    <div>
      <BackLink to="/admin/treinamentos">Treinamentos</BackLink>
      <PageHeader
        eyebrow={`${training.data.code} · ${training.data.category || 'Sem categoria'}`}
        title={training.data.name}
        description={training.data.description}
        action={<StatusBadge value={training.data.status} />}
      />
      {!versions.data?.length ? (
        <EmptyState title="Nenhuma versão cadastrada" />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {versions.data.map((version) => (
            <Link
              key={version.id}
              to={`/admin/treinamentos/${trainingId}/versoes/${version.id}/editor`}
              className="panel p-5 transition hover:border-primary"
            >
              <div className="flex items-start justify-between gap-3">
                <span className="eyebrow text-primary">Versão {version.versionNumber}</span>
                <StatusBadge value={version.status} />
              </div>
              <dl className="mt-5 grid grid-cols-2 gap-4">
                <Info label="Carga horária" value={`${version.workloadMinutes} min`} />
                <Info label="Nota mínima" value={`${version.passingScore}%`} />
                <Info label="Validade" value={version.validityType} />
              </dl>
              <span className="mt-5 inline-flex items-center gap-2 text-sm font-semibold text-primary">
                Abrir editor <ArrowRight size={15} />
              </span>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

type Module = { id: string; title: string; description: string; order: number; status: string }

export function TrainingVersionEditorPage() {
  const { trainingId = '', versionId = '' } = useParams()
  const version = useApiData<TrainingVersion>(versionId ? `/training-versions/${versionId}` : null)
  const modules = useApiData<Module[]>(versionId ? `/training-versions/${versionId}/modules` : null)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)

  async function createModule(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    try {
      await api(`/training-versions/${versionId}/modules`, {
        method: 'POST',
        body: JSON.stringify({
          title,
          description,
          order: (modules.data?.length || 0) + 1,
          status: 'ACTIVE',
        }),
      })
      setTitle('')
      setDescription('')
      modules.reload()
      toast.success('Módulo adicionado ao rascunho.')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  async function publish() {
    try {
      await api(`/training-versions/${versionId}/publish`, { method: 'POST' })
      version.reload()
      toast.success('Versão publicada.')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  if (version.loading || modules.loading) return <LoadingState />
  if (version.error) return <ErrorState message={version.error} retry={version.reload} />
  if (modules.error) return <ErrorState message={modules.error} retry={modules.reload} />
  if (!version.data) return <EmptyState title="Versão não encontrada" />

  const draft = version.data.status === 'DRAFT'
  return (
    <div>
      <BackLink to={`/admin/treinamentos/${trainingId}`}>Detalhe do treinamento</BackLink>
      <PageHeader
        eyebrow="Editor de conteúdo"
        title={`Versão ${version.data.versionNumber}`}
        description="Organize módulos e revise o conteúdo antes de publicar. Versões publicadas permanecem imutáveis."
        action={draft ? <Button onClick={publish}>Publicar versão</Button> : <StatusBadge value={version.data.status} />}
      />
      <div className="grid gap-6 xl:grid-cols-[1.2fr_.8fr]">
        <section className="panel">
          <header className="border-b border-border p-5">
            <h2 className="display text-2xl font-bold">Módulos</h2>
          </header>
          {!modules.data?.length ? (
            <div className="p-5 text-sm text-muted-foreground">Nenhum módulo cadastrado.</div>
          ) : (
            <div className="divide-y divide-border">
              {modules.data.map((module) => (
                <div key={module.id} className="flex items-center gap-4 p-5">
                  <span className="grid size-9 place-items-center bg-muted font-mono text-xs text-primary">{module.order}</span>
                  <span className="min-w-0 flex-1">
                    <strong className="block text-sm">{module.title}</strong>
                    <span className="mt-1 block text-xs text-muted-foreground">{module.description || 'Sem descrição'}</span>
                  </span>
                  <StatusBadge value={module.status} />
                </div>
              ))}
            </div>
          )}
        </section>
        <form onSubmit={createModule} className="panel h-fit p-5">
          <p className="eyebrow text-primary">Novo módulo</p>
          <h2 className="display mt-2 text-2xl font-bold">Adicionar ao rascunho</h2>
          <div className="mt-5 space-y-4">
            <FormField label="Título" value={title} onChange={setTitle} maxLength={150} disabled={!draft} />
            <label>
              <span className="mb-1.5 block text-sm font-semibold">Descrição</span>
              <textarea
                value={description}
                onChange={(event) => setDescription(event.target.value)}
                rows={3}
                disabled={!draft}
                className="w-full rounded-md border border-border p-3 text-sm disabled:bg-muted"
              />
            </label>
            <Button disabled={!draft || submitting} className="w-full">
              {submitting ? <InlineLoading label="Adicionando" /> : <><Plus size={16} /> Adicionar módulo</>}
            </Button>
          </div>
        </form>
      </div>
    </div>
  )
}

export function ActivitiesPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const state = useApiData<PageResponse<Activity>>(withPage('/activities', page, search))

  return (
    <div>
      <PageHeader
        eyebrow="Requisitos operacionais"
        title="Atividades"
        description="Relacione atividades a cargos e treinamentos obrigatórios."
        action={<LinkButton to="/admin/atividades/nova"><Plus size={16} /> Nova atividade</LinkButton>}
      />
      <div className="mb-5"><SearchField value={search} onChange={setSearch} placeholder="Buscar atividade" /></div>
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={BriefcaseBusiness} title="Nenhuma atividade cadastrada" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((activity) => (
              <div key={activity.id} className="flex flex-wrap items-center gap-4 p-4">
                <span className="grid size-10 place-items-center bg-primary/10 text-primary"><BriefcaseBusiness size={19} /></span>
                <span className="min-w-52 flex-1">
                  <strong className="block text-sm">{activity.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">{activity.description || 'Sem descrição'}</span>
                </span>
                <StatusBadge value={activity.status} />
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function CreateActivityPage() {
  const [name, setName] = useState('')
  const [description, setDescription] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      await api('/activities', { method: 'POST', body: JSON.stringify({ name, description, status: 'ACTIVE' }) })
      toast.success('Atividade cadastrada.')
      navigate('/admin/atividades')
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <BackLink to="/admin/atividades">Atividades</BackLink>
      <PageHeader eyebrow="Novo requisito operacional" title="Cadastrar atividade" />
      <form onSubmit={submit} className="panel">
        <div className="space-y-5 p-5 sm:p-7">
          <FormField label="Nome" value={name} onChange={setName} maxLength={150} />
          <label>
            <span className="mb-1.5 block text-sm font-semibold">Descrição</span>
            <textarea value={description} onChange={(event) => setDescription(event.target.value)} rows={5} maxLength={2000} className="w-full rounded-md border border-border p-3 text-sm" />
          </label>
        </div>
        <FormFooter submitting={submitting} error={error} label="Criar atividade" />
      </form>
    </div>
  )
}

export function AssignmentsAdminPage() {
  const [page, setPage] = useState(0)
  const state = useApiData<PageResponse<Assignment>>(`/training-assignments?page=${page}&size=15`)
  return (
    <div>
      <PageHeader
        eyebrow="Distribuição de capacitações"
        title="Atribuições"
        description="Acompanhe status, origem, prioridade e prazo preservados em cada atribuição."
        action={<LinkButton to="/admin/atribuicoes/nova"><Plus size={16} /> Nova atribuição</LinkButton>}
      />
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={ClipboardList} title="Nenhuma atribuição encontrada" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((assignment) => (
              <div key={assignment.id} className="flex flex-wrap items-center gap-4 p-4">
                <span className="grid size-10 place-items-center bg-primary/10 text-primary"><ClipboardList size={18} /></span>
                <span className="min-w-56 flex-1">
                  <strong className="block text-sm">{assignment.training.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">
                    {assignment.employee.name} · versão {assignment.trainingVersion}
                  </span>
                </span>
                <span className="text-xs text-muted-foreground">{formatDate(assignment.dueDate)}</span>
                <StatusBadge value={assignment.status} />
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function CreateAssignmentPage() {
  const employees = useApiData<PageResponse<Employee>>('/employees?size=100&status=ACTIVE&sort=name,asc')
  const trainings = useApiData<PageResponse<Training>>('/trainings?size=100&status=ACTIVE&sort=name,asc')
  const [form, setForm] = useState({
    employeeId: '',
    trainingId: '',
    dueDate: '',
    priority: 'NORMAL',
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    const key = crypto.randomUUID()
    setSubmitting(true)
    setError('')
    try {
      await api('/training-assignments', {
        method: 'POST',
        headers: { 'Idempotency-Key': key },
        body: JSON.stringify({
          ...form,
          origin: 'EMPLOYEE',
          sourceReferenceId: form.employeeId,
          dueDate: form.dueDate || null,
          idempotencyKey: key,
        }),
      })
      toast.success('Treinamento atribuído.')
      navigate('/admin/atribuicoes')
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-4xl">
      <BackLink to="/admin/atribuicoes">Atribuições</BackLink>
      <PageHeader
        eyebrow="Atribuição individual"
        title="Nova atribuição"
        description="A versão publicada vigente será resolvida pelo backend quando nenhuma versão específica for informada."
      />
      <form onSubmit={submit} className="panel">
        <div className="grid gap-5 p-5 sm:grid-cols-2 sm:p-7">
          <SelectField
            label="Colaborador"
            value={form.employeeId}
            onChange={(employeeId) => setForm({ ...form, employeeId })}
            options={employees.data?.content || []}
            loading={employees.loading}
          />
          <SelectField
            label="Treinamento"
            value={form.trainingId}
            onChange={(trainingId) => setForm({ ...form, trainingId })}
            options={trainings.data?.content || []}
            loading={trainings.loading}
          />
          <FormField
            label="Prazo para conclusão"
            type="date"
            value={form.dueDate}
            onChange={(dueDate) => setForm({ ...form, dueDate })}
            min={new Date().toISOString().slice(0, 10)}
            required={false}
          />
          <SelectNative
            label="Prioridade"
            value={form.priority}
            onChange={(priority) => setForm({ ...form, priority })}
            options={[['NORMAL', 'Normal'], ['HIGH', 'Alta']]}
          />
        </div>
        <FormFooter submitting={submitting} error={error} label="Criar atribuição" />
      </form>
    </div>
  )
}

export function ExpirationsPage() {
  const [page, setPage] = useState(0)
  const state = useApiData<PageResponse<Expiration>>(`/expirations?page=${page}&size=15&sort=expirationDate,asc`)
  const [recalculating, setRecalculating] = useState(false)

  async function recalculate() {
    setRecalculating(true)
    try {
      await api('/expirations/recalculate', { method: 'POST' })
      toast.success('Recálculo de vencimentos solicitado.')
      state.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setRecalculating(false)
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Controle de validade"
        title="Expirações e reciclagens"
        description="Identifique conclusões vencidas ou próximas do vencimento e seus impactos."
        action={<Button onClick={recalculate} disabled={recalculating}>{recalculating ? <InlineLoading label="Recalculando" /> : <><RefreshCw size={16} /> Atualizar dados</>}</Button>}
      />
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={CheckCircle2} title="Nenhuma expiração encontrada" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((expiration) => (
              <div key={expiration.completionId} className="grid gap-3 p-4 sm:grid-cols-[1fr_1fr_auto] sm:items-center">
                <div>
                  <p className="text-xs text-muted-foreground">Colaborador</p>
                  <p className="mt-1 break-all text-sm font-semibold">{expiration.employeeId}</p>
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">Validade</p>
                  <p className="mt-1 text-sm">{formatDate(expiration.expirationDate)}</p>
                </div>
                <StatusBadge value={expiration.status} />
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

type QrVerification = {
  employee: { name: string; registration: string; job: string }
  trainings: Array<{ name: string; code: string; isRegulatoryStandard: boolean; completedAt: string; expiresAt?: string; status: string }>
  regulatoryStandards: Array<{ name: string; code: string; completedAt: string; expiresAt?: string; status: string }>
  activities: Array<{ name: string; status: string; pendingTrainings: string[] }>
}

export function QrVerificationPage() {
  const [token, setToken] = useState('')
  const navigate = useNavigate()
  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        eyebrow="Consulta autenticada"
        title="Verificar QR Code"
        description="A consulta registra auditoria e exibe apenas dados necessários para validação operacional."
      />
      <div className="grid gap-6 lg:grid-cols-[1fr_.72fr]">
        <form
          className="panel p-5 sm:p-7"
          onSubmit={(event) => {
            event.preventDefault()
            navigate(`/equipe/verificar-qr/${encodeURIComponent(token.trim())}`)
          }}
        >
          <span className="grid size-11 place-items-center border border-primary/25 bg-primary/5 text-primary"><QrCode size={22} /></span>
          <h2 className="display mt-5 text-3xl font-bold">Inserir token seguro</h2>
          <p className="mt-2 text-sm text-muted-foreground">Use o token lido do QR Code do colaborador.</p>
          <label className="relative mt-7 block">
            <span className="sr-only">Token do QR Code</span>
            <KeyRound className="absolute left-3 top-3 text-muted-foreground" size={16} />
            <input
              required
              value={token}
              onChange={(event) => setToken(event.target.value)}
              className="h-11 w-full rounded-md border border-border bg-card pl-9 pr-3 font-mono text-sm"
              placeholder="Token"
            />
          </label>
          <Button className="mt-4 w-full">Validar <ArrowRight size={16} /></Button>
        </form>
        <aside className="border border-[#b8d9d6] bg-[#edf7f6] p-5 sm:p-7">
          <p className="eyebrow text-primary">Consulta responsável</p>
          <h2 className="display mt-3 text-2xl font-bold">Dados mínimos, decisão clara.</h2>
          <ul className="mt-6 space-y-4 text-sm text-muted-foreground">
            <li>Nome, matrícula e cargo para confirmação funcional.</li>
            <li>Treinamentos, NRs, validades e atividades atuais.</li>
            <li>Sem CPF, endereço, telefone ou informações médicas.</li>
          </ul>
        </aside>
      </div>
    </div>
  )
}

export function QrVerificationResultPage() {
  const { token = '' } = useParams()
  const state = useApiData<QrVerification>(token ? `/qr-verifications/${encodeURIComponent(token)}` : null)
  if (state.loading) return <LoadingState />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState title="QR Code não encontrado" />

  return (
    <div>
      <BackLink to="/equipe/verificar-qr">Nova verificação</BackLink>
      <section className="panel">
        <header className="border-b border-border p-5 sm:p-7">
          <p className="eyebrow text-primary">Verificação autenticada</p>
          <h1 className="display mt-2 text-3xl font-bold">{state.data.employee.name}</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            {state.data.employee.registration} · {state.data.employee.job}
          </p>
        </header>
        <div className="grid gap-7 p-5 sm:p-7 xl:grid-cols-[1.1fr_.9fr]">
          <section>
            <h2 className="display text-2xl font-bold">Autorização operacional</h2>
            <div className="mt-5 space-y-3">
              {state.data.activities.map((activity) => (
                <article key={activity.name} className="border border-border p-4">
                  <div className="flex items-start justify-between gap-3">
                    <strong>{activity.name}</strong>
                    <StatusBadge value={activity.status} />
                  </div>
                  {!!activity.pendingTrainings.length && (
                    <p className="mt-3 text-sm text-destructive">Pendências: {activity.pendingTrainings.join(', ')}</p>
                  )}
                </article>
              ))}
            </div>
          </section>
          <section className="xl:border-l xl:border-border xl:pl-6">
            <h2 className="display text-2xl font-bold">Treinamentos realizados</h2>
            <div className="mt-5 divide-y divide-border border-y border-border">
              {state.data.trainings.map((training, index) => (
                <div key={`${training.code}-${index}`} className="py-4">
                  <div className="flex items-start justify-between gap-3">
                    <strong className="text-sm">{training.name}</strong>
                    <StatusBadge value={training.status} />
                  </div>
                  <p className="mt-1 text-xs text-muted-foreground">
                    Concluído em {formatDate(training.completedAt)} · vence em {formatDate(training.expiresAt)}
                  </p>
                </div>
              ))}
            </div>
          </section>
        </div>
      </section>
      <p className="mt-5 border-l-4 border-primary bg-[#edf7f6] p-4 text-sm text-[#1b6066]">
        A qualificação é baseada nos treinamentos registrados e não substitui liberações médicas, operacionais ou
        legais externas.
      </p>
    </div>
  )
}

export function QualificationsManagementPage({ team = false }: { team?: boolean }) {
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('')
  const endpoint = team ? '/reports/qualifications' : '/qualifications'
  const state = useApiData<PageResponse<Qualification>>(
    `${endpoint}?page=${page}&size=15${status ? `&status=${status}` : ''}`,
  )
  return (
    <div>
      <PageHeader
        eyebrow={team ? 'Escopo autorizado' : 'Conformidade operacional'}
        title={team ? 'Qualificações da equipe' : 'Qualificações'}
        description="Consulte atividades liberadas, vencendo ou bloqueadas e os requisitos responsáveis."
      />
      <div className="mb-5 flex items-center gap-2">
        <Filter size={16} className="text-muted-foreground" />
        <select
          value={status}
          onChange={(event) => { setStatus(event.target.value); setPage(0) }}
          className="h-10 rounded-md border border-border bg-card px-3 text-sm"
        >
          <option value="">Todos os status</option>
          <option value="AVAILABLE">Liberadas</option>
          <option value="EXPIRING">Vencendo</option>
          <option value="BLOCKED">Bloqueadas</option>
          <option value="NOT_ASSIGNED">Não atribuídas</option>
        </select>
      </div>
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={ShieldAlert} title="Nenhuma qualificação encontrada" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((qualification) => (
              <div key={qualification.id} className="flex flex-wrap items-start gap-4 p-4">
                <span className="grid size-9 place-items-center bg-muted text-primary"><ShieldAlert size={17} /></span>
                <span className="min-w-52 flex-1">
                  <strong className="block text-sm">{qualification.employee.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">
                    {qualification.activity.name} · {qualification.employee.registration}
                  </span>
                  {!!qualification.blockingReasons.length && (
                    <span className="mt-2 block text-xs text-destructive">
                      {qualification.blockingReasons.map((reason) => reason.trainingName || reason.type).join(', ')}
                    </span>
                  )}
                </span>
                <StatusBadge value={qualification.status} />
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function OrganizationPage() {
  const [tab, setTab] = useState<'units' | 'sectors' | 'jobs'>('units')
  const config = {
    units: { title: 'Unidades', endpoint: '/units', icon: Building2 },
    sectors: { title: 'Setores', endpoint: '/sectors', icon: ActivityIcon },
    jobs: { title: 'Cargos', endpoint: '/jobs', icon: BriefcaseBusiness },
  }[tab]
  const state = useApiData<PageResponse<CatalogItem>>(`${config.endpoint}?size=100&sort=name,asc`)

  return (
    <div>
      <PageHeader
        eyebrow="Estrutura organizacional"
        title="Organização"
        description="Unidades, setores e cargos orientam escopos, atividades padrão e atribuições."
      />
      <div className="mb-5 flex gap-1 border-b border-border">
        {(['units', 'sectors', 'jobs'] as const).map((item) => (
          <button
            key={item}
            onClick={() => setTab(item)}
            className={`border-b-2 px-4 py-3 text-sm font-semibold ${tab === item ? 'border-primary text-primary' : 'border-transparent text-muted-foreground'}`}
          >
            {{ units: 'Unidades', sectors: 'Setores', jobs: 'Cargos' }[item]}
          </button>
        ))}
      </div>
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={config.icon} title={`Nenhum item em ${config.title.toLocaleLowerCase('pt-BR')}`} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {state.data.content.map((item) => (
            <article key={item.id} className="panel p-5">
              <config.icon className="text-primary" size={20} />
              <h2 className="display mt-5 text-2xl font-bold">{item.name}</h2>
              <p className="mt-1 text-xs text-muted-foreground">{item.code || item.description || 'Sem descrição'}</p>
              <div className="mt-4"><StatusBadge value={item.status} /></div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}

type GenericRecord = Record<string, unknown> & { id: string }

const genericConfigs = {
  certificates: {
    eyebrow: 'Evidências e validade',
    title: 'Certificados',
    description: 'Consulte certificados emitidos, processados ou revogados.',
    endpoint: '/certificates',
    icon: BadgeCheck,
  },
  users: {
    eyebrow: 'Identidade e acesso',
    title: 'Usuários e permissões',
    description: 'Consulte contas, papéis e status de acesso.',
    endpoint: '/users',
    icon: UserCog,
  },
  audit: {
    eyebrow: 'Rastreabilidade',
    title: 'Auditoria',
    description: 'Ações relevantes registradas com usuário, entidade e instante.',
    endpoint: '/audit-logs',
    icon: ClipboardList,
  },
  notifications: {
    eyebrow: 'Comunicação interna',
    title: 'Notificações',
    description: 'Notificações associadas ao usuário administrativo atual.',
    endpoint: '/me/notifications',
    icon: Bell,
  },
  emails: {
    eyebrow: 'Entregas de e-mail',
    title: 'E-mails',
    description: 'Acompanhe tentativas, falhas e reenvios de mensagens.',
    endpoint: '/admin/email-deliveries',
    icon: Mail,
  },
} as const

export function GenericManagementPage({ kind }: { kind: keyof typeof genericConfigs }) {
  const config = genericConfigs[kind]
  const [page, setPage] = useState(0)
  const state = useApiData<PageResponse<GenericRecord>>(`${config.endpoint}?page=${page}&size=15`)
  const Icon = config.icon

  return (
    <div>
      <PageHeader eyebrow={config.eyebrow} title={config.title} description={config.description} />
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={Icon} title={`Nenhum registro em ${config.title.toLocaleLowerCase('pt-BR')}`} />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((item) => (
              <div key={item.id} className="flex flex-wrap items-center gap-4 p-4">
                <span className="grid size-9 place-items-center bg-muted text-primary"><Icon size={17} /></span>
                <span className="min-w-52 flex-1">
                  <strong className="block break-all text-sm">{primaryText(item)}</strong>
                  <span className="mt-1 block break-all text-xs text-muted-foreground">{secondaryText(item)}</span>
                </span>
                {typeof item.status === 'string' && <StatusBadge value={item.status} />}
              </div>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function ReportsPage({ team = false }: { team?: boolean }) {
  const [report, setReport] = useState<'training-status' | 'qualifications' | 'expirations'>('training-status')
  const state = useApiData<PageResponse<GenericRecord>>(`/reports/${report}?size=20`)
  return (
    <div>
      <PageHeader
        eyebrow={team ? 'Escopo da equipe' : 'Análise operacional'}
        title="Relatórios"
        description="Dados paginados e filtrados no backend; exportações usam somente o conjunto carregado."
        action={
          <Button
            variant="outline"
            onClick={() => {
              if (!state.data) return
              const blob = new Blob([JSON.stringify(state.data.content, null, 2)], { type: 'application/json' })
              const url = URL.createObjectURL(blob)
              const anchor = document.createElement('a')
              anchor.href = url
              anchor.download = `${report}.json`
              anchor.click()
              URL.revokeObjectURL(url)
            }}
            disabled={!state.data?.content.length}
          >
            <Download size={16} /> Exportar dados
          </Button>
        }
      />
      <div className="mb-5 flex flex-wrap gap-2">
        {[
          ['training-status', 'Status de treinamentos'],
          ['qualifications', 'Qualificações'],
          ['expirations', 'Expirações'],
        ].map(([value, label]) => (
          <Button key={value} variant={report === value ? 'primary' : 'outline'} onClick={() => setReport(value as typeof report)}>
            {label}
          </Button>
        ))}
      </div>
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState title="Nenhum dado para este relatório" />
      ) : (
        <div className="panel divide-y divide-border">
          {state.data.content.map((item) => (
            <div key={item.id || JSON.stringify(item)} className="p-4">
              <strong className="text-sm">{primaryText(item)}</strong>
              <p className="mt-1 text-xs text-muted-foreground">{secondaryText(item)}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

type OrganizationSettings = {
  expiringSoonDays: number
  defaultPassingScore: number
  defaultRequiredVideoPercentage: number
}

export function SettingsPage() {
  const state = useApiData<OrganizationSettings>('/organization/settings')
  const [draft, setDraft] = useState<OrganizationSettings | null>(null)
  const [saving, setSaving] = useState(false)
  const values = draft || state.data

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!values) return
    setSaving(true)
    try {
      const result = await api<OrganizationSettings>('/organization/settings', {
        method: 'PATCH',
        body: JSON.stringify(values),
      })
      setDraft(result)
      toast.success('Configurações salvas.')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  if (state.loading) return <LoadingState />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!values) return <EmptyState title="Configurações indisponíveis" />

  return (
    <div className="max-w-3xl">
      <PageHeader
        eyebrow="Parâmetros oficiais"
        title="Configurações"
        description="O percentual obrigatório de vídeo permanece fixo em 80% conforme a fonte da verdade."
      />
      <form onSubmit={save} className="panel">
        <div className="grid gap-5 p-5 sm:grid-cols-2 sm:p-7">
          <FormField
            label="Janela de vencimento próximo (dias)"
            type="number"
            value={String(values.expiringSoonDays)}
            onChange={(value) => setDraft({ ...values, expiringSoonDays: Number(value) })}
            min={1}
            max={3650}
          />
          <FormField
            label="Nota mínima padrão (%)"
            type="number"
            value={String(values.defaultPassingScore)}
            onChange={(value) => setDraft({ ...values, defaultPassingScore: Number(value) })}
            min={70}
            max={100}
          />
          <FormField
            label="Percentual obrigatório de vídeo (%)"
            type="number"
            value={String(values.defaultRequiredVideoPercentage)}
            onChange={() => undefined}
            min={80}
            max={80}
            disabled
          />
        </div>
        <footer className="flex justify-end border-t border-border p-5">
          <Button disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : 'Salvar configurações'}</Button>
        </footer>
      </form>
    </div>
  )
}

function FormField({
  label,
  value,
  onChange,
  type = 'text',
  required = true,
  ...props
}: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  required?: boolean
} & Omit<React.InputHTMLAttributes<HTMLInputElement>, 'value' | 'onChange' | 'type' | 'required'>) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <input
        {...props}
        required={required}
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-11 w-full rounded-md border border-border bg-card px-3 text-sm disabled:bg-muted"
      />
    </label>
  )
}

function SelectField<T extends { id: string; name: string }>({
  label,
  value,
  onChange,
  options,
  loading,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: T[]
  loading: boolean
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <select
        required
        value={value}
        onChange={(event) => onChange(event.target.value)}
        disabled={loading}
        className="h-11 w-full rounded-md border border-border bg-card px-3 text-sm disabled:bg-muted"
      >
        <option value="">{loading ? 'Carregando…' : 'Selecione'}</option>
        {options.map((option) => <option key={option.id} value={option.id}>{option.name}</option>)}
      </select>
    </label>
  )
}

function SelectNative({
  label,
  value,
  onChange,
  options,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: Array<[string, string]>
}) {
  return (
    <label>
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} className="h-11 w-full rounded-md border border-border bg-card px-3 text-sm">
        {options.map(([optionValue, labelValue]) => <option key={optionValue} value={optionValue}>{labelValue}</option>)}
      </select>
    </label>
  )
}

function FormFooter({ submitting, error, label }: { submitting: boolean; error: string; label: string }) {
  return (
    <footer className="border-t border-border p-5">
      {error && <p className="mb-4 text-sm text-destructive" role="alert">{error}</p>}
      <div className="flex justify-end">
        <Button disabled={submitting}>{submitting ? <InlineLoading label="Salvando" /> : label}</Button>
      </div>
    </footer>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-1 break-words text-sm font-semibold">{value}</dd>
    </div>
  )
}

function initials(name: string): string {
  return name.split(/\s+/).slice(0, 2).map((part) => part[0]).join('').toUpperCase()
}

function primaryText(item: GenericRecord): string {
  return String(item.name || item.title || item.email || item.subject || item.validationCode || item.action || item.type || item.id)
}

function secondaryText(item: GenericRecord): string {
  const value =
    item.description ||
    item.message ||
    item.registration ||
    item.code ||
    item.recipient ||
    item.entityType ||
    item.createdAt ||
    ''
  return typeof value === 'string' && value.includes('T') ? formatDateTime(value) : String(value)
}
