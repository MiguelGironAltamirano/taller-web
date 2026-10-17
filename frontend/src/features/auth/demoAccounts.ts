/**
 * Cuentas de la API simulada para recorrer los cuatro roles sin backend.
 * Solo se muestran con `VITE_USE_MOCKS=true`: contra el backend real no existen estas cuentas.
 */
export const SHOW_DEMO_ACCOUNTS = import.meta.env.VITE_USE_MOCKS === 'true'

const PASSWORD = 'Demo12345!'

export const DEMO_ACCOUNTS = [
  { name: 'Lucía Quispe Huamán', initials: 'LQ', role: 'Solicitante', email: 'lquispe@unmsm.edu.pe', password: PASSWORD },
  { name: 'Carlos Mendoza Rojas', initials: 'CM', role: 'Técnico · Mantenimiento', email: 'cmendoza@unmsm.edu.pe', password: PASSWORD },
  { name: 'Ana Villanueva Ramos', initials: 'AV', role: 'Supervisora · Mantenimiento', email: 'avillanueva@unmsm.edu.pe', password: PASSWORD },
  { name: 'Roberto Salazar Chávez', initials: 'RS', role: 'Administrador', email: 'rsalazar@unmsm.edu.pe', password: PASSWORD },
] as const
