/**
 * Cuentas de la API simulada para recorrer los cuatro roles sin backend.
 * Solo se muestran con `VITE_SHOW_DEMO_ACCOUNTS=true` (desarrollo y staging, nunca producción).
 */
export const SHOW_DEMO_ACCOUNTS = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS === 'true'

const PASSWORD = 'Demo12345!'

export const DEMO_ACCOUNTS = [
  { name: 'Lucía Quispe Huamán', initials: 'LQ', role: 'Solicitante', email: 'lquispe@unmsm.edu.pe', password: PASSWORD },
  { name: 'Carlos Mendoza Rojas', initials: 'CM', role: 'Técnico · Mantenimiento', email: 'cmendoza@unmsm.edu.pe', password: PASSWORD },
  { name: 'Ana Villanueva Ramos', initials: 'AV', role: 'Supervisora · Mantenimiento', email: 'avillanueva@unmsm.edu.pe', password: PASSWORD },
  { name: 'Roberto Salazar Chávez', initials: 'RS', role: 'Administrador', email: 'rsalazar@unmsm.edu.pe', password: PASSWORD },
] as const
