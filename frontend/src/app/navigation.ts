import { homePathFor } from '../features/auth/context'
import type { Role } from '../shared/domain'
import { LIST_TITLES } from '../shared/domain'
import type { IconName } from '../shared/ui'

export type NavItem = {
  to: string
  label: string
  icon: IconName
  /** Encabezado de sección que se muestra encima del ítem (los siguientes continúan en la misma sección). */
  section?: string
  /** Qué contador mostrar: solicitudes abiertas o notificaciones sin leer. */
  badge?: 'open' | 'unread'
}

export type BottomNavItem = { to: string; label: string; icon: IconName; badge?: 'open' | 'unread' }

/** Menú lateral por rol (diseño §Roles). */
export const NAV: Record<Role, NavItem[]> = {
  solicitante: [
    { to: '/inicio', label: 'Inicio', icon: 'home', section: 'Principal' },
    { to: '/solicitudes', label: LIST_TITLES.solicitante, icon: 'file', badge: 'open' },
    { to: '/solicitudes/nueva', label: 'Nueva solicitud', icon: 'plus', section: 'Gestión' },
    { to: '/notificaciones', label: 'Notificaciones', icon: 'bell', section: 'Cuenta', badge: 'unread' },
  ],
  tecnico: [
    { to: '/solicitudes', label: LIST_TITLES.tecnico, icon: 'file', section: 'Principal', badge: 'open' },
    { to: '/calendario', label: 'Calendario de vencimientos', icon: 'calendar' },
    { to: '/notificaciones', label: 'Notificaciones', icon: 'bell', section: 'Cuenta', badge: 'unread' },
  ],
  supervisor: [
    { to: '/dashboard', label: 'Dashboard', icon: 'chart', section: 'Principal' },
    { to: '/solicitudes', label: LIST_TITLES.supervisor, icon: 'file', section: 'Gestión', badge: 'open' },
    { to: '/equipo', label: 'Equipo', icon: 'users' },
    { to: '/reportes', label: 'Reportes', icon: 'download' },
    { to: '/notificaciones', label: 'Notificaciones', icon: 'bell', section: 'Cuenta', badge: 'unread' },
  ],
  administrador: [
    { to: '/dashboard', label: 'Dashboard global', icon: 'chart', section: 'Principal' },
    { to: '/solicitudes', label: 'Solicitudes', icon: 'file', badge: 'open' },
    { to: '/admin/usuarios', label: 'Usuarios', icon: 'users', section: 'Administración' },
    { to: '/admin/catalogos', label: 'Catálogos', icon: 'folder' },
    { to: '/admin/triage', label: 'Reglas de triage', icon: 'zap' },
    { to: '/admin/auditoria', label: 'Auditoría', icon: 'shield' },
    { to: '/admin/configuracion', label: 'Configuración', icon: 'cog' },
    { to: '/notificaciones', label: 'Notificaciones', icon: 'bell', section: 'Cuenta', badge: 'unread' },
  ],
}

/** Navegación inferior en móvil: 4 accesos + "Menú" que abre el cajón con el resto. */
export const BOTTOM_NAV: Record<Role, BottomNavItem[]> = {
  solicitante: [
    { to: '/inicio', label: 'Inicio', icon: 'home' },
    { to: '/solicitudes', label: 'Solicitudes', icon: 'file', badge: 'open' },
    { to: '/solicitudes/nueva', label: 'Nueva', icon: 'plus' },
    { to: '/notificaciones', label: 'Avisos', icon: 'bell', badge: 'unread' },
  ],
  tecnico: [
    { to: '/solicitudes', label: 'Bandeja', icon: 'file', badge: 'open' },
    { to: '/calendario', label: 'Calendario', icon: 'calendar' },
    { to: '/notificaciones', label: 'Avisos', icon: 'bell', badge: 'unread' },
  ],
  supervisor: [
    { to: '/dashboard', label: 'Dashboard', icon: 'chart' },
    { to: '/solicitudes', label: 'Bandeja', icon: 'file', badge: 'open' },
    { to: '/equipo', label: 'Equipo', icon: 'users' },
    { to: '/notificaciones', label: 'Avisos', icon: 'bell', badge: 'unread' },
  ],
  administrador: [
    { to: '/dashboard', label: 'Dashboard', icon: 'chart' },
    { to: '/admin/usuarios', label: 'Usuarios', icon: 'users' },
    { to: '/admin/catalogos', label: 'Catálogos', icon: 'folder' },
    { to: '/notificaciones', label: 'Avisos', icon: 'bell', badge: 'unread' },
  ],
}

/** Un ítem está activo en su ruta exacta; "solicitudes" también en el detalle (no en "nueva"). */
export function isActivePath(itemTo: string, pathname: string): boolean {
  if (pathname === itemTo) return true
  return itemTo === '/solicitudes' && pathname.startsWith('/solicitudes/') && pathname !== '/solicitudes/nueva'
}

/** Un paso de la ruta; sin `to` es la página actual (no es enlace). */
export type Crumb = { label: string; to?: string }

const PAGE_LABELS: Record<string, string> = {
  '/notificaciones': 'Notificaciones',
  '/calendario': 'Calendario',
  '/dashboard': 'Dashboard',
  '/equipo': 'Equipo',
  '/reportes': 'Reportes',
  '/admin/usuarios': 'Usuarios',
  '/admin/catalogos': 'Catálogos',
  '/admin/triage': 'Reglas de triage',
  '/admin/auditoria': 'Auditoría',
  '/admin/configuracion': 'Configuración',
}

/** Ruta de navegación de la barra superior; "Inicio" lleva a la pantalla de inicio del rol. */
export function breadcrumbsFor(pathname: string, role: Role): Crumb[] {
  const home = homePathFor(role)
  if (pathname === home || pathname === '/inicio') return [{ label: 'Inicio' }]
  const crumbs: Crumb[] = [{ label: 'Inicio', to: home }]
  const list: Crumb = { label: LIST_TITLES[role], to: '/solicitudes' }
  if (pathname === '/solicitudes') return [...crumbs, { label: LIST_TITLES[role] }]
  if (pathname === '/solicitudes/nueva') return [...crumbs, list, { label: 'Nueva solicitud' }]
  if (pathname.startsWith('/solicitudes/')) return [...crumbs, list, { label: pathname.split('/')[2] }]
  if (PAGE_LABELS[pathname]) return [...crumbs, { label: PAGE_LABELS[pathname] }]
  return crumbs
}
