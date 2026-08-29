import { FormEvent, useRef, useState } from 'react'
import {
  AlertCircle,
  ArrowRight,
  BadgeCheck,
  Bell,
  BookOpen,
  CheckCircle2,
  Clock3,
  Download,
  FileCheck2,
  LockKeyhole,
  Play,
  QrCode,
  ShieldAlert,
} from 'lucide-react'
import { QRCodeSVG } from 'qrcode.react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api, User } from './api'
import { useAuth } from './auth'
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
  StatusBadge,
} from './components'
import { useApiData } from './hooks'
import {
  AssessmentAvailability,
  AssessmentResult,
  Assignment,
  AssignmentDetail,
  Certificate,
  Notification,
  PageResponse,
  QrCodeData,
  Qualification,
  Questionnaire,
} from './types'
import { apiErrorMessage } from './pages-auth'

const assignmentStatusFilters = [
  { value: '', label: 'Todas' },
  { value: 'NOT_STARTED', label: 'Não iniciadas' },
  { value: 'IN_PROGRESS', label: 'Em andamento' },
  { value: 'AWAITING_ASSESSMENT', label: 'Em avaliação' },
  { value: 'COMPLETED', label: 'Concluídas' },
  { value: 'EXPIRED', label: 'Vencidas' },
]

