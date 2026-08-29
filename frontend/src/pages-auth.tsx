import { FormEvent, ReactNode, useState } from 'react'
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  BadgeCheck,
  CheckCircle2,
  Eye,
  EyeOff,
  LockKeyhole,
  Search,
} from 'lucide-react'
import { Link, Navigate, useLocation, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { api, ApiError } from './api'
import { homeForRole, useAuth } from './auth'
import { Button, formatDate, InlineLoading, StatusBadge } from './components'
import { useApiData } from './hooks'
import { Logo } from './layout'

function AuthShell({ children }: { children: ReactNode }) {
  return (
    <main className="relative min-h-screen bg-background">
      <header className="absolute inset-x-0 top-0 z-10 px-5 py-5 sm:px-8 lg:px-12">
        <Logo />
      </header>
      <section className="flex min-h-screen items-center justify-center px-5 py-24 sm:px-8 lg:px-12">
        <div className="w-full max-w-[430px]">{children}</div>
      </section>
    </main>
  )
}

function Field({
  label,
  type = 'text',
  value,
  onChange,
  autoComplete,
  required = true,
}: {
  label: string
  type?: string
  value: string
  onChange: (value: string) => void
  autoComplete?: string
  required?: boolean
}) {
  const [visible, setVisible] = useState(false)
  const password = type === 'password'
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-semibold">{label}</span>
      <span className="relative block">
        <input
          required={required}
          type={password && visible ? 'text' : type}
          value={value}
          onChange={(event) => onChange(event.target.value)}
          autoComplete={autoComplete}
          className="h-11 w-full rounded-md border border-border bg-card px-3 pr-11 text-sm focus:border-primary"
        />
        {password && (
          <button
            type="button"
            aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
            onClick={() => setVisible((current) => !current)}
            className="absolute inset-y-0 right-0 grid w-11 place-items-center text-muted-foreground"
          >
            {visible ? <EyeOff size={17} /> : <Eye size={17} />}
          </button>
        )}
      </span>
    </label>
  )
}

