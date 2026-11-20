// Load test (docs/calidad/plan-de-pruebas.md §6): the expected busy hour. People log in while others work on their
// inbox, both at the same time, with the thresholds of the plan.
//   - login: 20 virtual users for 3 minutes.
//   - inbox: 50 virtual users for 5 minutes; opening the inbox loads the list and the counters, then the person
//     filters or searches one or two more times.
import { sleep } from 'k6'
import exec from 'k6/execution'
import { think } from './lib/config.js'
import { listRequests, requestCounts } from './lib/inbox.js'
import { browsingSessions, login, loginAccounts } from './lib/session.js'
import { summaryFor } from './lib/summary.js'

const LOGIN_VUS = Number(__ENV.LOGIN_VUS || 20)
const INBOX_VUS = Number(__ENV.INBOX_VUS || 50)

export const options = {
  setupTimeout: '2m',
  scenarios: {
    login: { executor: 'constant-vus', exec: 'loginFlow', vus: LOGIN_VUS, duration: __ENV.LOGIN_DURATION || '3m' },
    inbox: { executor: 'constant-vus', exec: 'inboxFlow', vus: INBOX_VUS, duration: __ENV.INBOX_DURATION || '5m' },
  },
  thresholds: {
    'http_req_duration{scenario:login}': ['p(95)<800'],
    'http_req_failed{scenario:login}': ['rate<0.01'],
    'http_req_duration{scenario:inbox,endpoint:list}': ['p(95)<500', 'p(99)<1000'],
    'http_req_duration{scenario:inbox,endpoint:counts}': ['p(95)<400'],
    'http_req_failed{scenario:inbox}': ['rate<0.01'],
    'checks{scenario:login}': ['rate>0.99'],
    'checks{scenario:inbox}': ['rate>0.99'],
  },
  summaryTrendStats: ['avg', 'min', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

export function setup() {
  // The login accounts first: creating them logs the administrator in, and the browsing sessions log it in again.
  const loginPool = loginAccounts(LOGIN_VUS)
  return { loginPool, sessions: browsingSessions() }
}

export function loginFlow(data) {
  const account = data.loginPool[(exec.vu.idInTest - 1) % data.loginPool.length]
  login(account.email, account.password)
  sleep(think(1, 3))
}

export function inboxFlow(data) {
  const session = data.sessions[(exec.vu.idInTest - 1) % data.sessions.length]
  listRequests(session.token)
  requestCounts(session.token)
  sleep(think(1, 3))
  const more = Math.floor(Math.random() * 3)
  for (let i = 0; i < more; i++) {
    listRequests(session.token)
    sleep(think(1, 3))
  }
}

export const handleSummary = summaryFor('load')
