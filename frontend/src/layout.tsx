import { useMemo, useState } from 'react'
import {
  Activity,
  BadgeCheck,
  BarChart3,
  Bell,
  BookOpen,
  BriefcaseBusiness,
  Building2,
  ChevronDown,
  ChevronRight,
  ClipboardList,
  Clock3,
  GraduationCap,
  LayoutDashboard,
  LogOut,
  Mail,
  Menu,
  PanelLeftClose,
  PanelLeftOpen,
  QrCode,
  ScrollText,
  Settings,
  ShieldCheck,
  UserCog,
  UserRound,
  Users,
  X,
} from 'lucide-react'
import { Link, NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { api, Role } from './api'
import { useAuth } from './auth'
import { formatDateTime } from './components'
import { useApiData } from './hooks'
import { Notification, PageResponse } from './types'

type NavItem = {
  label: string
  to: string
  icon: typeof LayoutDashboard
}

const navigation: Record<Role, NavItem[]> = {
  EMPLOYEE: [
    { label: 'Visão geral', to: '/meu/dashboard', icon: LayoutDashboard },
    { label: 'Minhas atribuições', to: '/meu/atribuicoes', icon: BookOpen },
    { label: 'Qualificações', to: '/meu/qualificacoes', icon: ShieldCheck },
    { label: 'Certificados', to: '/meu/certificados', icon: BadgeCheck },
    { label: 'Meu QR Code', to: '/meu/qr-code', icon: QrCode },
    { label: 'Notificações', to: '/meu/notificacoes', icon: Bell },
  ],
  MANAGER: [
    { label: 'Visão da equipe', to: '/equipe/dashboard', icon: LayoutDashboard },
    { label: 'Colaboradores', to: '/equipe/colaboradores', icon: Users },
    { label: 'Qualificações', to: '/equipe/qualificacoes', icon: ShieldCheck },
    { label: 'Verificar QR', to: '/equipe/verificar-qr', icon: QrCode },
    { label: 'Relatórios', to: '/equipe/relatorios', icon: BarChart3 },
  ],
  SUPERVISOR: [
    { label: 'Visão da equipe', to: '/equipe/dashboard', icon: LayoutDashboard },
    { label: 'Colaboradores', to: '/equipe/colaboradores', icon: Users },
    { label: 'Qualificações', to: '/equipe/qualificacoes', icon: ShieldCheck },
    { label: 'Verificar QR', to: '/equipe/verificar-qr', icon: QrCode },
    { label: 'Relatórios', to: '/equipe/relatorios', icon: BarChart3 },
  ],
  ADMIN: [
    { label: 'Visão geral', to: '/admin/dashboard', icon: LayoutDashboard },
    { label: 'Colaboradores', to: '/admin/colaboradores', icon: Users },
    { label: 'Organização', to: '/admin/organizacao', icon: Building2 },
    { label: 'Atividades', to: '/admin/atividades', icon: BriefcaseBusiness },
    { label: 'Treinamentos', to: '/admin/treinamentos', icon: BookOpen },
    { label: 'Atribuições', to: '/admin/atribuicoes', icon: ClipboardList },
    { label: 'Expirações', to: '/admin/expiracoes', icon: Clock3 },
    { label: 'Certificados', to: '/admin/certificados', icon: BadgeCheck },
    { label: 'Usuários', to: '/admin/usuarios', icon: UserCog },
    { label: 'Notificações', to: '/admin/notificacoes', icon: Bell },
    { label: 'E-mails', to: '/admin/emails', icon: Mail },
    { label: 'Auditoria', to: '/admin/auditoria', icon: ScrollText },
    { label: 'Relatórios', to: '/admin/relatorios', icon: BarChart3 },
    { label: 'Configurações', to: '/admin/configuracoes', icon: Settings },
  ],
}

export function Logo({ light = false, compact = false }: { light?: boolean; compact?: boolean }) {
  return (
    <div className="flex items-center gap-3" aria-label="worksafe training system">
      <span
        className={`grid size-9 shrink-0 place-items-center border ${
          light ? 'border-[#5a7478] bg-[#294449]' : 'border-primary/25 bg-primary/10'
        }`}
      >
        <ShieldCheck className={light ? 'text-[#8ed2cc]' : 'text-primary'} size={20} />
      </span>
      {!compact && (
        <span>
          <span className={`display block text-[17px] font-bold leading-none ${light ? 'text-white' : ''}`}>
            work<span className={light ? 'text-[#8ed2cc]' : 'text-primary'}>safe</span>
          </span>
          <span
            className={`mt-1 block font-mono text-[9px] uppercase tracking-[.16em] ${
              light ? 'text-[#a9bec0]' : 'text-muted-foreground'
            }`}
          >
            training system
          </span>
        </span>
      )}
    </div>
  )
}

function Sidebar({
  items,
  compact,
  closeMobile,
  onCompact,
}: {
  items: NavItem[]
  compact: boolean
  closeMobile: () => void
  onCompact: () => void
}) {
  return (
    <aside className={`flex h-full flex-col bg-sidebar text-white ${compact ? 'w-16' : 'w-64'}`}>
      <div className={`flex h-[65px] items-center border-b border-sidebar-border ${compact ? 'justify-center' : 'justify-between px-5'}`}>
        <Logo light compact={compact} />
        {!compact && (
          <button
            className="hidden text-[#a9bec0] hover:text-white lg:block"
            onClick={onCompact}
            aria-label="Recolher navegação"
          >
            <PanelLeftClose size={18} />
          </button>
        )}
        <button className="text-[#a9bec0] lg:hidden" onClick={closeMobile} aria-label="Fechar navegação">
          <X size={20} />
        </button>
      </div>
      <nav className="flex-1 overflow-y-auto p-3" aria-label="Navegação principal">
        <p className={`eyebrow pb-2 text-[#9eb3b5] ${compact ? 'sr-only' : 'px-3'}`}>Navegação</p>
        {items.map((item) => {
          const Icon = item.icon
          return (
            <NavLink
              key={item.to}
              to={item.to}
              title={compact ? item.label : undefined}
              onClick={closeMobile}
              className={({ isActive }) =>
                `mb-1 flex items-center rounded-md py-2.5 text-sm font-semibold transition ${
                  compact ? 'justify-center px-2' : 'gap-3 px-3'
                } ${
                  isActive
                    ? 'bg-sidebar-accent text-white'
                    : 'text-[#bdd0d1] hover:bg-sidebar-accent hover:text-white'
                }`
              }
            >
              <Icon size={17} />
              {!compact && item.label}
            </NavLink>
          )
        })}
      </nav>
      <div className="border-t border-sidebar-border p-3">
        <div
          title={compact ? 'Organização atual' : undefined}
          className={`rounded-md bg-[#29383d] text-[#b9cecf] ${compact ? 'grid size-10 place-items-center' : 'p-3'}`}
        >
          {compact ? (
            <Building2 size={17} />
          ) : (
            <>
              <p className="flex items-center gap-2 text-xs font-semibold text-white">
                <Building2 size={15} className="text-[#8ed2cc]" /> Organização
              </p>
              <p className="mt-1 pl-6 text-[11px]">Ambiente corporativo</p>
            </>
          )}
        </div>
      </div>
    </aside>
  )
}

function NotificationMenu({ close }: { close: () => void }) {
  const { data, loading, error, reload } = useApiData<PageResponse<Notification>>('/me/notifications?size=4')

  async function markAllRead() {
    try {
      await api<void>('/me/notifications/read-all', { method: 'PATCH' })
      toast.success('Notificações marcadas como lidas.')
      reload()
    } catch (reason) {
      toast.error(reason instanceof Error ? reason.message : 'Não foi possível atualizar as notificações.')
    }
  }

  return (
    <section className="absolute right-0 top-11 z-40 w-[min(22rem,calc(100vw-2rem))] border border-border bg-card shadow-xl">
      <header className="flex items-center justify-between border-b border-border p-4">
        <strong className="text-sm">Notificações</strong>
        <button className="text-xs font-semibold text-primary hover:underline" onClick={markAllRead}>
          Marcar todas
        </button>
      </header>
      <div className="max-h-80 overflow-y-auto">
        {loading && <p className="p-4 text-sm text-muted-foreground">Carregando notificações…</p>}
        {error && (
          <button className="w-full p-4 text-left text-sm text-destructive" onClick={reload}>
            {error} Tentar novamente.
          </button>
        )}
        {data?.content.length === 0 && <p className="p-4 text-sm text-muted-foreground">Nenhuma notificação.</p>}
        {data?.content.map((notification) => (
          <Link
            key={notification.id}
            to="/meu/notificacoes"
            onClick={close}
            className={`block border-b border-border p-4 last:border-b-0 hover:bg-muted/50 ${
              notification.readAt ? '' : 'border-l-2 border-l-primary'
            }`}
          >
            <strong className="block text-sm">{notification.title}</strong>
            <span className="mt-1 line-clamp-2 block text-xs leading-5 text-muted-foreground">
              {notification.message}
            </span>
            <span className="mt-2 block text-[10px] text-muted-foreground">
              {formatDateTime(notification.createdAt)}
            </span>
          </Link>
        ))}
      </div>
      <Link
        to="/meu/notificacoes"
        onClick={close}
        className="flex items-center justify-center gap-1 border-t border-border p-3 text-xs font-semibold text-primary"
      >
        Ver todas <ChevronRight size={14} />
      </Link>
    </section>
  )
}

export function AppShell() {
  const { session, role, signOut } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [mobileOpen, setMobileOpen] = useState(false)
  const [compact, setCompact] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const { data: unread, reload: reloadUnread } = useApiData<{ count: number }>('/me/notifications/unread-count')

  const items = navigation[role!]
  const current = useMemo(
    () => items.find((item) => location.pathname.startsWith(item.to))?.label || 'Início',
    [items, location.pathname],
  )

  async function handleSignOut() {
    try {
      await signOut()
      navigate('/login', { replace: true })
    } catch {
      navigate('/login', { replace: true })
    }
  }

  const desktopOffset = compact ? 'lg:ml-16' : 'lg:ml-64'
  const initials = session?.user.email.slice(0, 2).toUpperCase() || 'US'

  return (
    <div className="min-h-screen bg-background">
      <div className="fixed inset-y-0 left-0 z-30 hidden lg:block">
        <Sidebar items={items} compact={compact} closeMobile={() => setMobileOpen(false)} onCompact={() => setCompact(true)} />
      </div>
      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden">
          <button
            aria-label="Fechar menu"
            className="absolute inset-0 bg-black/45"
            onClick={() => setMobileOpen(false)}
          />
          <div className="relative h-full w-64">
            <Sidebar
              items={items}
              compact={false}
              closeMobile={() => setMobileOpen(false)}
              onCompact={() => setCompact(true)}
            />
          </div>
        </div>
      )}

      <header className={`sticky top-0 z-20 border-b border-border bg-background/95 backdrop-blur ${desktopOffset}`}>
        <div className="flex h-16 items-center gap-3 px-4 sm:px-6">
          <button
            className="grid size-9 place-items-center lg:hidden"
            onClick={() => setMobileOpen(true)}
            aria-label="Abrir navegação"
          >
            <Menu size={20} />
          </button>
          {compact && (
            <button
              className="hidden size-9 place-items-center text-muted-foreground hover:text-primary lg:grid"
              onClick={() => setCompact(false)}
              aria-label="Expandir navegação"
            >
              <PanelLeftOpen size={18} />
            </button>
          )}
          <div className="min-w-0 flex-1">
            <div className="hidden items-center gap-2 text-xs text-muted-foreground sm:flex">
              <span>Início</span>
              <ChevronRight size={13} />
              <strong className="truncate text-foreground">{current}</strong>
            </div>
            <p className="display truncate text-xl font-bold sm:hidden">{current}</p>
          </div>
          <div className="relative">
            <button
              aria-label={`Notificações${unread?.count ? `, ${unread.count} não lidas` : ''}`}
              className="relative grid size-9 place-items-center hover:bg-muted"
              onClick={() => {
                setNotificationsOpen((open) => !open)
                setProfileOpen(false)
                reloadUnread()
              }}
            >
              <Bell size={19} />
              {!!unread?.count && <span className="absolute right-1 top-1 size-2 rounded-full bg-destructive ring-2 ring-background" />}
            </button>
            {notificationsOpen && <NotificationMenu close={() => setNotificationsOpen(false)} />}
          </div>
          <div className="relative">
            <button
              className="flex items-center gap-2 border-l border-border pl-3"
              onClick={() => {
                setProfileOpen((open) => !open)
                setNotificationsOpen(false)
              }}
              aria-label="Abrir menu da conta"
            >
              <span className="grid size-8 place-items-center rounded-md bg-primary text-xs font-bold text-white">
                {initials}
              </span>
              <span className="hidden text-left md:block">
                <span className="block max-w-48 truncate text-xs font-bold">{session?.user.email}</span>
                <span className="block text-[10px] text-muted-foreground">{roleName(role!)}</span>
              </span>
              <ChevronDown size={14} />
            </button>
            {profileOpen && (
              <div className="absolute right-0 top-11 z-40 w-72 border border-border bg-card p-4 shadow-xl">
                <div className="flex gap-3 border-b border-border pb-4">
                  <span className="grid size-12 place-items-center rounded-md bg-primary/10 text-primary">
                    <UserRound size={22} />
                  </span>
                  <div className="min-w-0">
                    <p className="truncate text-sm font-bold">{session?.user.email}</p>
                    <p className="mt-1 text-xs text-muted-foreground">{roleName(role!)}</p>
                  </div>
                </div>
                <Link
                  to={role === 'EMPLOYEE' ? '/meu/perfil' : role === 'ADMIN' ? '/admin/perfil' : '/equipe/perfil'}
                  className="mt-3 flex items-center gap-2 py-2 text-sm font-semibold hover:text-primary"
                  onClick={() => setProfileOpen(false)}
                >
                  <UserRound size={16} /> Meu perfil
                </Link>
                <button
                  className="flex w-full items-center gap-2 py-2 text-left text-sm font-semibold text-destructive"
                  onClick={handleSignOut}
                >
                  <LogOut size={16} /> Sair
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className={`pb-20 ${desktopOffset}`}>
        <div className="mx-auto max-w-[1440px] p-4 sm:p-6 lg:p-8">
          <Outlet />
        </div>
      </main>

      <nav className="fixed inset-x-0 bottom-0 z-20 flex h-16 justify-around border-t border-border bg-card lg:hidden">
        {items.slice(0, 5).map((item) => {
          const Icon = item.icon
          const active = location.pathname.startsWith(item.to)
          return (
            <NavLink
              key={item.to}
              to={item.to}
              className={`flex min-w-0 flex-1 flex-col items-center justify-center gap-1 px-1 text-[10px] font-semibold ${
                active ? 'text-primary' : 'text-muted-foreground'
              }`}
            >
              <Icon size={18} />
              <span className="max-w-full truncate">{item.label}</span>
            </NavLink>
          )
        })}
      </nav>
    </div>
  )
}

function roleName(role: Role): string {
  if (role === 'ADMIN') return 'Administrador'
  if (role === 'MANAGER') return 'Gestor'
  if (role === 'SUPERVISOR') return 'Supervisor'
  return 'Colaborador'
}

export const shellIcons = {
  Activity,
  GraduationCap,
}
