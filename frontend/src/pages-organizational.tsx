import { FormEvent, useEffect, useState } from 'react'
import {
  Activity as ActivityIcon,
  ArrowRight,
  BriefcaseBusiness,
  Building2,
  CheckCircle2,
  Link2,
  Plus,
  Save,
  ShieldAlert,
  Trash2,
  UserRound,
  X,
} from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api } from './api'
import { apiErrorMessage } from './pages-auth'
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
import { Activity, Employee, PageResponse, Qualification, Training } from './types'

type CatalogRecord = {
  id: string
  name: string
  status: string
  code?: string | null
  description?: string | null
  unitId?: string
}

type OrganizationTab = 'units' | 'sectors' | 'jobs'

type RelatedJob = {
  id: string
  name: string
  linkedAt: string
}

type ActivityRequirement = {
  id: string
  activityId: string
  training: Pick<Training, 'id' | 'name' | 'code'>
  versionPolicy: 'LATEST_PUBLISHED' | 'FIXED_VERSION'
  trainingVersionId?: string | null
  required: boolean
  linkedAt: string
}

type EmployeeActivity = {
  employeeId: string
  activity: Activity
  origins: Array<'JOB' | 'MANUAL'>
  assignedAt: string
  effective: boolean
}

const pageSize = 15

export function buildPagedPath(path: string, page: number, params: Record<string, string | undefined> = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(pageSize), sort: 'name,asc' })
  Object.entries(params).forEach(([key, value]) => {
    if (value) query.set(key, value)
  })
  return `${path}?${query.toString()}`
}

export function buildStatusPayload(status: 'ACTIVE' | 'INACTIVE') {
  return { status }
}

function Field({
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

function TextArea({
  label,
  value,
  onChange,
  required = false,
  ...props
}: {
  label: string
  value: string
  onChange: (value: string) => void
  required?: boolean
} & Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'value' | 'onChange' | 'required'>) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <textarea
        {...props}
        required={required}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="w-full rounded-md border border-border bg-card p-3 text-sm disabled:bg-muted"
      />
    </label>
  )
}

function SelectField<T extends { id: string; name: string }>({
  label,
  value,
  onChange,
  options,
  loading = false,
  required = true,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  options: T[]
  loading?: boolean
  required?: boolean
}) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <select
        required={required}
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

function NativeSelect({
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
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-11 w-full rounded-md border border-border bg-card px-3 text-sm"
      >
        {options.map(([optionValue, optionLabel]) => <option key={optionValue} value={optionValue}>{optionLabel}</option>)}
      </select>
    </label>
  )
}

function FormError({ error }: { error: string }) {
  return error ? <p className="text-sm text-destructive" role="alert">{error}</p> : null
}

function CatalogIcon({ kind }: { kind: OrganizationTab }) {
  const Icon = kind === 'units' ? Building2 : kind === 'sectors' ? ActivityIcon : BriefcaseBusiness
  return <Icon className="text-primary" size={20} />
}

function organizationEndpoint(tab: OrganizationTab) {
  return `/${tab}`
}

