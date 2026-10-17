import { adminHandlers } from './admin'
import { authHandlers, catalogHandlers } from './auth'
import { dashboardHandlers, notificationHandlers, triageHandlers } from './insights'
import { requestHandlers } from './requests'

export { resetMockData } from '../db'

/** API simulada completa sobre `/api/v1` (contrato del MVP + extensiones del diseño). */
export const handlers = [
  ...authHandlers,
  ...catalogHandlers,
  ...requestHandlers,
  ...notificationHandlers,
  ...triageHandlers,
  ...dashboardHandlers,
  ...adminHandlers,
]