export function LoginPage() {
  const { session, signIn, sessionExpired, dismissSessionExpired } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const location = useLocation()
  const navigate = useNavigate()

  const returnPath = (() => {
    const from = (location.state as { from?: unknown } | null)?.from
		return typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : null
  })()

  if (session) {
		const destination = returnPath || homeForRole(session.user.role)
    return <Navigate to={destination} replace />
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setLoading(true)
    try {
      const next = await signIn(email.trim(), password)
		navigate(returnPath || homeForRole(next.user.role), { replace: true })
      toast.success('Acesso realizado com segurança.')
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível entrar.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell>
      <p className="eyebrow text-primary">Acesso protegido</p>
      <h1 className="display mt-3 text-4xl font-bold">Entrar no sistema</h1>
      <p className="mt-2 text-sm text-muted-foreground">Use suas credenciais corporativas para continuar.</p>
      {sessionExpired && (
        <div className="mt-6 flex gap-3 rounded-md border border-[#e8d29f] bg-[#fff8e7] p-4 text-sm text-warning" role="alert">
          <LockKeyhole className="shrink-0" size={18} />
          <div>
            <strong>Sua sessão expirou.</strong>
            <p className="mt-1">Entre novamente para continuar com segurança.</p>
            <button className="mt-2 font-semibold underline" onClick={dismissSessionExpired}>
              Entendi
            </button>
          </div>
        </div>
      )}
      <form onSubmit={submit} className="mt-8 space-y-5">
        <Field label="E-mail corporativo" type="email" value={email} onChange={setEmail} autoComplete="username" />
        <Field label="Senha" type="password" value={password} onChange={setPassword} autoComplete="current-password" />
        <Link to="/recuperar-senha" className="block text-sm font-semibold text-primary hover:underline">
          Esqueci minha senha
        </Link>
        {error && (
          <div className="flex gap-2 rounded-md border border-destructive/30 bg-red-50 p-3 text-sm text-destructive" role="alert">
            <AlertCircle className="shrink-0" size={17} /> {error}
          </div>
        )}
        <Button disabled={loading} className="w-full">
          {loading ? <InlineLoading label="Autenticando" /> : <>Entrar <ArrowRight size={17} /></>}
        </Button>
      </form>
      <p className="mt-8 border-t border-border pt-5 text-xs leading-5 text-muted-foreground">
        O acesso e as permissões são definidos pelo administrador da organização. Não há perfis demonstrativos ou
        credenciais simuladas nesta interface.
      </p>
      <Link to="/validar-certificado" className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-primary">
        <BadgeCheck size={16} /> Validar um certificado
      </Link>
    </AuthShell>
  )
}

export function RecoveryPage() {
  const [email, setEmail] = useState('')
  const [loading, setLoading] = useState(false)
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setError('')
    try {
      await api<void>(
        '/auth/password/forgot',
        { method: 'POST', body: JSON.stringify({ email: email.trim() }) },
        false,
      )
      setSent(true)
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível registrar a solicitação.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell>
      <Link to="/login" className="inline-flex items-center gap-2 text-sm font-semibold text-primary">
        <ArrowLeft size={16} /> Voltar ao acesso
      </Link>
      {sent ? (
        <>
          <div className="mt-8 inline-flex items-center gap-2 rounded-md border border-[#b7d8c8] bg-[#edf7f0] px-3 py-2 text-xs font-semibold text-success">
            <CheckCircle2 size={15} /> Solicitação registrada
          </div>
          <h1 className="display mt-5 text-4xl font-bold">Confira seu e-mail</h1>
          <p className="mt-3 text-sm leading-6 text-muted-foreground">
            Se houver uma conta vinculada ao endereço informado, enviaremos instruções. Por segurança, o sistema não
            confirma se a conta existe.
          </p>
        </>
      ) : (
        <>
          <p className="eyebrow mt-8 text-primary">Segurança da conta</p>
          <h1 className="display mt-3 text-4xl font-bold">Recuperar acesso</h1>
          <p className="mt-2 text-sm leading-6 text-muted-foreground">
            Informe seu e-mail corporativo para receber as instruções.
          </p>
          <form onSubmit={submit} className="mt-8 space-y-5">
            <Field label="E-mail corporativo" type="email" value={email} onChange={setEmail} autoComplete="email" />
            {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
            <Button disabled={loading} className="w-full">
              {loading ? <InlineLoading label="Enviando" /> : <>Enviar instruções <ArrowRight size={17} /></>}
            </Button>
          </form>
        </>
      )}
    </AuthShell>
  )
}

export function ResetPasswordPage() {
  const { token: pathToken = '' } = useParams()
  const [searchParams] = useSearchParams()
  const token = pathToken || searchParams.get('token') || ''
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    if (!token) {
      setError('O link de recuperação é inválido.')
      return
    }
    if (password !== confirmation) {
      setError('As senhas informadas não coincidem.')
      return
    }
    setLoading(true)
    try {
      await api<void>(
        '/auth/password/reset',
        { method: 'POST', body: JSON.stringify({ token, newPassword: password }) },
        false,
      )
      toast.success('Senha atualizada. Entre com sua nova senha.')
      navigate('/login', { replace: true })
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : 'Não foi possível atualizar a senha.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <AuthShell>
      <p className="eyebrow text-primary">Segurança da conta</p>
      <h1 className="display mt-3 text-4xl font-bold">Definir nova senha</h1>
      <p className="mt-2 text-sm text-muted-foreground">Use uma senha que atenda à política de segurança da organização.</p>
      <form onSubmit={submit} className="mt-8 space-y-5">
        <Field label="Nova senha" type="password" value={password} onChange={setPassword} autoComplete="new-password" />
        <Field
          label="Confirmar nova senha"
          type="password"
          value={confirmation}
          onChange={setConfirmation}
          autoComplete="new-password"
        />
        {error && <p className="text-sm text-destructive" role="alert">{error}</p>}
        <Button disabled={loading} className="w-full">
          {loading ? <InlineLoading label="Atualizando" /> : 'Atualizar senha'}
        </Button>
      </form>
    </AuthShell>
  )
}

type CertificateValidation = {
  valid: boolean
  status: string
  employeeName: string
  employeeRegistration: string
  trainingName: string
  completedAt: string
  expiresAt?: string | null
  issuedAt: string
}

export function CertificateValidationPage() {
  const [code, setCode] = useState('')
  const [submittedCode, setSubmittedCode] = useState<string | null>(null)
  const state = useApiData<CertificateValidation>(
    submittedCode ? `/certificate-validations/${encodeURIComponent(submittedCode)}` : null,
  )

  return (
    <AuthShell>
      <Link to="/login" className="inline-flex items-center gap-2 text-sm font-semibold text-primary">
        <ArrowLeft size={16} /> Voltar ao acesso
      </Link>
      <p className="eyebrow mt-8 text-primary">Consulta pública</p>
      <h1 className="display mt-3 text-4xl font-bold">Validar certificado</h1>
      <p className="mt-2 text-sm leading-6 text-muted-foreground">
        Informe o código impresso no certificado para conferir sua autenticidade.
      </p>
      <form
        className="mt-7 flex gap-2"
        onSubmit={(event) => {
          event.preventDefault()
          setSubmittedCode(code.trim())
        }}
      >
        <label className="relative flex-1">
          <span className="sr-only">Código de validação</span>
          <Search className="absolute left-3 top-3 text-muted-foreground" size={17} />
          <input
            required
            value={code}
            onChange={(event) => setCode(event.target.value.toUpperCase())}
            placeholder="Código de validação"
            className="h-11 w-full rounded-md border border-border bg-card pl-10 pr-3 font-mono text-sm"
          />
        </label>
        <Button disabled={state.loading}>{state.loading ? 'Validando…' : 'Validar'}</Button>
      </form>
      {state.error && (
        <div className="mt-5 border-l-4 border-destructive bg-red-50 p-4 text-sm text-destructive" role="alert">
          {state.error}
        </div>
      )}
      {state.data && (
        <section className="panel mt-6 p-5">
          <div className="flex items-center justify-between gap-3">
            <BadgeCheck className={state.data.valid ? 'text-primary' : 'text-destructive'} size={26} />
            <StatusBadge value={state.data.status} />
          </div>
          <h2 className="display mt-5 text-2xl font-bold">{state.data.trainingName}</h2>
          <p className="mt-2 text-sm text-muted-foreground">
            {state.data.valid
              ? 'Certificado válido conforme os registros da organização.'
              : 'Este certificado não está válido no momento.'}
          </p>
          <dl className="mt-4 grid gap-3 border-t border-border pt-4 text-sm sm:grid-cols-2">
            <Info label="Colaborador" value={state.data.employeeName} />
            <Info label="Matrícula" value={state.data.employeeRegistration} />
            <Info label="Conclusão" value={formatDate(state.data.completedAt)} />
            <Info label="Emissão" value={formatDate(state.data.issuedAt)} />
            <Info label="Vencimento" value={formatDate(state.data.expiresAt)} />
            <Info label="Código consultado" value={submittedCode ?? '—'} />
          </dl>
        </section>
      )}
    </AuthShell>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <dt className="text-xs text-muted-foreground">{label}</dt>
      <dd className="mt-1 font-semibold">{value}</dd>
    </div>
  )
}

export function ErrorPage() {
  const { code = '404' } = useParams()
  const { session } = useAuth()
  const description =
    code === '403'
      ? 'Você não possui permissão para acessar esta área.'
      : code === '401'
        ? 'Sua sessão não é válida ou expirou.'
        : 'A página solicitada não foi encontrada.'
  return (
    <AuthShell>
      <div className="inline-flex items-center gap-2 rounded-md border border-destructive/25 bg-red-50 px-3 py-2 text-xs font-semibold text-destructive">
        <LockKeyhole size={14} /> Acesso restrito
      </div>
      <h1 className="display mt-5 text-6xl font-bold">{code}</h1>
      <h2 className="display mt-2 text-3xl font-bold">{description}</h2>
      <p className="mt-3 text-sm text-muted-foreground">
        Volte para uma área autorizada ou entre novamente se sua sessão tiver expirado.
      </p>
      <Link
        to={session ? homeForRole(session.user.role) : '/login'}
        className="mt-7 inline-flex min-h-10 items-center gap-2 rounded-md bg-primary px-4 text-sm font-semibold text-white"
      >
        <ArrowLeft size={16} /> Voltar
      </Link>
    </AuthShell>
  )
}

export function apiErrorMessage(reason: unknown): string {
  if (reason instanceof ApiError || reason instanceof Error) return reason.message
  return 'Não foi possível concluir a operação.'
}