export function OrganizationAdminPage() {
  const [tab, setTab] = useState<OrganizationTab>('units')
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [editingId, setEditingId] = useState<string | null>(null)
  const [form, setForm] = useState({ name: '', code: '', description: '', unitId: '', status: 'ACTIVE' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const units = useApiData<PageResponse<CatalogRecord>>('/units?size=100&status=ACTIVE&sort=name,asc')
  const endpoint = organizationEndpoint(tab)
  const state = useApiData<PageResponse<CatalogRecord>>(
    buildPagedPath(endpoint, page, {
      search: search.trim() || undefined,
      unitId: tab === 'sectors' ? form.unitId || undefined : undefined,
    }),
  )
  const emptyIcon = tab === 'units' ? Building2 : tab === 'sectors' ? ActivityIcon : BriefcaseBusiness

  function changeTab(nextTab: OrganizationTab) {
    setTab(nextTab)
    setPage(0)
    setEditingId(null)
    setError('')
    setForm({ name: '', code: '', description: '', unitId: '', status: 'ACTIVE' })
  }

  function edit(item: CatalogRecord) {
    setEditingId(item.id)
    setError('')
    setForm({
      name: item.name,
      code: item.code || '',
      description: item.description || '',
      unitId: item.unitId || '',
      status: item.status,
    })
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const payload = tab === 'units'
        ? { name: form.name, code: form.code || null, status: form.status }
        : tab === 'sectors'
          ? { unitId: form.unitId, name: form.name, code: form.code || null, status: form.status }
          : { name: form.name, description: form.description || null, status: form.status }
      await api(`${endpoint}${editingId ? `/${editingId}` : ''}`, {
        method: editingId ? 'PATCH' : 'POST',
        body: JSON.stringify(payload),
      })
      toast.success(`${tab === 'units' ? 'Unidade' : tab === 'sectors' ? 'Setor' : 'Cargo'} ${editingId ? 'atualizado' : 'cadastrado'}.`)
      setEditingId(null)
      setForm({ name: '', code: '', description: '', unitId: '', status: 'ACTIVE' })
      state.reload()
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  async function changeStatus(item: CatalogRecord) {
    const status = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await api(`${endpoint}/${item.id}/status`, { method: 'PATCH', body: JSON.stringify(buildStatusPayload(status)) })
      toast.success(`${item.name} ${status === 'ACTIVE' ? 'ativado' : 'inativado'}.`)
      state.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  const title = tab === 'units' ? 'Unidades' : tab === 'sectors' ? 'Setores' : 'Cargos'

  return (
    <div>
      <PageHeader
        eyebrow="Estrutura organizacional"
        title="Organização"
        description="Unidades, setores e cargos orientam os vínculos de colaboradores e atividades padrão."
      />
      <div className="mb-5 flex flex-wrap gap-1 border-b border-border" role="tablist" aria-label="Cadastros organizacionais">
        {(['units', 'sectors', 'jobs'] as OrganizationTab[]).map((item) => (
          <button
            key={item}
            type="button"
            role="tab"
            aria-selected={tab === item}
            onClick={() => changeTab(item)}
            className={`border-b-2 px-4 py-3 text-sm font-semibold ${tab === item ? 'border-primary text-primary' : 'border-transparent text-muted-foreground'}`}
          >
            {{ units: 'Unidades', sectors: 'Setores', jobs: 'Cargos' }[item]}
          </button>
        ))}
      </div>
      <div className="grid gap-6 xl:grid-cols-[minmax(17rem,.65fr)_minmax(0,1.35fr)]">
        <form onSubmit={submit} className="panel h-fit p-5 sm:p-6">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="eyebrow text-primary">{editingId ? 'Edição' : 'Novo cadastro'}</p>
              <h2 className="display mt-2 text-2xl font-bold">{editingId ? `Editar ${title.slice(0, -1).toLocaleLowerCase('pt-BR')}` : `Nova ${title.slice(0, -1).toLocaleLowerCase('pt-BR')}`}</h2>
            </div>
            {editingId && <Button type="button" variant="ghost" aria-label="Cancelar edição" onClick={() => { setEditingId(null); setError('') }}><X size={16} /></Button>}
          </div>
          <div className="mt-5 space-y-4">
            <Field label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} maxLength={150} />
            {tab !== 'jobs' && <Field label="Código" value={form.code} onChange={(code) => setForm({ ...form, code })} required={false} maxLength={20} />}
            {tab === 'jobs' && <TextArea label="Descrição" value={form.description} onChange={(description) => setForm({ ...form, description })} maxLength={1000} rows={4} />}
            {tab === 'sectors' && (
              <SelectField
                label="Unidade"
                value={form.unitId}
                onChange={(unitId) => setForm({ ...form, unitId })}
                options={units.data?.content || []}
                loading={units.loading}
              />
            )}
            <NativeSelect label="Status" value={form.status} onChange={(status) => setForm({ ...form, status })} options={[['ACTIVE', 'Ativo'], ['INACTIVE', 'Inativo']]} />
            <FormError error={error} />
            <Button disabled={submitting} className="w-full">
              {submitting ? <InlineLoading label="Salvando" /> : <><Save size={16} /> {editingId ? 'Salvar alterações' : `Criar ${title.slice(0, -1).toLocaleLowerCase('pt-BR')}`}</>}
            </Button>
          </div>
        </form>
        <section>
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <SearchField value={search} onChange={(value) => { setSearch(value); setPage(0) }} placeholder={`Buscar ${title.toLocaleLowerCase('pt-BR')}`} />
            {tab === 'sectors' && <p className="text-xs text-muted-foreground">Filtre também pela unidade no formulário.</p>}
          </div>
          {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
            <EmptyState icon={emptyIcon} title={`Nenhum registro em ${title.toLocaleLowerCase('pt-BR')}`} description="Crie o primeiro registro ou ajuste os filtros." />
          ) : (
            <>
              <div className="panel divide-y divide-border">
                {state.data.content.map((item) => (
                  <article key={item.id} className="flex flex-wrap items-center gap-4 p-4">
                    <span className="grid size-10 shrink-0 place-items-center bg-primary/10"><CatalogIcon kind={tab} /></span>
                    <span className="min-w-48 flex-1">
                      <strong className="block text-sm">{item.name}</strong>
                      <span className="mt-1 block text-xs text-muted-foreground">{item.code || item.description || 'Sem código ou descrição'}</span>
                    </span>
                    <StatusBadge value={item.status} />
                    <div className="flex items-center gap-1">
                      <Button type="button" variant="ghost" onClick={() => edit(item)}>Editar</Button>
                      <Button type="button" variant="outline" onClick={() => changeStatus(item)}>{item.status === 'ACTIVE' ? 'Inativar' : 'Ativar'}</Button>
                    </div>
                  </article>
                ))}
              </div>
              <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
            </>
          )}
        </section>
      </div>
    </div>
  )
}

export function ActivitiesAdminPage() {
  const [page, setPage] = useState(0)
  const [search, setSearch] = useState('')
  const [status, setStatus] = useState('')
  const state = useApiData<PageResponse<Activity>>(
    buildPagedPath('/activities', page, { search: search.trim() || undefined, status: status || undefined }),
  )

  return (
    <div>
      <PageHeader
        eyebrow="Requisitos operacionais"
        title="Atividades"
        description="Cadastre atividades, vincule-as a cargos e mantenha os treinamentos obrigatórios em um fluxo rastreável."
        action={<LinkButton to="/admin/atividades/nova"><Plus size={16} /> Nova atividade</LinkButton>}
      />
      <div className="mb-5 flex flex-wrap items-center gap-3">
        <SearchField value={search} onChange={(value) => { setSearch(value); setPage(0) }} placeholder="Buscar atividade" />
        <NativeSelect label="" value={status} onChange={(value) => { setStatus(value); setPage(0) }} options={[['', 'Todos os status'], ['ACTIVE', 'Ativas'], ['INACTIVE', 'Inativas']]} />
      </div>
      {state.loading ? <LoadingState /> : state.error ? <ErrorState message={state.error} retry={state.reload} /> : !state.data?.content.length ? (
        <EmptyState icon={BriefcaseBusiness} title="Nenhuma atividade cadastrada" action={<LinkButton to="/admin/atividades/nova"><Plus size={16} /> Criar atividade</LinkButton>} />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((activity) => (
              <Link key={activity.id} to={`/admin/atividades/${activity.id}`} className="flex flex-wrap items-center gap-4 p-4 transition hover:bg-muted/40">
                <span className="grid size-10 shrink-0 place-items-center bg-primary/10 text-primary"><BriefcaseBusiness size={19} /></span>
                <span className="min-w-52 flex-1">
                  <strong className="block text-sm">{activity.name}</strong>
                  <span className="mt-1 block text-xs text-muted-foreground">{activity.description || 'Sem descrição'}</span>
                </span>
                <StatusBadge value={activity.status} />
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

export function CreateActivityAdminPage() {
  const [form, setForm] = useState({ name: '', description: '', status: 'ACTIVE' })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const activity = await api<Activity>('/activities', { method: 'POST', body: JSON.stringify(form) })
      toast.success('Atividade cadastrada.')
      navigate(`/admin/atividades/${activity.id}`)
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="mx-auto max-w-3xl">
      <BackLink to="/admin/atividades">Atividades</BackLink>
      <PageHeader eyebrow="Novo requisito operacional" title="Cadastrar atividade" description="Uma atividade inativa não pode receber novos vínculos ou atribuições." />
      <form onSubmit={submit} className="panel">
        <div className="space-y-5 p-5 sm:p-7">
          <Field label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} maxLength={150} />
          <TextArea label="Descrição" value={form.description} onChange={(description) => setForm({ ...form, description })} maxLength={2000} rows={5} />
          <NativeSelect label="Status" value={form.status} onChange={(status) => setForm({ ...form, status })} options={[['ACTIVE', 'Ativa'], ['INACTIVE', 'Inativa']]} />
        </div>
        <footer className="border-t border-border p-5">
          <FormError error={error} />
          <div className="mt-4 flex justify-end"><Button disabled={submitting}>{submitting ? <InlineLoading label="Salvando" /> : <><Plus size={16} /> Criar atividade</>}</Button></div>
        </footer>
      </form>
    </div>
  )
}

export function ActivityDetailAdminPage() {
  const { activityId = '' } = useParams()
  const activity = useApiData<Activity>(activityId ? `/activities/${activityId}` : null)
  const jobs = useApiData<RelatedJob[]>(activityId ? `/activities/${activityId}/jobs` : null)
  const requirements = useApiData<ActivityRequirement[]>(activityId ? `/activities/${activityId}/requirements` : null)
  const jobOptions = useApiData<PageResponse<CatalogRecord>>('/jobs?size=100&status=ACTIVE&sort=name,asc')
  const trainingOptions = useApiData<PageResponse<Training>>('/trainings?size=100&status=ACTIVE&sort=name,asc')
  const [qualPage, setQualPage] = useState(0)
  const [qualStatus, setQualStatus] = useState('')
  const qualifications = useApiData<PageResponse<Qualification>>(
    activityId ? `/activities/${activityId}/qualified-employees?page=${qualPage}&size=${pageSize}&sort=status,asc${qualStatus ? `&status=${qualStatus}` : ''}` : null,
  )
  const [form, setForm] = useState({ name: '', description: '' })
  const [jobId, setJobId] = useState('')
  const [trainingId, setTrainingId] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (activity.data) setForm({ name: activity.data.name, description: activity.data.description || '' })
  }, [activity.data])

  async function saveActivity(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    setError('')
    try {
      await api(`/activities/${activityId}`, { method: 'PATCH', body: JSON.stringify(form) })
      toast.success('Atividade atualizada.')
      activity.reload()
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  async function changeStatus() {
    if (!activity.data) return
    const status = activity.data.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
    try {
      await api(`/activities/${activityId}/status`, { method: 'PATCH', body: JSON.stringify(buildStatusPayload(status)) })
      toast.success(`Atividade ${status === 'ACTIVE' ? 'ativada' : 'inativada'}.`)
      activity.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  async function addJob(event: FormEvent) {
    event.preventDefault()
    if (!jobId) return
    setSaving(true)
    setError('')
    try {
      await api(`/jobs/${jobId}/activities`, { method: 'POST', body: JSON.stringify({ activityId, applyToCurrentEmployees: true }) })
      toast.success('Atividade vinculada ao cargo; colaboradores ativos foram propagados.')
      setJobId('')
      jobs.reload()
      qualifications.reload()
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  async function removeJob(link: RelatedJob) {
    try {
      await api(`/jobs/${link.id}/activities/${activityId}`, { method: 'DELETE' })
      toast.success('Vínculo removido sem apagar históricos.')
      jobs.reload()
      qualifications.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  async function addRequirement(event: FormEvent) {
    event.preventDefault()
    if (!trainingId) return
    setSaving(true)
    setError('')
    try {
      await api(`/activities/${activityId}/requirements`, {
        method: 'POST',
        body: JSON.stringify({ trainingId, versionPolicy: 'LATEST_PUBLISHED', required: true, applyToCurrentEmployees: true }),
      })
      toast.success('Treinamento obrigatório vinculado.')
      setTrainingId('')
      requirements.reload()
      qualifications.reload()
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  async function removeRequirement(requirement: ActivityRequirement) {
    try {
      await api(`/activities/${activityId}/requirements/${requirement.id}`, { method: 'DELETE' })
      toast.success('Requisito removido; conclusões e atribuições históricas foram preservadas.')
      requirements.reload()
      qualifications.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  if (activity.loading) return <LoadingState />
  if (activity.error) return <ErrorState message={activity.error} retry={activity.reload} />
  if (!activity.data) return <EmptyState title="Atividade não encontrada" />

  const linkedJobIds = new Set((jobs.data || []).map((item) => item.id))
  const availableJobs = (jobOptions.data?.content || []).filter((item) => !linkedJobIds.has(item.id))
  const linkedTrainingIds = new Set((requirements.data || []).map((item) => item.training.id))
  const availableTrainings = (trainingOptions.data?.content || []).filter((item) => !linkedTrainingIds.has(item.id))

  return (
    <div>
      <BackLink to="/admin/atividades">Atividades</BackLink>
      <PageHeader
        eyebrow="Configuração operacional"
        title={activity.data.name}
        description="O vínculo Cargo → atividade padrão → treinamentos obrigatórios gera as atribuições e qualificações do colaborador."
        action={<div className="flex items-center gap-2"><StatusBadge value={activity.data.status} /><Button variant="outline" onClick={changeStatus}>{activity.data.status === 'ACTIVE' ? 'Inativar' : 'Ativar'}</Button></div>}
      />
      <FormError error={error} />
      <div className="mt-5 grid gap-6 xl:grid-cols-2">
        <form onSubmit={saveActivity} className="panel p-5 sm:p-6">
          <h2 className="display text-2xl font-bold">Dados da atividade</h2>
          <div className="mt-5 space-y-4">
            <Field label="Nome" value={form.name} onChange={(name) => setForm({ ...form, name })} maxLength={150} />
            <TextArea label="Descrição" value={form.description} onChange={(description) => setForm({ ...form, description })} maxLength={2000} rows={5} />
            <Button disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : <><Save size={16} /> Salvar atividade</>}</Button>
          </div>
        </form>
        <section className="panel p-5 sm:p-6">
          <div className="flex items-start justify-between gap-3"><div><h2 className="display text-2xl font-bold">Cargos padrão</h2><p className="mt-1 text-sm text-muted-foreground">A propagação alcança colaboradores ativos do cargo.</p></div><Link2 className="text-primary" size={20} /></div>
          <form onSubmit={addJob} className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="min-w-0 flex-1"><SelectField label="Adicionar cargo" value={jobId} onChange={setJobId} options={availableJobs} loading={jobOptions.loading} /></div>
            <Button disabled={saving || !jobId}><Plus size={16} /> Vincular</Button>
          </form>
          <div className="mt-5 divide-y divide-border border-y border-border">
            {jobs.loading && <p className="py-4 text-sm text-muted-foreground">Carregando cargos…</p>}
            {!jobs.loading && !jobs.data?.length && <p className="py-4 text-sm text-muted-foreground">Nenhum cargo vinculado.</p>}
            {jobs.data?.map((link) => <div key={link.id} className="flex items-center gap-3 py-4"><span className="min-w-0 flex-1"><strong className="block text-sm">{link.name}</strong><span className="text-xs text-muted-foreground">Vinculado em {formatDateTime(link.linkedAt)} · a propagação preserva históricos</span></span><Button type="button" variant="ghost" aria-label={`Remover vínculo com ${link.name}`} onClick={() => removeJob(link)}><Trash2 size={16} /></Button></div>)}
          </div>
        </section>
        <section className="panel p-5 sm:p-6">
          <div className="flex items-start justify-between gap-3"><div><h2 className="display text-2xl font-bold">Treinamentos obrigatórios</h2><p className="mt-1 text-sm text-muted-foreground">Somente versões publicadas e válidas liberam a atividade.</p></div><CheckCircle2 className="text-primary" size={20} /></div>
          <form onSubmit={addRequirement} className="mt-5 flex flex-col gap-3 sm:flex-row sm:items-end">
            <div className="min-w-0 flex-1"><SelectField label="Adicionar treinamento" value={trainingId} onChange={setTrainingId} options={availableTrainings} loading={trainingOptions.loading} /></div>
            <Button disabled={saving || !trainingId}><Plus size={16} /> Vincular</Button>
          </form>
          <div className="mt-5 divide-y divide-border border-y border-border">
            {requirements.loading && <p className="py-4 text-sm text-muted-foreground">Carregando requisitos…</p>}
            {!requirements.loading && !requirements.data?.length && <p className="py-4 text-sm text-muted-foreground">Nenhum treinamento obrigatório vinculado.</p>}
            {requirements.data?.map((requirement) => <div key={requirement.id} className="flex items-center gap-3 py-4"><span className="min-w-0 flex-1"><strong className="block text-sm">{requirement.training.name}</strong><span className="text-xs text-muted-foreground">{requirement.training.code} · versão publicada vigente</span></span><Button type="button" variant="ghost" aria-label="Remover requisito" onClick={() => removeRequirement(requirement)}><Trash2 size={16} /></Button></div>)}
          </div>
        </section>
        <section className="panel p-5 sm:p-6 xl:col-span-2">
          <div className="flex flex-wrap items-end justify-between gap-4"><div><h2 className="display text-2xl font-bold">Colaboradores por qualificação</h2><p className="mt-1 text-sm text-muted-foreground">Consulta paginada e filtrada no backend.</p></div><NativeSelect label="Situação" value={qualStatus} onChange={(value) => { setQualStatus(value); setQualPage(0) }} options={[['', 'Todas'], ['AVAILABLE', 'Liberadas'], ['EXPIRING', 'Vencendo'], ['BLOCKED', 'Bloqueadas'], ['NOT_ASSIGNED', 'Não atribuídas']]} /></div>
          {qualifications.loading ? <div className="mt-5"><LoadingState /></div> : qualifications.error ? <div className="mt-5"><ErrorState message={qualifications.error} retry={qualifications.reload} /></div> : !qualifications.data?.content.length ? <p className="mt-5 text-sm text-muted-foreground">Nenhum colaborador corresponde ao filtro.</p> : <><div className="mt-5 grid gap-3 md:grid-cols-2 xl:grid-cols-3">{qualifications.data.content.map((item) => <article key={item.id} className="border border-border p-4"><div className="flex items-start gap-3"><span className="grid size-9 place-items-center bg-muted text-primary">{item.status === 'AVAILABLE' ? <CheckCircle2 size={17} /> : <ShieldAlert size={17} />}</span><span className="min-w-0 flex-1"><strong className="block text-sm">{item.employee.name}</strong><span className="mt-1 block text-xs text-muted-foreground">{item.employee.registration}</span></span><StatusBadge value={item.status} /></div>{item.blockingReasons.length > 0 && <p className="mt-3 text-xs text-destructive">{item.blockingReasons.map((reason) => reason.trainingName || reason.type).join(', ')}</p>}</article>)}</div><Pagination page={qualPage} totalPages={qualifications.data.totalPages} onChange={setQualPage} /></>}
        </section>
      </div>
    </div>
  )
}

export function EmployeeJobPanel({ employee, onChanged }: { employee: Employee; onChanged: () => void }) {
  const jobs = useApiData<PageResponse<CatalogRecord>>('/jobs?size=100&status=ACTIVE&sort=name,asc')
  const [jobId, setJobId] = useState(employee.job.id)
  const [removePrevious, setRemovePrevious] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => setJobId(employee.job.id), [employee.job.id])

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (jobId === employee.job.id) return
    setSaving(true)
    setError('')
    try {
      const effects = await api<{ activitiesAdded: number; assignmentsCreated: number }>('/employees/' + employee.id + '/job', {
        method: 'PATCH',
        body: JSON.stringify({ jobId, removePreviousJobActivities: removePrevious }),
      })
      toast.success(`Cargo alterado; ${effects.activitiesAdded} atividades padrão adicionadas e ${effects.assignmentsCreated} atribuições geradas.`)
      onChanged()
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  return (
    <section className="mt-6 border-t border-border pt-5">
      <h2 className="display text-xl font-bold">Alterar cargo</h2>
      <form onSubmit={submit} className="mt-4 space-y-4">
        <SelectField label="Novo cargo" value={jobId} onChange={setJobId} options={jobs.data?.content || []} loading={jobs.loading} />
        <label className="flex items-start gap-2 text-xs text-muted-foreground"><input type="checkbox" checked={removePrevious} onChange={(event) => setRemovePrevious(event.target.checked)} className="mt-0.5" /> Remover somente as atividades originadas pelo cargo anterior.</label>
        <FormError error={error} />
        <Button disabled={saving || jobId === employee.job.id}>{saving ? <InlineLoading label="Atualizando" /> : <><Save size={16} /> Atualizar cargo</>}</Button>
      </form>
    </section>
  )
}

export function EmployeeActivitiesPanel({ employeeId, canManage, onChanged }: { employeeId: string; canManage: boolean; onChanged?: () => void }) {
  const activities = useApiData<EmployeeActivity[]>(`/employees/${employeeId}/activities`)
  const options = useApiData<PageResponse<Activity>>('/activities?size=100&status=ACTIVE&sort=name,asc')
  const [activityId, setActivityId] = useState('')
  const [reason, setReason] = useState('')
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  async function add(event: FormEvent) {
    event.preventDefault()
    if (!activityId) return
    setSaving(true)
    setError('')
    try {
      await api(`/employees/${employeeId}/activities`, { method: 'POST', body: JSON.stringify({ activityId, reason: reason || null }) })
      toast.success('Atividade específica atribuída e requisitos propagados.')
      setActivityId('')
      setReason('')
      activities.reload()
      onChanged?.()
    } catch (value) {
      setError(apiErrorMessage(value))
    } finally {
      setSaving(false)
    }
  }

  async function remove(item: EmployeeActivity) {
    if (!item.origins.includes('MANUAL')) return
    try {
      await api(`/employees/${employeeId}/activities/${item.activity.id}`, { method: 'DELETE' })
      toast.success('Atividade específica removida; histórico preservado.')
      activities.reload()
      onChanged?.()
    } catch (value) {
      toast.error(apiErrorMessage(value))
    }
  }

  return (
    <section className="panel mt-6 p-5 sm:p-6">
      <div className="flex items-start justify-between gap-3"><div><p className="eyebrow text-primary">Relações operacionais</p><h2 className="display mt-2 text-2xl font-bold">Atividades atribuídas</h2><p className="mt-1 text-sm text-muted-foreground">Atividades do cargo e atividades específicas permanecem distintas e auditáveis.</p></div><UserRound className="text-primary" size={22} /></div>
      {canManage && <form onSubmit={add} className="mt-5 grid gap-4 lg:grid-cols-[1fr_1fr_auto] lg:items-end"><SelectField label="Atividade específica" value={activityId} onChange={setActivityId} options={options.data?.content || []} loading={options.loading} /><Field label="Motivo (opcional)" value={reason} onChange={setReason} required={false} maxLength={1000} /><Button disabled={saving || !activityId}>{saving ? <InlineLoading label="Atribuindo" /> : <><Plus size={16} /> Atribuir</>}</Button></form>}
      <FormError error={error} />
      {activities.loading ? <div className="mt-5"><LoadingState /></div> : activities.error ? <div className="mt-5"><ErrorState message={activities.error} retry={activities.reload} /></div> : !activities.data?.length ? <div className="mt-5"><EmptyState icon={ActivityIcon} title="Nenhuma atividade atribuída" description="As atividades padrão aparecem após o vínculo ao cargo." /></div> : <div className="mt-5 divide-y divide-border border-y border-border">{activities.data.map((item) => <article key={item.activity.id} className="flex flex-wrap items-center gap-4 py-4"><span className="grid size-9 place-items-center bg-muted text-primary"><ActivityIcon size={17} /></span><span className="min-w-52 flex-1"><strong className="block text-sm">{item.activity.name}</strong><span className="mt-1 block text-xs text-muted-foreground">{item.origins.map((origin) => origin === 'JOB' ? 'Cargo' : 'Específica').join(' + ')} · atribuída em {formatDate(item.assignedAt)}</span></span><StatusBadge value={item.effective ? 'AVAILABLE' : 'NOT_ASSIGNED'} />{canManage && item.origins.includes('MANUAL') && <Button type="button" variant="ghost" aria-label={`Remover ${item.activity.name}`} onClick={() => remove(item)}><Trash2 size={16} /></Button>}</article>)}</div>}
    </section>
  )
}
