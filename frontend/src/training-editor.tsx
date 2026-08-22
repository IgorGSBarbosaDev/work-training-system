import { FormEvent, InputHTMLAttributes, useEffect, useState } from 'react'
import {
  ArrowDown,
  ArrowLeft,
  ArrowUp,
  Check,
  ChevronDown,
  ChevronRight,
  Edit3,
  Eye,
  Plus,
  Save,
  Trash2,
  UploadCloud,
  Video as VideoIcon,
  X,
} from 'lucide-react'
import { useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ApiError, api } from './api'
import {
  BackLink,
  Button,
  EmptyState,
  ErrorState,
  InlineLoading,
  LoadingState,
  PageHeader,
  StatusBadge,
} from './components'
import { apiErrorMessage } from './pages-auth'
import { useApiData } from './hooks'
import {
  ContentSummary,
  TrainingAnswerOption,
  TrainingModule,
  TrainingQuestion,
  TrainingQuestionnaire,
  TrainingVersion,
  TrainingVideo,
  UploadResponse,
} from './types'

type VersionForm = {
  workloadMinutes: number
  validityType: TrainingVersion['validityType']
  validityValue: number | null
  passingScore: number
  maxAttempts: number | null
  retryIntervalMinutes: number
}

type ModuleForm = {
  title: string
  description: string
  order: number
  status: TrainingModule['status']
}

type QuestionnaireForm = {
  title: string
  passingScore: number
  maxAttempts: number | null
  retryIntervalMinutes: number
  shuffleQuestions: boolean
  status: TrainingQuestionnaire['status']
}

type VideoForm = {
  title: string
  description: string
  order: number
  durationSeconds: number
  required: boolean
  status: TrainingVideo['status']
}

function Field({ label, className = '', ...props }: { label: string; className?: string } & InputHTMLAttributes<HTMLInputElement>) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <input {...props} className="h-10 w-full rounded-md border border-border bg-card px-3 text-sm disabled:bg-muted" />
    </label>
  )
}

function TextArea({ label, value, onChange, className = '', ...props }: {
  label: string
  value: string
  onChange: (value: string) => void
  className?: string
} & Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'value' | 'onChange'>) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <textarea {...props} value={value} onChange={(event) => onChange(event.target.value)} className="w-full rounded-md border border-border bg-card p-3 text-sm disabled:bg-muted" />
    </label>
  )
}

function Select({ label, value, onChange, options, className = '', disabled = false }: {
  label: string
  value: string
  onChange: (value: string) => void
  options: Array<[string, string]>
  className?: string
  disabled?: boolean
}) {
  return (
    <label className={`block ${className}`}>
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <select value={value} onChange={(event) => onChange(event.target.value)} disabled={disabled} className="h-10 w-full rounded-md border border-border bg-card px-3 text-sm disabled:bg-muted">
        {options.map(([option, optionLabel]) => <option key={option} value={option}>{optionLabel}</option>)}
      </select>
    </label>
  )
}

function InlineError({ message }: { message: string }) {
  return message ? <p className="text-sm text-destructive" role="alert">{message}</p> : null
}

function move<T>(items: T[], index: number, direction: -1 | 1): T[] {
  const target = index + direction
  if (target < 0 || target >= items.length) return items
  const next = [...items]
  const [item] = next.splice(index, 1)
  next.splice(target, 0, item)
  return next
}

async function reorder<T extends { id: string }>(path: string, items: T[], onSuccess: () => void) {
  const response = await api<T[]>(path, {
    method: 'PATCH',
    body: JSON.stringify({ items: items.map((item, index) => ({ id: item.id, order: index + 1 })) }),
  })
  onSuccess()
  return response
}

async function uploadTrainingVideo(file: File): Promise<UploadResponse> {
  const requested = await api<UploadResponse>('/uploads', {
    method: 'POST',
    body: JSON.stringify({
      purpose: 'TRAINING_VIDEO',
      fileName: file.name,
      contentType: file.type,
      sizeBytes: file.size,
    }),
  })
  if (!requested.uploadUrl) throw new Error('O servidor não retornou uma URL protegida de upload.')
  const uploaded = await fetch(requested.uploadUrl, {
    method: requested.method || 'PUT',
    headers: requested.requiredHeaders,
    body: file,
  })
  if (!uploaded.ok) throw new Error('O MinIO recusou o upload do vídeo.')
  return api<UploadResponse>(`/uploads/${requested.uploadId}/complete`, { method: 'POST' })
}

function readVideoDuration(file: File): Promise<number> {
  return new Promise((resolve, reject) => {
    const element = document.createElement('video')
    const objectUrl = URL.createObjectURL(file)
    element.preload = 'metadata'
    element.onloadedmetadata = () => {
      URL.revokeObjectURL(objectUrl)
      if (!Number.isFinite(element.duration) || element.duration <= 0) reject(new Error('Não foi possível identificar a duração do vídeo.'))
      else resolve(Math.ceil(element.duration))
    }
    element.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('O arquivo selecionado não parece ser um vídeo compatível.'))
    }
    element.src = objectUrl
  })
}