export function MyAssignmentsPage() {
  const [status, setStatus] = useState('')
  const [page, setPage] = useState(0)
  const query = new URLSearchParams({ page: String(page), size: '10', sort: 'dueDate,asc' })
  if (status) query.set('status', status)
  const state = useApiData<PageResponse<Assignment>>(`/me/training-assignments?${query}`)

  return (
    <div>
      <PageHeader
        eyebrow="Capacitações atribuídas"
        title="Minhas atribuições"
        description="Acompanhe prazos, progresso e requisitos necessários para suas atividades."
        action={<LinkButton to="/meu/qualificacoes" variant="outline">Ver qualificações</LinkButton>}
      />
      <div className="mb-5 flex gap-2 overflow-x-auto pb-2" role="tablist" aria-label="Filtrar atribuições por status">
        {assignmentStatusFilters.map((filter) => (
          <button
            key={filter.value}
            role="tab"
            aria-selected={status === filter.value}
            onClick={() => {
              setStatus(filter.value)
              setPage(0)
            }}
            className={`whitespace-nowrap border-b-2 px-3 py-2 text-sm font-semibold ${
              status === filter.value
                ? 'border-primary text-primary'
                : 'border-transparent text-muted-foreground hover:text-foreground'
            }`}
          >
            {filter.label}
          </button>
        ))}
      </div>
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState
          icon={BookOpen}
          title="Nenhuma atribuição neste filtro"
          description="Quando um treinamento for atribuído a você, ele aparecerá aqui."
        />
      ) : (
        <>
          <div className="space-y-3">
            {state.data.content.map((assignment) => (
              <AssignmentRow key={assignment.id} assignment={assignment} />
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

function AssignmentRow({ assignment }: { assignment: Assignment }) {
  return (
    <Link
      to={`/meu/atribuicoes/${assignment.id}`}
      className="panel grid gap-4 p-4 transition hover:border-primary hover:shadow-md sm:grid-cols-[1fr_auto] sm:items-center"
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <StatusBadge value={assignment.status} />
          {assignment.recertification && (
            <span className="rounded-md border border-border bg-muted px-2 py-1 text-[10px] font-semibold uppercase">
              Reciclagem
            </span>
          )}
        </div>
        <h2 className="display mt-3 text-2xl font-bold">{assignment.training.name}</h2>
        <p className="mt-1 text-xs text-muted-foreground">
          Versão {assignment.trainingVersion} · prioridade {assignment.priority.toLocaleLowerCase('pt-BR')} · prazo{' '}
          {formatDate(assignment.dueDate)}
        </p>
      </div>
      <span className="inline-flex items-center gap-2 text-sm font-semibold text-primary">
        Ver detalhes <ArrowRight size={16} />
      </span>
    </Link>
  )
}

export function AssignmentDetailPage() {
  const { assignmentId = '' } = useParams()
  const state = useApiData<AssignmentDetail>(assignmentId ? `/me/training-assignments/${assignmentId}` : null)
  const [starting, setStarting] = useState(false)

  async function start() {
    setStarting(true)
    try {
      await api(`/training-assignments/${assignmentId}/start`, { method: 'POST' })
      toast.success('Treinamento iniciado.')
      state.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setStarting(false)
    }
  }

  if (state.loading) return <LoadingState />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState title="Atribuição não encontrada" />

  const assignment = state.data
  const videos = assignment.learningPath.modules.flatMap((module) =>
    module.videos.map((video) => ({ ...video, moduleTitle: module.title })),
  )
  const required = videos.filter((video) => video.required)
  const completed = required.filter((video) => video.completed)
  const percentage = required.length ? Math.round((completed.length / required.length) * 100) : 100
  const nextVideo = videos.find((video) => !video.completed) || videos[0]
  const questionnaire = assignment.learningPath.modules
    .map((module) => module.questionnaire)
    .find((item) => item?.available)

  return (
    <div>
      <BackLink to="/meu/atribuicoes">Minhas atribuições</BackLink>
      <section className="panel">
        <header className="border-b border-border p-5 sm:p-7">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="eyebrow text-primary">Versão {assignment.trainingVersion}</p>
              <h1 className="display mt-2 text-3xl font-bold">{assignment.training.name}</h1>
              <p className="mt-2 text-sm text-muted-foreground">
                Atribuído em {formatDate(assignment.assignedDate)} · prazo {formatDate(assignment.dueDate)}
              </p>
            </div>
            <StatusBadge value={assignment.status} />
          </div>
        </header>
        <div className="grid gap-7 p-5 sm:p-7 lg:grid-cols-[1.35fr_.65fr]">
          <div>
            <h2 className="display text-2xl font-bold">Seu progresso</h2>
            <p className="mt-1 text-sm text-muted-foreground">
              Cada vídeo obrigatório precisa atingir 80%. Quando houver avaliação, a nota mínima será informada pelo
              backend.
            </p>
            <div className="mt-5 border border-border p-4">
              <div className="mb-2 flex justify-between text-sm font-semibold">
                <span>Vídeos obrigatórios concluídos</span>
                <span className="text-primary">{completed.length}/{required.length}</span>
              </div>
              <div className="h-2 bg-muted" role="progressbar" aria-valuenow={percentage} aria-valuemin={0} aria-valuemax={100}>
                <div className="h-full bg-primary" style={{ width: `${percentage}%` }} />
              </div>
              <div className="mt-4 divide-y divide-border">
                {videos.map((video) => (
                  <div key={video.id} className="flex items-center gap-3 py-3">
                    {video.completed ? (
                      <CheckCircle2 className="shrink-0 text-success" size={18} />
                    ) : (
                      <Clock3 className="shrink-0 text-warning" size={18} />
                    )}
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold">{video.title}</p>
                      <p className="text-xs text-muted-foreground">
                        {video.moduleTitle} · {Math.round(video.percentageWatched)}% assistido
                      </p>
                    </div>
                    {video.required && <span className="text-[10px] font-semibold uppercase text-muted-foreground">Obrigatório</span>}
                  </div>
                ))}
              </div>
            </div>
            <div className="mt-5 flex flex-wrap gap-3">
              {assignment.status === 'NOT_STARTED' ? (
                <Button onClick={start} disabled={starting}>
                  {starting ? <InlineLoading label="Iniciando" /> : <><Play size={16} /> Iniciar treinamento</>}
                </Button>
              ) : nextVideo && !['COMPLETED', 'EXPIRED', 'CANCELLED', 'WAIVED'].includes(assignment.status) ? (
                <LinkButton to={`/meu/atribuicoes/${assignment.id}/videos/${nextVideo.id}`}>
                  <Play size={16} /> Continuar treinamento
                </LinkButton>
              ) : null}
              {questionnaire && (
                <LinkButton to={`/meu/atribuicoes/${assignment.id}/questionarios/${questionnaire.id}`} variant="outline">
                  <FileCheck2 size={16} /> Iniciar questionário
                </LinkButton>
              )}
            </div>
          </div>
          <aside className="border-t border-border pt-5 lg:border-l lg:border-t-0 lg:pl-6 lg:pt-0">
            <h2 className="eyebrow text-muted-foreground">Detalhes da atribuição</h2>
            <dl className="mt-4 space-y-4 text-sm">
              <Info label="Origem" value={assignment.origin} />
              <Info label="Prioridade" value={assignment.priority} />
              <Info label="Prazo" value={formatDate(assignment.dueDate)} />
              <Info label="Versão preservada" value={String(assignment.trainingVersion)} />
              <Info label="Avaliação" value={assignment.learningPath.assessment.summary || (assignment.learningPath.assessment.required ? 'Obrigatória' : 'Não exigida')} />
            </dl>
          </aside>
        </div>
      </section>
    </div>
  )
}

type PlaybackResponse = { url: string; expiresAt: string; resumeAtSeconds: number }
type VideoProgressResponse = {
  positionSeconds: number
  watchedSeconds: number
  percentageWatched: number
  completed: boolean
  assignmentStatus: string
}

export function TrainingPlayerPage() {
  const { assignmentId = '', videoId = '' } = useParams()
  const detail = useApiData<AssignmentDetail>(assignmentId ? `/me/training-assignments/${assignmentId}` : null)
  const [playback, setPlayback] = useState<PlaybackResponse | null>(null)
  const [loadingPlayback, setLoadingPlayback] = useState(false)
  const [saving, setSaving] = useState(false)
  const [saveMessage, setSaveMessage] = useState('')
  const lastPosition = useRef(0)
  const watchedSinceSave = useRef(0)
  const progressInitialized = useRef(false)

  const videoInfo = detail.data?.learningPath.modules
    .flatMap((module) => module.videos.map((video) => ({ ...video, moduleTitle: module.title })))
    .find((video) => video.id === videoId)

  async function requestPlayback() {
    setLoadingPlayback(true)
    try {
      const response = await api<PlaybackResponse>(`/videos/${videoId}/playback-url`, { method: 'POST' })
      setPlayback(response)
      lastPosition.current = response.resumeAtSeconds
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setLoadingPlayback(false)
    }
  }

  function trackTime(event: React.SyntheticEvent<HTMLVideoElement>) {
    const current = event.currentTarget.currentTime
    const delta = current - lastPosition.current
    if (delta > 0 && delta <= 2.5 && !event.currentTarget.paused) watchedSinceSave.current += delta
    lastPosition.current = current
  }

  async function initializeProgress(element: HTMLVideoElement) {
    if (progressInitialized.current) return
    progressInitialized.current = true
    try {
      await api(`/training-assignments/${assignmentId}/videos/${videoId}/progress`, {
        method: 'PUT',
        body: JSON.stringify({
          positionSeconds: Math.floor(element.currentTime), watchedSeconds: 0, reportedPercentage: 0,
          eventAt: new Date().toISOString(), eventId: crypto.randomUUID(), finalEvent: false,
        }),
      })
    } catch (reason) {
      progressInitialized.current = false
      setSaveMessage(apiErrorMessage(reason))
    }
  }

  async function saveProgress(element: HTMLVideoElement, finalEvent: boolean) {
    if (saving || watchedSinceSave.current <= 0) return
    const watchedSeconds = watchedSinceSave.current
    watchedSinceSave.current = 0
    setSaving(true)
    setSaveMessage('Salvando progresso…')
    try {
      const response = await api<VideoProgressResponse>(
        `/training-assignments/${assignmentId}/videos/${videoId}/progress`,
        {
          method: 'PUT',
          body: JSON.stringify({
            positionSeconds: Math.floor(element.currentTime),
            watchedSeconds: Number(watchedSeconds.toFixed(3)),
            reportedPercentage: element.duration
              ? Number(((element.currentTime / element.duration) * 100).toFixed(2))
              : 0,
            eventAt: new Date().toISOString(),
            eventId: crypto.randomUUID(),
            finalEvent,
          }),
        },
      )
      setSaveMessage(response.completed ? 'Vídeo obrigatório concluído.' : 'Progresso salvo.')
      detail.reload()
    } catch (reason) {
      watchedSinceSave.current += watchedSeconds
      setSaveMessage(apiErrorMessage(reason))
    } finally {
      setSaving(false)
    }
  }

  if (detail.loading) return <LoadingState />
  if (detail.error) return <ErrorState message={detail.error} retry={detail.reload} />
  if (!detail.data || !videoInfo) return <EmptyState title="Vídeo não encontrado nesta atribuição" />

  const modules = detail.data.learningPath.modules
  return (
    <div>
      <BackLink to={`/meu/atribuicoes/${assignmentId}`}>Detalhe da atribuição</BackLink>
      <div className="grid gap-6 xl:grid-cols-[1fr_330px]">
        <section>
          <div className="relative grid aspect-video place-items-center overflow-hidden bg-[#172529]">
            {!playback ? (
              <button
                onClick={requestPlayback}
                disabled={loadingPlayback}
                className="relative grid size-16 place-items-center rounded-full border border-[#82c8c4] bg-[#204c50] text-[#bce8e5] transition hover:bg-primary hover:text-white disabled:opacity-50"
                aria-label="Carregar vídeo protegido"
              >
                {loadingPlayback ? <InlineLoading label="" /> : <Play fill="currentColor" size={25} />}
              </button>
            ) : (
              <video
                className="h-full w-full bg-black"
                controls
                preload="metadata"
                src={playback.url}
                onLoadedMetadata={(event) => {
                  event.currentTarget.currentTime = playback.resumeAtSeconds
                  lastPosition.current = playback.resumeAtSeconds
                }}
                onTimeUpdate={trackTime}
                onPlay={(event) => void initializeProgress(event.currentTarget)}
                onPause={(event) => void saveProgress(event.currentTarget, false)}
                onEnded={(event) => void saveProgress(event.currentTarget, true)}
                onError={() => setSaveMessage('Não foi possível reproduzir o vídeo protegido.')}
              >
                Seu navegador não suporta reprodução de vídeo.
              </video>
            )}
          </div>
          <div className="mt-5 flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="eyebrow text-primary">{videoInfo.moduleTitle} · vídeo {videoInfo.required ? 'obrigatório' : 'opcional'}</p>
              <h1 className="display mt-2 text-3xl font-bold">{videoInfo.title}</h1>
              <p className="mt-2 text-sm text-muted-foreground">{detail.data.training.name} · versão {detail.data.trainingVersion}</p>
            </div>
            <StatusBadge value={videoInfo.completed ? 'COMPLETED' : 'IN_PROGRESS'} />
          </div>
          <div className="mt-6 border-l-4 border-primary bg-[#edf7f6] p-4 text-sm text-[#1b6066]">
            <strong>Regra de conclusão:</strong> o backend registra o tempo efetivamente assistido e conclui o vídeo
            obrigatório somente ao atingir 80%. Saltos no vídeo não contam como tempo assistido.
          </div>
          <div className="mt-5">
            <div className="mb-2 flex justify-between text-sm font-semibold">
              <span>Progresso registrado</span>
              <span className="text-primary">{Math.round(videoInfo.percentageWatched)}%</span>
            </div>
            <div className="h-2 bg-muted">
              <div className="h-full bg-primary" style={{ width: `${Math.min(100, videoInfo.percentageWatched)}%` }} />
            </div>
            {saveMessage && <p className="mt-3 text-xs text-muted-foreground" aria-live="polite">{saveMessage}</p>}
          </div>
        </section>
        <aside className="panel h-fit">
          <header className="border-b border-border p-4">
            <h2 className="display text-xl font-bold">Caminho de aprendizagem</h2>
            <p className="text-xs text-muted-foreground">{modules.length} módulo(s)</p>
          </header>
          <div className="divide-y divide-border">
            {modules.flatMap((module) =>
              module.videos.map((video) => (
                <Link
                  key={video.id}
                  to={`/meu/atribuicoes/${assignmentId}/videos/${video.id}`}
                  className={`flex items-center gap-3 p-4 text-sm transition hover:bg-muted/50 ${
                    video.id === videoId ? 'border-l-2 border-primary bg-primary/5' : ''
                  }`}
                >
                  {video.completed ? <CheckCircle2 className="text-success" size={17} /> : <Play size={17} />}
                  <span className="min-w-0 flex-1">
                    <strong className="block truncate">{video.title}</strong>
                    <span className="text-xs text-muted-foreground">{Math.round(video.percentageWatched)}%</span>
                  </span>
                </Link>
              )),
            )}
          </div>
        </aside>
      </div>
    </div>
  )
}

export function QuestionnairePage() {
  const { assignmentId = '', questionnaireId = '' } = useParams()
  const questionnaire = useApiData<Questionnaire>(
    assignmentId && questionnaireId
      ? `/training-assignments/${assignmentId}/questionnaires/${questionnaireId}`
      : null,
  )
  const availability = useApiData<AssessmentAvailability>(
    assignmentId && questionnaireId
      ? `/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/availability`
      : null,
  )
  const [answers, setAnswers] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!questionnaire.data) return
    if (Object.keys(answers).length !== questionnaire.data.questions.length) {
      setError('Responda todas as questões antes de enviar.')
      return
    }
    setSubmitting(true)
    setError('')
    try {
      const result = await api<AssessmentResult>(
        `/training-assignments/${assignmentId}/questionnaires/${questionnaireId}/attempts`,
        {
          method: 'POST',
          headers: { 'Idempotency-Key': crypto.randomUUID() },
          body: JSON.stringify({
            answers: questionnaire.data.questions.map((question) => ({
              questionId: question.id,
              answerOptionId: answers[question.id],
            })),
          }),
        },
      )
      navigate(`/meu/atribuicoes/${assignmentId}/resultado`, { state: result })
    } catch (reason) {
      setError(apiErrorMessage(reason))
    } finally {
      setSubmitting(false)
    }
  }

  if (questionnaire.loading || availability.loading) return <LoadingState />
  if (questionnaire.error) return <ErrorState message={questionnaire.error} retry={questionnaire.reload} />
  if (availability.error) return <ErrorState message={availability.error} retry={availability.reload} />
  if (!questionnaire.data || !availability.data) return <EmptyState title="Questionário indisponível" />

  if (!availability.data.available) {
    return (
      <div className="mx-auto max-w-3xl">
        <BackLink to={`/meu/atribuicoes/${assignmentId}`}>Detalhe da atribuição</BackLink>
        <EmptyState
          icon={LockKeyhole}
          title="Nova tentativa indisponível"
          description={
            availability.data.nextAttemptAt
              ? `Você poderá tentar novamente em ${formatDateTime(availability.data.nextAttemptAt)}.`
              : 'Conclua os conteúdos obrigatórios ou verifique o limite de tentativas.'
          }
        />
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl">
      <BackLink to={`/meu/atribuicoes/${assignmentId}`}>Detalhe da atribuição</BackLink>
      <form onSubmit={submit} className="panel">
        <header className="border-b border-border p-5 sm:p-7">
          <p className="eyebrow text-primary">Avaliação obrigatória</p>
          <h1 className="display mt-2 text-3xl font-bold">{questionnaire.data.title}</h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Nota mínima: {availability.data.passingScore}% · tentativas usadas: {availability.data.attemptsUsed}
            {availability.data.maxAttempts ? ` de ${availability.data.maxAttempts}` : ''}
          </p>
        </header>
        <div className="divide-y divide-border">
          {questionnaire.data.questions.map((question, index) => (
            <fieldset key={question.id} className="p-5 sm:p-7">
              <legend className="w-full">
                <span className="eyebrow text-muted-foreground">Questão {index + 1}</span>
                <span className="display mt-2 block text-2xl font-bold">{question.statement}</span>
              </legend>
              <div className="mt-5 space-y-3">
                {question.options.map((option) => (
                  <label
                    key={option.id}
                    className={`flex cursor-pointer items-start gap-3 border p-4 ${
                      answers[question.id] === option.id ? 'border-primary bg-primary/5' : 'border-border hover:border-primary/40'
                    }`}
                  >
                    <input
                      type="radio"
                      name={question.id}
                      value={option.id}
                      checked={answers[question.id] === option.id}
                      onChange={() => setAnswers((current) => ({ ...current, [question.id]: option.id }))}
                      className="mt-1 accent-[#0f6973]"
                    />
                    <span className="text-sm leading-6">{option.text}</span>
                  </label>
                ))}
              </div>
            </fieldset>
          ))}
        </div>
        <footer className="border-t border-border p-5 sm:flex sm:items-center sm:justify-between">
          <p className="mb-3 text-xs text-muted-foreground sm:mb-0">
            {Object.keys(answers).length} de {questionnaire.data.questions.length} respondidas
          </p>
          <Button disabled={submitting}>
            {submitting ? <InlineLoading label="Enviando" /> : <>Enviar respostas <FileCheck2 size={16} /></>}
          </Button>
        </footer>
        {error && <p className="border-t border-destructive/20 bg-red-50 p-4 text-sm text-destructive" role="alert">{error}</p>}
      </form>
    </div>
  )
}

export function AssessmentResultPage() {
  const { assignmentId = '' } = useParams()
  const location = useLocation()
  const result = location.state as AssessmentResult | null

  if (!result) {
    return (
      <div className="mx-auto max-w-3xl">
        <EmptyState
          icon={FileCheck2}
          title="Resultado não disponível nesta sessão"
          description="Abra o detalhe da atribuição para consultar o histórico de tentativas registrado no servidor."
          action={<LinkButton to={`/meu/atribuicoes/${assignmentId}`}>Voltar à atribuição</LinkButton>}
        />
      </div>
    )
  }

  const approved = result.result === 'APPROVED'
  return (
    <div className="mx-auto max-w-3xl">
      <section className="panel p-6 text-center sm:p-10">
        <span className={`mx-auto grid size-16 place-items-center rounded-full ${approved ? 'bg-[#e0f1e6] text-success' : 'bg-[#fff1ef] text-destructive'}`}>
          {approved ? <CheckCircle2 size={32} /> : <AlertCircle size={32} />}
        </span>
        <p className="eyebrow mt-6 text-primary">Resultado da avaliação</p>
        <h1 className="display mt-3 text-4xl font-bold">
          {approved ? 'Avaliação aprovada' : 'Nota mínima não atingida'}
        </h1>
        <p className="mx-auto mt-3 max-w-lg text-sm text-muted-foreground">
          O resultado foi calculado e registrado pelo backend. A conclusão depende de todos os conteúdos e
          questionários exigidos na versão atribuída.
        </p>
        <div className="mx-auto mt-8 grid max-w-md grid-cols-2 divide-x divide-border border border-border">
          <div className="p-5">
            <p className="eyebrow text-muted-foreground">Sua nota</p>
            <p className="display mt-2 text-4xl font-bold text-primary">{result.score}%</p>
          </div>
          <div className="p-5">
            <p className="eyebrow text-muted-foreground">Nota mínima</p>
            <p className="display mt-2 text-4xl font-bold">{result.passingScore}%</p>
          </div>
        </div>
        <div className="mt-7 flex flex-wrap justify-center gap-3">
          <LinkButton to={`/meu/atribuicoes/${assignmentId}`}>Ver atribuição</LinkButton>
          <LinkButton to="/meu/dashboard" variant="outline">Voltar ao painel</LinkButton>
        </div>
      </section>
    </div>
  )
}

export function MyQualificationsPage() {
  const { session } = useAuth()
  const employeeId = session?.user.employeeId
  const state = useApiData<PageResponse<Qualification>>(
    employeeId ? `/employees/${employeeId}/qualifications?size=50` : null,
  )

  return (
    <div>
      <PageHeader
        eyebrow="Situação operacional"
        title="Minhas qualificações"
        description="A situação é calculada pelo backend a partir dos treinamentos concluídos e válidos."
      />
      {!employeeId ? (
        <ErrorState message="Seu usuário não está vinculado a um colaborador. Solicite a correção ao administrador." />
      ) : state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState icon={ShieldAlert} title="Nenhuma atividade atribuída" />
      ) : (
        <div className="grid gap-4 lg:grid-cols-2">
          {state.data.content.map((qualification) => (
            <QualificationCard key={qualification.id} qualification={qualification} />
          ))}
        </div>
      )}
    </div>
  )
}

function QualificationCard({ qualification }: { qualification: Qualification }) {
  return (
    <article className="panel p-5">
      <div className="flex items-start justify-between gap-3">
        <span className="grid size-10 place-items-center bg-primary/10 text-primary">
          {qualification.status === 'AVAILABLE' ? <CheckCircle2 size={20} /> : <ShieldAlert size={20} />}
        </span>
        <StatusBadge value={qualification.status} />
      </div>
      <h2 className="display mt-5 text-2xl font-bold">{qualification.activity.name}</h2>
      <p className="mt-2 text-sm text-muted-foreground">
        {qualification.nextExpirationDate
          ? `Próximo vencimento: ${formatDate(qualification.nextExpirationDate)}`
          : 'Sem vencimento próximo registrado.'}
      </p>
      {!!qualification.blockingReasons.length && (
        <div className="mt-4 border-l-4 border-destructive bg-red-50 p-4">
          <p className="text-xs font-semibold uppercase text-destructive">Motivos do bloqueio</p>
          <ul className="mt-2 space-y-1 text-sm text-[#7d3228]">
            {qualification.blockingReasons.map((reason, index) => (
              <li key={`${reason.type}-${reason.trainingId}-${index}`}>
                {reason.trainingName || reason.type}
                {reason.expirationDate ? ` · venceu em ${formatDate(reason.expirationDate)}` : ''}
              </li>
            ))}
          </ul>
        </div>
      )}
      <p className="mt-4 border-t border-border pt-4 text-xs leading-5 text-muted-foreground">{qualification.disclaimer}</p>
    </article>
  )
}

export function MyCertificatesPage() {
  const state = useApiData<PageResponse<Certificate>>('/me/certificates?size=20')
  return (
    <div>
      <PageHeader
        eyebrow="Evidências de conclusão"
        title="Meus certificados"
        description="Consulte certificados válidos, revogados ou em processamento gerados pelo sistema."
      />
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState icon={BadgeCheck} title="Nenhum certificado disponível" />
      ) : (
        <div className="space-y-3">
          {state.data.content.map((certificate) => (
            <Link
              key={certificate.id}
              to={`/meu/certificados/${certificate.id}`}
              className="panel flex flex-wrap items-center gap-4 p-4 transition hover:border-primary"
            >
              <span className="grid size-10 place-items-center bg-primary/10 text-primary"><BadgeCheck size={20} /></span>
              <span className="min-w-60 flex-1">
                <strong className="block text-sm">Certificado {certificate.validationCode}</strong>
                <span className="mt-1 block text-xs text-muted-foreground">
                  Emitido em {formatDate(certificate.issuedDate)} · geração {certificate.generationNumber}
                </span>
              </span>
              <StatusBadge value={certificate.status} />
              <ArrowRight className="text-muted-foreground" size={17} />
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}

type CertificateDownload = { url: string; expiresAt: string }

export function CertificateDetailPage() {
  const { certificateId = '' } = useParams()
  const state = useApiData<Certificate>(certificateId ? `/certificates/${certificateId}` : null)
  const [downloading, setDownloading] = useState(false)

  async function download() {
    setDownloading(true)
    try {
      const response = await api<CertificateDownload>(`/certificates/${certificateId}/download`)
      window.open(response.url, '_blank', 'noopener,noreferrer')
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setDownloading(false)
    }
  }

  if (state.loading) return <LoadingState />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState title="Certificado não encontrado" />

  const certificate = state.data
  return (
    <div>
      <BackLink to="/meu/certificados">Meus certificados</BackLink>
      <section className="panel p-5 sm:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <StatusBadge value={certificate.status} />
            <h1 className="display mt-4 text-3xl font-bold">Certificado de conclusão</h1>
            <p className="mt-2 font-mono text-sm text-muted-foreground">{certificate.validationCode}</p>
          </div>
          {certificate.status !== 'REVOKED' && (
            <Button onClick={download} disabled={downloading}>
              {downloading ? <InlineLoading label="Preparando" /> : <><Download size={16} /> Baixar PDF</>}
            </Button>
          )}
        </div>
        <div className="mt-8 border border-[#b8d9d6] bg-[#f2f9f8] p-8 text-center sm:p-12">
          <BadgeCheck className="mx-auto text-primary" size={42} />
          <p className="eyebrow mt-4 text-primary">Certificado registrado</p>
          <h2 className="display mt-4 text-3xl font-bold">Documento #{certificate.generationNumber}</h2>
          <p className="mt-4 text-sm text-muted-foreground">Emitido em {formatDate(certificate.issuedDate)}</p>
          {certificate.revocationReason && (
            <p className="mt-4 border-l-4 border-destructive bg-red-50 p-3 text-sm text-destructive">
              Revogado: {certificate.revocationReason}
            </p>
          )}
        </div>
      </section>
    </div>
  )
}

export function MyQrCodePage() {
  const state = useApiData<QrCodeData>('/me/qr-code')
  if (state.loading) return <LoadingState />
  if (state.error) return <ErrorState message={state.error} retry={state.reload} />
  if (!state.data) return <EmptyState icon={QrCode} title="QR Code indisponível" />

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        eyebrow="Validação em campo"
        title="Meu QR Code"
        description="Gestores autorizados podem consultar seus treinamentos e qualificações sem expor dados pessoais desnecessários."
      />
      <section className="panel p-6 text-center sm:p-10">
        <StatusBadge value={state.data.status} />
        {state.data.status === 'ACTIVE' ? (
          <>
            <div className="mx-auto mt-7 w-fit bg-white p-5 shadow-inner">
              <QRCodeSVG
                value={state.data.verificationUrl}
                size={220}
                level="M"
                title="QR Code individual do colaborador"
              />
            </div>
            <p className="mt-7 break-all font-mono text-xs tracking-[.08em] text-muted-foreground">{state.data.token}</p>
          </>
        ) : (
          <div className="mx-auto mt-7 max-w-md border-l-4 border-destructive bg-red-50 p-5 text-left text-sm text-destructive">
            Este QR Code está revogado. Solicite ao administrador a geração de um novo código.
          </div>
        )}
        <p className="mx-auto mt-4 max-w-md text-sm leading-6 text-muted-foreground">
          O token é aleatório e a consulta autenticada é registrada para auditoria.
        </p>
        {state.data.status === 'ACTIVE' && (
          <Button variant="outline" className="mt-6" onClick={() => window.print()}>
            <Download size={16} /> Imprimir
          </Button>
        )}
      </section>
    </div>
  )
}

export function NotificationsPage() {
  const [page, setPage] = useState(0)
  const state = useApiData<PageResponse<Notification>>(`/me/notifications?page=${page}&size=15`)
  const [updating, setUpdating] = useState<string | null>(null)

  async function markRead(notification: Notification) {
    if (notification.readAt) return
    setUpdating(notification.id)
    try {
      await api(`/me/notifications/${notification.id}/read`, { method: 'PATCH' })
      state.reload()
    } catch (reason) {
      toast.error(apiErrorMessage(reason))
    } finally {
      setUpdating(null)
    }
  }

  return (
    <div>
      <PageHeader
        eyebrow="Atualizações da plataforma"
        title="Notificações"
        description="Atribuições, prazos, conclusões e mudanças de qualificação registradas para você."
      />
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : !state.data?.content.length ? (
        <EmptyState icon={Bell} title="Nenhuma notificação" />
      ) : (
        <>
          <div className="panel divide-y divide-border">
            {state.data.content.map((notification) => (
              <button
                key={notification.id}
                className={`flex w-full gap-4 p-4 text-left hover:bg-muted/40 ${notification.readAt ? '' : 'border-l-4 border-l-primary'}`}
                onClick={() => markRead(notification)}
                disabled={updating === notification.id}
              >
                <span className="grid size-9 shrink-0 place-items-center bg-primary/10 text-primary"><Bell size={17} /></span>
                <span className="min-w-0 flex-1">
                  <strong className="block text-sm">{notification.title}</strong>
                  <span className="mt-1 block text-sm leading-6 text-muted-foreground">{notification.message}</span>
                  <span className="mt-2 block text-[10px] text-muted-foreground">{formatDateTime(notification.createdAt)}</span>
                </span>
                {!notification.readAt && <span className="mt-2 size-2 rounded-full bg-primary" aria-label="Não lida" />}
              </button>
            ))}
          </div>
          <Pagination page={page} totalPages={state.data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  )
}

export function ProfilePage() {
  const { session } = useAuth()
  const state = useApiData<User>('/auth/me')
  const user = state.data || session?.user

  return (
    <div className="mx-auto max-w-4xl">
      <PageHeader
        eyebrow="Conta e permissões"
        title="Meu perfil"
        description="Consulte os dados de acesso vinculados à sua sessão."
      />
      {state.loading ? (
        <LoadingState />
      ) : state.error ? (
        <ErrorState message={state.error} retry={state.reload} />
      ) : user ? (
        <div className="grid gap-6 lg:grid-cols-[.7fr_1.3fr]">
          <aside className="panel p-6">
            <span className="grid size-24 place-items-center rounded-md bg-primary/10 text-primary"><BadgeCheck size={42} /></span>
            <h2 className="display mt-5 break-all text-2xl font-bold">{user.email}</h2>
            <div className="mt-4"><StatusBadge value={user.status} /></div>
          </aside>
          <section className="panel p-6">
            <h2 className="display text-2xl font-bold">Dados da conta</h2>
            <dl className="mt-6 grid gap-5 sm:grid-cols-2">
              <Info label="E-mail" value={user.email} />
              <Info label="Perfil" value={user.role} />
              <Info label="Identificador" value={user.id} />
              <Info label="Colaborador vinculado" value={user.employeeId || 'Sem vínculo'} />
            </dl>
            {!!user.permissions?.length && (
              <div className="mt-6 border-t border-border pt-5">
                <p className="eyebrow text-muted-foreground">Permissões adicionais</p>
                <div className="mt-3 flex flex-wrap gap-2">
                  {user.permissions.map((permission) => (
                    <span key={permission} className="rounded-md border border-border bg-muted px-2 py-1 text-xs">
                      {permission}
                    </span>
                  ))}
                </div>
              </div>
            )}
          </section>
        </div>
      ) : null}
    </div>
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
