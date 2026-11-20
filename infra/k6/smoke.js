// Smoke: one virtual user walks the endpoints the load scenarios use and checks every answer. It runs first so a
// broken stack or a changed contract fails in seconds instead of polluting a 10-minute run.
import http from 'k6/http'
import { check, group, sleep } from 'k6'
import { API } from './lib/config.js'
import { listRequests, requestCounts } from './lib/inbox.js'
import { authHeaders, browsingSessions, loginAccounts, login } from './lib/session.js'
import { summaryFor } from './lib/summary.js'

export const options = {
  vus: 1,
  iterations: 3,
  thresholds: {
    checks: ['rate==1'],
    http_req_failed: ['rate==0'],
  },
}

export function setup() {
  return { loginPool: loginAccounts(1), sessions: browsingSessions() }
}

export default function (data) {
  group('login', () => {
    const account = data.loginPool[0]
    check(login(account.email, account.password), { 'fresh account can log in': (t) => !!t })
  })
  for (const session of data.sessions) {
    group(`inbox as ${session.role}`, () => {
      const page = listRequests(session.token)
      requestCounts(session.token)
      const first = page.status === 200 ? page.json('items.0.code') : null
      if (first) {
        const detail = http.get(`${API}/requests/${first}`, {
          ...authHeaders(session.token),
          tags: { endpoint: 'detail', name: 'GET /requests/{code}' },
        })
        check(detail, { 'detail 200': (r) => r.status === 200 })
      }
    })
  }
  sleep(1)
}

export const handleSummary = summaryFor('smoke')