export function TrainingVersionEditorPage() {
  const { trainingId = '', versionId = '' } = useParams()
  const version = useApiData<TrainingVersion>(versionId ? `/training-versions/${versionId}` : null)
  const modules = useApiData<TrainingModule[]>(versionId ? `/training-versions/${versionId}/modules` : null)
  const summary = useApiData<ContentSummary>(versionId ? `/training-versions/${versionId}/content-summary` : null)
  const [form, setForm] = useState<VersionForm | null>(null)
  const [moduleForm, setModuleForm] = useState<ModuleForm>({ title: '', description: '', order: 1, status: 'ACTIVE' })
  const [saving, setSaving] = useState(false)
  const [moduleSaving, setModuleSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!version.data) return
    setForm({
      workloadMinutes: version.data.workloadMinutes,
      validityType: version.data.validityType,
      validityValue: version.data.validityValue ?? null,
      passingScore: version.data.passingScore,
      maxAttempts: version.data.maxAttempts ?? null,
      retryIntervalMinutes: version.data.retryIntervalMinutes,
    })
  }, [version.data])

  async function saveVersion(event: FormEvent) {
    event.preventDefault()
    if (!form) return
    setError('')
    if (form.workloadMinutes <= 0 || form.passingScore < 70 || form.passingScore > 100 || form.retryIntervalMinutes < 0
      || (form.maxAttempts !== null && form.maxAttempts <= 0)
      || (form.validityType === 'INDEFINITE' ? form.validityValue !== null : !form.validityValue || form.validityValue <= 0)) {
      setError('Revise os parâmetros: carga e validade devem ser positivas, nota entre 70 e 100 e tentativas válidas.')
      return
    }
    setSaving(true)
    try {
      await api(`/training-versions/${versionId}`, { method: 'PATCH', body: JSON.stringify(form) })
      version.reload()
      toast.success('Parâmetros da versão salvos.')
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  async function createModule(event: FormEvent) {
    event.preventDefault()
    setModuleSaving(true)
    try {
      await api(`/training-versions/${versionId}/modules`, {
        method: 'POST',
        body: JSON.stringify({ ...moduleForm, order: (modules.data?.length || 0) + 1 }),
      })
      setModuleForm({ title: '', description: '', order: 1, status: 'ACTIVE' })
      modules.reload()
      summary.reload()
      toast.success('Módulo adicionado ao rascunho.')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setModuleSaving(false)
    }
  }

  async function reorderModule(index: number, direction: -1 | 1) {
    if (!modules.data) return
    try {
      await reorder(`/training-versions/${versionId}/modules/order`, move(modules.data, index, direction), () => {
        modules.reload()
        summary.reload()
      })
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  async function publish() {
    try {
      await api(`/training-versions/${versionId}/publish`, { method: 'POST' })
      version.reload()
      summary.reload()
      toast.success('Versão publicada e versão anterior preservada como arquivada.')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    }
  }

  if (version.loading || modules.loading || summary.loading) return <LoadingState />
  if (version.error) return <ErrorState message={version.error} retry={version.reload} />
  if (modules.error) return <ErrorState message={modules.error} retry={modules.reload} />
  if (summary.error) return <ErrorState message={summary.error} retry={summary.reload} />
  if (!version.data || !form) return <EmptyState title="Versão não encontrada" />

  const draft = version.data.status === 'DRAFT'
  return (
    <div>
      <BackLink to={`/admin/treinamentos/${trainingId}`}><ArrowLeft size={16} /> Detalhe do treinamento</BackLink>
      <PageHeader
        eyebrow="Editor administrativo de versão"
        title={`Versão ${version.data.versionNumber}`}
        description="Edite somente rascunhos. O conteúdo publicado permanece imutável para preservar progresso, conclusões e validade."
        action={draft ? <Button onClick={publish} disabled={!summary.data?.publishable}><Check size={16} /> Publicar versão</Button> : <StatusBadge value={version.data.status} />}
      />

      {summary.data && (
        <section className="mb-6 grid gap-3 sm:grid-cols-4" aria-label="Resumo de publicação">
          {[
            ['Módulos ativos', summary.data.activeModules],
            ['Vídeos obrigatórios', summary.data.activeRequiredVideos],
            ['Questionários', summary.data.activeQuestionnaires],
            ['Questões ativas', summary.data.activeQuestions],
          ].map(([label, value]) => <div key={String(label)} className="panel p-4"><p className="text-xs text-muted-foreground">{label}</p><strong className="display mt-2 block text-2xl">{value}</strong></div>)}
        </section>
      )}
      {draft && summary.data && !summary.data.publishable && (
        <div className="mb-6 border-l-4 border-warning bg-[#fff8e7] p-4 text-sm text-warning" role="status">
          <strong>Versão incompleta.</strong>
          <ul className="mt-2 list-disc space-y-1 pl-5">{summary.data.violations.map((violation) => <li key={violation}>{violation}</li>)}</ul>
        </div>
      )}

      <div className="grid gap-6 xl:grid-cols-[.72fr_1.28fr]">
        <form onSubmit={saveVersion} className="panel h-fit">
          <header className="border-b border-border p-5"><p className="eyebrow text-primary">Regras da versão</p><h2 className="display mt-2 text-2xl font-bold">Parâmetros</h2></header>
          <div className="grid gap-4 p-5 sm:grid-cols-2">
            <Field label="Carga horária (minutos)" type="number" min={1} value={String(form.workloadMinutes)} disabled={!draft} onChange={(event) => setForm({ ...form, workloadMinutes: Number(event.target.value) })} />
            <Field label="Nota mínima (%)" type="number" min={70} max={100} step="0.01" value={String(form.passingScore)} disabled={!draft} onChange={(event) => setForm({ ...form, passingScore: Number(event.target.value) })} />
            <Select label="Tipo de validade" value={form.validityType} disabled={!draft} onChange={(value) => setForm({ ...form, validityType: value as VersionForm['validityType'], validityValue: value === 'INDEFINITE' ? null : form.validityValue || 1 })} options={[["DAYS", "Dias"], ["MONTHS", "Meses"], ["INDEFINITE", "Indeterminada"]]} />
            {form.validityType !== 'INDEFINITE' && <Field label="Prazo de validade" type="number" min={1} value={String(form.validityValue ?? '')} disabled={!draft} onChange={(event) => setForm({ ...form, validityValue: Number(event.target.value) })} />}
            <Field label="Máximo de tentativas" type="number" min={1} value={String(form.maxAttempts ?? '')} placeholder="Sem limite" disabled={!draft} onChange={(event) => setForm({ ...form, maxAttempts: event.target.value ? Number(event.target.value) : null })} />
            <Field label="Intervalo entre tentativas (min)" type="number" min={0} value={String(form.retryIntervalMinutes)} disabled={!draft} onChange={(event) => setForm({ ...form, retryIntervalMinutes: Number(event.target.value) })} />
          </div>
          {draft && <div className="border-t border-border p-5"><InlineError message={error} /><Button className="mt-3 w-full" disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : <><Save size={16} /> Salvar parâmetros</>}</Button></div>}
        </form>

        <div className="space-y-6">
          <section className="panel">
            <header className="border-b border-border p-5"><p className="eyebrow text-primary">Conteúdo ordenado</p><h2 className="display mt-2 text-2xl font-bold">Módulos</h2><p className="mt-1 text-sm text-muted-foreground">Expanda um módulo para editar vídeos e o questionário sob demanda.</p></header>
            {!modules.data?.length ? <div className="p-5"><EmptyState title="Nenhum módulo cadastrado" description="Adicione o primeiro módulo ao rascunho." /></div> : <div className="divide-y divide-border">{modules.data.map((module, index) => <ModuleEditor key={module.id} module={module} draft={draft} index={index} total={modules.data?.length || 0} onReorder={reorderModule} onChanged={() => { modules.reload(); summary.reload() }} />)}</div>}
          </section>
          {draft && <form onSubmit={createModule} className="panel p-5"><p className="eyebrow text-primary">Novo módulo</p><h2 className="display mt-2 text-2xl font-bold">Adicionar conteúdo</h2><div className="mt-5 grid gap-4 sm:grid-cols-2"><Field className="sm:col-span-2" label="Título" required value={moduleForm.title} onChange={(event) => setModuleForm({ ...moduleForm, title: event.target.value })} maxLength={150} /><TextArea className="sm:col-span-2" label="Descrição" value={moduleForm.description} onChange={(description) => setModuleForm({ ...moduleForm, description })} rows={3} maxLength={2000} /><Button className="sm:col-span-2" disabled={moduleSaving || !moduleForm.title.trim()}>{moduleSaving ? <InlineLoading label="Adicionando" /> : <><Plus size={16} /> Adicionar módulo</>}</Button></div></form>}
        </div>
      </div>
    </div>
  )
}

function ModuleEditor({ module, draft, index, total, onReorder, onChanged }: { module: TrainingModule; draft: boolean; index: number; total: number; onReorder: (index: number, direction: -1 | 1) => void; onChanged: () => void }) {
  const [expanded, setExpanded] = useState(false)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState<ModuleForm>({ title: module.title, description: module.description || '', order: module.order, status: module.status })
  const videos = useApiData<TrainingVideo[]>(expanded ? `/modules/${module.id}/videos` : null)
  const questionnaire = useOptionalQuestionnaire(module.id, expanded)

  useEffect(() => setForm({ title: module.title, description: module.description || '', order: module.order, status: module.status }), [module])

  async function saveModule(event: FormEvent) {
    event.preventDefault()
    setSaving(true)
    try {
      await api(`/modules/${module.id}`, { method: 'PATCH', body: JSON.stringify(form) })
      setEditing(false)
      onChanged()
      toast.success('Módulo atualizado.')
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  async function removeModule() {
    if (!window.confirm('Remover este módulo e todo o conteúdo do rascunho?')) return
    try {
      await api(`/modules/${module.id}`, { method: 'DELETE' })
      onChanged()
      toast.success('Módulo removido do rascunho.')
    } catch (reason) { toast.error(apiErrorMessage(reason)) }
  }

  return (
    <article className="p-5">
      <div className="flex flex-wrap items-start gap-3">
        <button type="button" onClick={() => setExpanded(!expanded)} className="grid size-9 shrink-0 place-items-center bg-muted text-primary" aria-expanded={expanded} aria-label={expanded ? 'Recolher módulo' : 'Expandir módulo'}>{expanded ? <ChevronDown size={18} /> : <ChevronRight size={18} />}</button>
        <span className="grid size-9 shrink-0 place-items-center bg-primary/10 font-mono text-xs text-primary">{module.order}</span>
        <div className="min-w-48 flex-1"><strong className="block text-sm">{module.title}</strong><span className="mt-1 block text-xs text-muted-foreground">{module.description || 'Sem descrição'}</span></div>
        <StatusBadge value={module.status} />
        {draft && <div className="flex items-center gap-1"><Button variant="ghost" className="px-2" disabled={index === 0} onClick={() => onReorder(index, -1)} aria-label="Mover módulo para cima"><ArrowUp size={16} /></Button><Button variant="ghost" className="px-2" disabled={index === total - 1} onClick={() => onReorder(index, 1)} aria-label="Mover módulo para baixo"><ArrowDown size={16} /></Button><Button variant="ghost" className="px-2" onClick={() => setEditing(!editing)} aria-label="Editar módulo"><Edit3 size={16} /></Button></div>}
      </div>
      {editing && draft && <form onSubmit={saveModule} className="mt-5 grid gap-4 border-l-2 border-primary/20 pl-4 sm:grid-cols-2"><Field label="Título" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={150} /><Select label="Status" value={form.status} onChange={(status) => setForm({ ...form, status: status as ModuleForm['status'] })} options={[["ACTIVE", "Ativo"], ["INACTIVE", "Inativo"]]} /><TextArea className="sm:col-span-2" label="Descrição" value={form.description} onChange={(description) => setForm({ ...form, description })} rows={2} maxLength={2000} /><div className="flex flex-wrap gap-2 sm:col-span-2"><Button disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : <><Save size={15} /> Salvar módulo</>}</Button><Button type="button" variant="outline" onClick={() => setEditing(false)}>Cancelar</Button><Button type="button" variant="danger" onClick={removeModule}><Trash2 size={15} /> Remover</Button></div><InlineError message={error} /></form>}
      {expanded && <div className="mt-6 space-y-6 border-l-2 border-border pl-4 sm:ml-12"><VideoSection moduleId={module.id} videos={videos.data || []} loading={videos.loading} error={videos.error} draft={draft} onChanged={() => { videos.reload(); onChanged() }} /><QuestionnaireSection moduleId={module.id} questionnaire={questionnaire.data} loading={questionnaire.loading} error={questionnaire.error} draft={draft} onChanged={() => { questionnaire.reload(); onChanged() }} /></div>}
    </article>
  )
}

function VideoSection({ moduleId, videos, loading, error, draft, onChanged }: { moduleId: string; videos: TrainingVideo[]; loading: boolean; error: string; draft: boolean; onChanged: () => void }) {
  const [adding, setAdding] = useState(false)
  async function reorderVideo(index: number, direction: -1 | 1) {
    try { await reorder(`/modules/${moduleId}/videos/order`, move(videos, index, direction), onChanged) } catch (reason) { toast.error(apiErrorMessage(reason)) }
  }
  return (
    <section>
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="display text-xl font-bold">Vídeos</h3><p className="text-xs text-muted-foreground">Arquivos privados no MinIO, carregados progressivamente.</p></div>{draft && <Button variant="outline" onClick={() => setAdding(!adding)}>{adding ? <X size={15} /> : <Plus size={15} />} {adding ? 'Fechar' : 'Adicionar vídeo'}</Button>}</div>
      {adding && draft && <VideoEditor moduleId={moduleId} videos={videos} onDone={() => { setAdding(false); onChanged() }} />}
      {loading ? <p className="mt-4 text-sm text-muted-foreground">Carregando metadados…</p> : error ? <InlineError message={error} /> : !videos.length ? <p className="mt-4 text-sm text-muted-foreground">Nenhum vídeo cadastrado.</p> : <div className="mt-4 space-y-3">{videos.map((video, index) => <VideoRow key={video.id} video={video} index={index} total={videos.length} draft={draft} onReorder={reorderVideo} onChanged={onChanged} />)}</div>}
    </section>
  )
}

function VideoRow({ video, index, total, draft, onReorder, onChanged }: { video: TrainingVideo; index: number; total: number; draft: boolean; onReorder: (index: number, direction: -1 | 1) => void; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  async function playback() {
    try {
      const response = await api<{ url: string }>(`/videos/${video.id}/playback-url`, { method: 'POST' })
      window.open(response.url, '_blank', 'noopener,noreferrer')
    } catch (reason) { toast.error(apiErrorMessage(reason)) }
  }
  async function remove() {
    if (!window.confirm('Remover este vídeo do rascunho?')) return
    try { await api(`/videos/${video.id}`, { method: 'DELETE' }); onChanged(); toast.success('Vídeo removido do rascunho.') } catch (reason) { toast.error(apiErrorMessage(reason)) }
  }
  async function changeStatus() {
    try { await api(`/videos/${video.id}/status`, { method: 'PATCH', body: JSON.stringify({ status: video.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }) }); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) }
  }
  return (
    <div className="border border-border bg-card p-4">
      <div className="flex flex-wrap items-start gap-3">
        <span className="grid size-8 place-items-center bg-muted font-mono text-xs text-primary">{video.order}</span>
        <VideoIcon className="mt-1 text-primary" size={17} />
        <div className="min-w-40 flex-1">
          <strong className="block text-sm">{video.title}</strong>
          <span className="mt-1 block text-xs text-muted-foreground">
            {Math.floor(video.durationSeconds / 60)} min · {video.required ? 'Obrigatório' : 'Opcional'} · {video.fileId ? 'MinIO verificado' : 'Referência legada'}
          </span>
        </div>
        <StatusBadge value={video.status} />
        <div className="flex items-center gap-1">
          {!draft && <Button variant="ghost" className="px-2" onClick={playback} aria-label="Testar reprodução protegida"><Eye size={16} /></Button>}
          {draft && <>
            <Button variant="ghost" className="px-2" disabled={index === 0} onClick={() => onReorder(index, -1)} aria-label="Mover vídeo para cima"><ArrowUp size={16} /></Button>
            <Button variant="ghost" className="px-2" disabled={index === total - 1} onClick={() => onReorder(index, 1)} aria-label="Mover vídeo para baixo"><ArrowDown size={16} /></Button>
            <Button variant="ghost" className="px-2" onClick={() => setEditing(!editing)} aria-label="Editar vídeo"><Edit3 size={16} /></Button>
            <Button variant="ghost" className="px-2" onClick={changeStatus} aria-label="Ativar ou inativar vídeo">{video.status === 'ACTIVE' ? <X size={16} /> : <Check size={16} />}</Button>
          </>}
        </div>
      </div>
      {editing && draft && <VideoEditor moduleId={video.moduleId} videos={[video]} video={video} onDone={() => { setEditing(false); onChanged() }} onCancel={() => setEditing(false)} />}
      {draft && <div className="mt-3 flex justify-end"><Button variant="danger" className="min-h-8 px-3 py-1 text-xs" onClick={remove}><Trash2 size={14} /> Remover vídeo</Button></div>}
    </div>
  )
}

function VideoEditor({ moduleId, videos, video, onDone, onCancel }: { moduleId: string; videos: TrainingVideo[]; video?: TrainingVideo; onDone: () => void; onCancel?: () => void }) {
  const [form, setForm] = useState<VideoForm>({ title: video?.title || '', description: video?.description || '', order: video?.order || videos.length + 1, durationSeconds: video?.durationSeconds || 1, required: video?.required ?? true, status: video?.status || 'ACTIVE' })
  const [file, setFile] = useState<File | null>(null)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  async function selectFile(next: File | null) {
    setFile(next)
    if (!next) return
    try {
      const durationSeconds = await readVideoDuration(next)
      setForm((current) => ({ ...current, durationSeconds }))
    } catch (reason) { setError(reason instanceof Error ? reason.message : 'Duração inválida.') }
  }
  async function save(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (!form.title.trim() || form.durationSeconds <= 0 || (!file && !video?.fileId && !video?.storageObjectKey)) { setError('Informe título, duração e um arquivo de vídeo concluído no MinIO.'); return }
    setSaving(true)
    try {
      let fileId = video?.fileId || null
      let storageObjectKey = video?.storageObjectKey || null
      if (file) { const uploaded = await uploadTrainingVideo(file); fileId = uploaded.fileId; storageObjectKey = null }
      const payload = { ...form, storageObjectKey, fileId }
      if (video) await api(`/videos/${video.id}`, { method: 'PATCH', body: JSON.stringify(payload) })
      else await api(`/modules/${moduleId}/videos`, { method: 'POST', body: JSON.stringify(payload) })
      toast.success(video ? 'Vídeo atualizado.' : 'Vídeo enviado e adicionado.')
      onDone()
    } catch (reason) { setError(apiErrorMessage(reason)) } finally { setSaving(false) }
  }
  return <form onSubmit={save} className="mt-4 grid gap-4 border-l-2 border-primary/20 bg-muted/30 p-4 sm:grid-cols-2"><Field className="sm:col-span-2" label="Título" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={150} /><TextArea className="sm:col-span-2" label="Descrição" value={form.description} onChange={(description) => setForm({ ...form, description })} rows={2} maxLength={2000} /><Field label="Ordem" type="number" min={1} value={String(form.order)} onChange={(event) => setForm({ ...form, order: Number(event.target.value) })} /><Field label="Duração (segundos)" type="number" min={1} value={String(form.durationSeconds)} onChange={(event) => setForm({ ...form, durationSeconds: Number(event.target.value) })} /><label className="flex items-center gap-3"><input type="checkbox" checked={form.required} onChange={(event) => setForm({ ...form, required: event.target.checked })} className="size-4 accent-[#0f6973]" /><span className="text-sm font-semibold">Vídeo obrigatório</span></label><Select label="Status" value={form.status} onChange={(status) => setForm({ ...form, status: status as VideoForm['status'] })} options={[["ACTIVE", "Ativo"], ["INACTIVE", "Inativo"]]} /><label className="block sm:col-span-2"><span className="mb-1.5 block text-sm font-semibold">Arquivo protegido {video?.fileId && <span className="font-normal text-muted-foreground">(selecione apenas para substituir)</span>}</span><input type="file" accept="video/mp4,video/webm" onChange={(event) => void selectFile(event.target.files?.[0] || null)} className="block w-full rounded-md border border-border bg-card p-2 text-sm" /></label><InlineError message={error} /><div className="flex flex-wrap gap-2 sm:col-span-2"><Button disabled={saving}>{saving ? <InlineLoading label={file ? 'Enviando ao MinIO' : 'Salvando'} /> : <><UploadCloud size={15} /> {video ? 'Salvar vídeo' : 'Enviar vídeo'}</>}</Button>{onCancel && <Button type="button" variant="outline" onClick={onCancel}>Cancelar</Button>}</div></form>
}

function useOptionalQuestionnaire(moduleId: string, enabled: boolean) {
  const [data, setData] = useState<TrainingQuestionnaire | null>(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [refresh, setRefresh] = useState(0)
  useEffect(() => {
    if (!enabled) return
    let active = true
    setLoading(true); setError('')
    api<TrainingQuestionnaire>(`/modules/${moduleId}/questionnaire`).then((result) => { if (active) setData(result) }).catch((reason) => {
      if (!active) return
      if (reason instanceof ApiError && reason.status === 404) setData(null)
      else setError(apiErrorMessage(reason))
    }).finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [moduleId, enabled, refresh])
  return { data, loading, error, reload: () => setRefresh((value) => value + 1) }
}

function QuestionnaireSection({ moduleId, questionnaire, loading, error, draft, onChanged }: { moduleId: string; questionnaire: TrainingQuestionnaire | null; loading: boolean; error: string; draft: boolean; onChanged: () => void }) {
  const [creating, setCreating] = useState(false)
  const [form, setForm] = useState<QuestionnaireForm>({ title: '', passingScore: 70, maxAttempts: 3, retryIntervalMinutes: 0, shuffleQuestions: false, status: 'ACTIVE' })
  async function create(event: FormEvent) {
    event.preventDefault(); setCreating(true)
    try { await api(`/modules/${moduleId}/questionnaire`, { method: 'POST', body: JSON.stringify(form) }); setForm({ title: '', passingScore: 70, maxAttempts: 3, retryIntervalMinutes: 0, shuffleQuestions: false, status: 'ACTIVE' }); onChanged(); toast.success('Questionário criado.') } catch (reason) { toast.error(apiErrorMessage(reason)) } finally { setCreating(false) }
  }
  return <section><div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="display text-xl font-bold">Questionário</h3><p className="text-xs text-muted-foreground">Múltipla escolha com uma única resposta correta por questão.</p></div></div>{loading ? <p className="mt-4 text-sm text-muted-foreground">Carregando questionário…</p> : error ? <InlineError message={error} /> : questionnaire ? <QuestionnaireEditor questionnaire={questionnaire} draft={draft} onChanged={onChanged} /> : draft ? <form onSubmit={create} className="mt-4 grid gap-4 border border-border bg-card p-4 sm:grid-cols-2"><Field className="sm:col-span-2" label="Título" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={150} /><Field label="Nota mínima (%)" type="number" min={70} max={100} value={String(form.passingScore)} onChange={(event) => setForm({ ...form, passingScore: Number(event.target.value) })} /><Field label="Máximo de tentativas" type="number" min={1} value={String(form.maxAttempts ?? '')} onChange={(event) => setForm({ ...form, maxAttempts: event.target.value ? Number(event.target.value) : null })} /><Field label="Intervalo (min)" type="number" min={0} value={String(form.retryIntervalMinutes)} onChange={(event) => setForm({ ...form, retryIntervalMinutes: Number(event.target.value) })} /><Select label="Status" value={form.status} onChange={(status) => setForm({ ...form, status: status as QuestionnaireForm['status'] })} options={[["ACTIVE", "Ativo"], ["INACTIVE", "Inativo"]]} /><label className="flex items-center gap-3 sm:col-span-2"><input type="checkbox" checked={form.shuffleQuestions} onChange={(event) => setForm({ ...form, shuffleQuestions: event.target.checked })} className="size-4 accent-[#0f6973]" /><span className="text-sm font-semibold">Ordenar questões aleatoriamente</span></label><Button className="sm:col-span-2" disabled={creating || !form.title.trim()}>{creating ? <InlineLoading label="Criando" /> : <><Plus size={15} /> Criar questionário</>}</Button></form> : <p className="mt-4 text-sm text-muted-foreground">Nenhum questionário cadastrado.</p>}</section>
}

function QuestionnaireEditor({ questionnaire, draft, onChanged }: { questionnaire: TrainingQuestionnaire; draft: boolean; onChanged: () => void }) {
  const questions = useApiData<TrainingQuestion[]>(`/questionnaires/${questionnaire.id}/questions`)
  const [editing, setEditing] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [form, setForm] = useState<QuestionnaireForm>({ title: questionnaire.title, passingScore: questionnaire.passingScore, maxAttempts: questionnaire.maxAttempts ?? null, retryIntervalMinutes: questionnaire.retryIntervalMinutes, shuffleQuestions: questionnaire.shuffleQuestions, status: questionnaire.status })
  const [newStatement, setNewStatement] = useState('')
  useEffect(() => setForm({ title: questionnaire.title, passingScore: questionnaire.passingScore, maxAttempts: questionnaire.maxAttempts ?? null, retryIntervalMinutes: questionnaire.retryIntervalMinutes, shuffleQuestions: questionnaire.shuffleQuestions, status: questionnaire.status }), [questionnaire])
  async function save(event: FormEvent) { event.preventDefault(); setSaving(true); setError(''); try { await api(`/questionnaires/${questionnaire.id}`, { method: 'PATCH', body: JSON.stringify(form) }); setEditing(false); onChanged(); toast.success('Questionário atualizado.') } catch (reason) { setError(apiErrorMessage(reason)) } finally { setSaving(false) } }
  async function createQuestion(event: FormEvent) { event.preventDefault(); try { await api(`/questionnaires/${questionnaire.id}/questions`, { method: 'POST', body: JSON.stringify({ statement: newStatement, order: (questions.data?.length || 0) + 1, status: 'ACTIVE' }) }); setNewStatement(''); questions.reload(); onChanged(); toast.success('Questão adicionada.') } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function reorderQuestion(index: number, direction: -1 | 1) { if (!questions.data) return; try { await reorder(`/questionnaires/${questionnaire.id}/questions/order`, move(questions.data, index, direction), () => { questions.reload(); onChanged() }) } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function remove() { if (!window.confirm('Remover o questionário e suas questões do rascunho?')) return; try { await api(`/modules/${questionnaire.moduleId}/questionnaire`, { method: 'DELETE' }); onChanged(); toast.success('Questionário removido do rascunho.') } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function changeStatus() { try { await api(`/questionnaires/${questionnaire.id}/status`, { method: 'PATCH', body: JSON.stringify({ status: questionnaire.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }) }); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  return <div className="mt-4 space-y-4 border border-border bg-card p-4"><div className="flex flex-wrap items-start gap-3"><div className="min-w-40 flex-1"><strong className="block text-sm">{questionnaire.title}</strong><span className="mt-1 block text-xs text-muted-foreground">Nota {questionnaire.passingScore}% · {questionnaire.maxAttempts ? `${questionnaire.maxAttempts} tentativas` : 'Tentativas sem limite'} · {questionnaire.shuffleQuestions ? 'ordem aleatória' : 'ordem definida'}</span></div><StatusBadge value={questionnaire.status} />{draft && <div className="flex gap-1"><Button variant="ghost" className="px-2" onClick={() => setEditing(!editing)} aria-label="Editar questionário"><Edit3 size={16} /></Button><Button variant="ghost" className="px-2" onClick={changeStatus} aria-label="Ativar ou inativar questionário">{questionnaire.status === 'ACTIVE' ? <X size={16} /> : <Check size={16} />}</Button><Button variant="danger" className="min-h-8 px-2" onClick={remove} aria-label="Remover questionário"><Trash2 size={15} /></Button></div>}</div>{editing && draft && <form onSubmit={save} className="grid gap-4 border-l-2 border-primary/20 pl-4 sm:grid-cols-2"><Field className="sm:col-span-2" label="Título" value={form.title} onChange={(event) => setForm({ ...form, title: event.target.value })} maxLength={150} /><Field label="Nota mínima (%)" type="number" min={70} max={100} value={String(form.passingScore)} onChange={(event) => setForm({ ...form, passingScore: Number(event.target.value) })} /><Field label="Máximo de tentativas" type="number" min={1} value={String(form.maxAttempts ?? '')} onChange={(event) => setForm({ ...form, maxAttempts: event.target.value ? Number(event.target.value) : null })} /><Field label="Intervalo (min)" type="number" min={0} value={String(form.retryIntervalMinutes)} onChange={(event) => setForm({ ...form, retryIntervalMinutes: Number(event.target.value) })} /><Select label="Status" value={form.status} onChange={(status) => setForm({ ...form, status: status as QuestionnaireForm['status'] })} options={[["ACTIVE", "Ativo"], ["INACTIVE", "Inativo"]]} /><label className="flex items-center gap-3 sm:col-span-2"><input type="checkbox" checked={form.shuffleQuestions} onChange={(event) => setForm({ ...form, shuffleQuestions: event.target.checked })} className="size-4 accent-[#0f6973]" /><span className="text-sm font-semibold">Ordenar questões aleatoriamente</span></label><InlineError message={error} /><Button disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : <><Save size={15} /> Salvar questionário</>}</Button></form>}{questions.loading ? <p className="text-sm text-muted-foreground">Carregando questões…</p> : questions.error ? <InlineError message={questions.error} /> : <div className="space-y-3">{questions.data?.map((question, index) => <QuestionEditor key={question.id} question={question} draft={draft} index={index} total={questions.data?.length || 0} onReorder={reorderQuestion} onChanged={() => { questions.reload(); onChanged() }} />)}</div>}{draft && <form onSubmit={createQuestion} className="flex flex-col gap-3 border-t border-border pt-4 sm:flex-row"><input required value={newStatement} onChange={(event) => setNewStatement(event.target.value)} maxLength={2000} placeholder="Enunciado da nova questão" className="h-10 min-w-0 flex-1 rounded-md border border-border px-3 text-sm" /><Button disabled={!newStatement.trim()}><Plus size={15} /> Adicionar questão</Button></form>}</div>
}

function QuestionEditor({ question, draft, index, total, onReorder, onChanged }: { question: TrainingQuestion; draft: boolean; index: number; total: number; onReorder: (index: number, direction: -1 | 1) => void; onChanged: () => void }) {
  const options = useApiData<TrainingAnswerOption[]>(`/questions/${question.id}/options`)
  const [editing, setEditing] = useState(false)
  const [statement, setStatement] = useState(question.statement)
  const [saving, setSaving] = useState(false)
  const [newOption, setNewOption] = useState('')
  useEffect(() => setStatement(question.statement), [question.statement])
  async function save() { setSaving(true); try { await api(`/questions/${question.id}`, { method: 'PATCH', body: JSON.stringify({ statement, order: question.order, status: question.status }) }); setEditing(false); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } finally { setSaving(false) } }
  async function changeStatus() { try { await api(`/questions/${question.id}/status`, { method: 'PATCH', body: JSON.stringify({ status: question.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }) }); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function remove() { if (!window.confirm('Remover esta questão e suas alternativas do rascunho?')) return; try { await api(`/questions/${question.id}`, { method: 'DELETE' }); onChanged(); toast.success('Questão removida.') } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function addOption(event: FormEvent) { event.preventDefault(); try { await api(`/questions/${question.id}/options`, { method: 'POST', body: JSON.stringify({ text: newOption, order: (options.data?.length || 0) + 1, correct: false, status: 'ACTIVE' }) }); setNewOption(''); options.reload(); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  return <article className="border border-border p-3"><div className="flex items-start gap-2"><span className="grid size-7 shrink-0 place-items-center bg-muted font-mono text-[10px] text-primary">{question.order}</span><div className="min-w-0 flex-1"><strong className="block text-sm">{question.statement}</strong><span className="mt-1 block text-xs text-muted-foreground">{options.data?.length || 0} alternativas · {options.data?.filter((option) => option.correct && option.status === 'ACTIVE').length || 0} correta ativa</span></div><StatusBadge value={question.status} />{draft && <div className="flex gap-0.5"><Button variant="ghost" className="px-1.5" disabled={index === 0} onClick={() => onReorder(index, -1)} aria-label="Mover questão para cima"><ArrowUp size={14} /></Button><Button variant="ghost" className="px-1.5" disabled={index === total - 1} onClick={() => onReorder(index, 1)} aria-label="Mover questão para baixo"><ArrowDown size={14} /></Button><Button variant="ghost" className="px-1.5" onClick={() => setEditing(!editing)} aria-label="Editar questão"><Edit3 size={14} /></Button><Button variant="ghost" className="px-1.5" onClick={changeStatus} aria-label="Ativar ou inativar questão">{question.status === 'ACTIVE' ? <X size={14} /> : <Check size={14} />}</Button><Button variant="danger" className="px-1.5" onClick={remove} aria-label="Remover questão"><Trash2 size={14} /></Button></div>}</div>{editing && draft && <div className="mt-3 flex flex-col gap-2 sm:flex-row"><input value={statement} onChange={(event) => setStatement(event.target.value)} maxLength={2000} className="h-9 min-w-0 flex-1 rounded-md border border-border px-3 text-sm" /><Button onClick={save} disabled={saving}>{saving ? <InlineLoading label="Salvando" /> : <Save size={14} />}</Button></div>}<div className="mt-3 space-y-2">{options.loading ? <p className="text-xs text-muted-foreground">Carregando alternativas…</p> : options.error ? <InlineError message={options.error} /> : options.data?.map((option, optionIndex) => <OptionEditor key={option.id} option={option} index={optionIndex} total={options.data?.length || 0} draft={draft} onReorder={async (direction) => { if (!options.data) return; try { await reorder(`/questions/${question.id}/options/order`, move(options.data, optionIndex, direction), () => { options.reload(); onChanged() }) } catch (reason) { toast.error(apiErrorMessage(reason)) } }} onChanged={() => { options.reload(); onChanged() }} />)}</div>{draft && <form onSubmit={addOption} className="mt-3 flex gap-2"><input required value={newOption} onChange={(event) => setNewOption(event.target.value)} maxLength={1000} placeholder="Texto da nova alternativa" className="h-9 min-w-0 flex-1 rounded-md border border-border px-3 text-sm" /><Button className="min-h-9 px-3" disabled={!newOption.trim()}><Plus size={14} /> Alternativa</Button></form>}</article>
}

function OptionEditor({ option, index, total, draft, onReorder, onChanged }: { option: TrainingAnswerOption; index: number; total: number; draft: boolean; onReorder: (direction: -1 | 1) => void; onChanged: () => void }) {
  const [editing, setEditing] = useState(false)
  const [text, setText] = useState(option.text)
  const [saving, setSaving] = useState(false)
  useEffect(() => setText(option.text), [option.text])
  async function update(correct: boolean, status = option.status) { setSaving(true); try { await api(`/answer-options/${option.id}`, { method: 'PATCH', body: JSON.stringify({ text, order: option.order, correct, status }) }); setEditing(false); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } finally { setSaving(false) } }
  async function markCorrect() {
    setSaving(true)
    try {
      const options = await api<TrainingAnswerOption[]>(`/questions/${option.questionId}/options`)
      const current = options.find((item) => item.correct && item.status === 'ACTIVE' && item.id !== option.id)
      if (current) await api(`/answer-options/${current.id}`, { method: 'PATCH', body: JSON.stringify({ text: current.text, order: current.order, correct: false, status: current.status }) })
      await api(`/answer-options/${option.id}`, { method: 'PATCH', body: JSON.stringify({ text, order: option.order, correct: true, status: 'ACTIVE' }) })
      onChanged()
    } catch (reason) { toast.error(apiErrorMessage(reason)) } finally { setSaving(false) }
  }
  async function changeStatus() { try { await api(`/answer-options/${option.id}/status`, { method: 'PATCH', body: JSON.stringify({ status: option.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE' }) }); onChanged() } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  async function remove() { if (!window.confirm('Remover esta alternativa do rascunho?')) return; try { await api(`/answer-options/${option.id}`, { method: 'DELETE' }); onChanged(); toast.success('Alternativa removida.') } catch (reason) { toast.error(apiErrorMessage(reason)) } }
  return <div className="flex flex-wrap items-center gap-2 border border-border/70 px-3 py-2"><button type="button" disabled={!draft || saving} onClick={() => void markCorrect()} aria-pressed={option.correct} aria-label={option.correct ? 'Resposta correta' : 'Definir como resposta correta'} className={`grid size-6 shrink-0 place-items-center rounded-full border ${option.correct ? 'border-primary bg-primary text-white' : 'border-border text-transparent'}`}><Check size={13} /></button>{editing && draft ? <input autoFocus value={text} onChange={(event) => setText(event.target.value)} maxLength={1000} className="h-8 min-w-40 flex-1 rounded-md border border-border px-2 text-sm" /> : <span className="min-w-32 flex-1 text-sm">{option.text}</span>}{option.correct && <span className="text-[10px] font-semibold text-primary">Correta</span>}{draft && <div className="flex gap-0.5"><Button variant="ghost" className="min-h-7 px-1.5" disabled={index === 0} onClick={() => onReorder(-1)} aria-label="Mover alternativa para cima"><ArrowUp size={13} /></Button><Button variant="ghost" className="min-h-7 px-1.5" disabled={index === total - 1} onClick={() => onReorder(1)} aria-label="Mover alternativa para baixo"><ArrowDown size={13} /></Button>{editing ? <Button variant="ghost" className="min-h-7 px-1.5" onClick={() => void update(option.correct)} disabled={saving}><Save size={13} /></Button> : <Button variant="ghost" className="min-h-7 px-1.5" onClick={() => setEditing(true)} aria-label="Editar alternativa"><Edit3 size={13} /></Button>}<Button variant="ghost" className="min-h-7 px-1.5" onClick={changeStatus} aria-label="Ativar ou inativar alternativa">{option.status === 'ACTIVE' ? <X size={13} /> : <Check size={13} />}</Button><Button variant="danger" className="min-h-7 px-1.5" onClick={remove} aria-label="Remover alternativa"><Trash2 size={13} /></Button></div>}</div>
}
