import http from 'k6/http'
import { check, fail } from 'k6'
import { API, BROWSING_ACCOUNTS, DEMO_PASSWORD, JSON_HEADERS } from './config.js'

/** Logs in and returns the access token, or fails the test: without a session nothing else can be measured. */
export function login(email, password, tags = {}) {
  const res = http.post(`${API}/auth/login`, JSON.stringify({ email, password }), {
    headers: JSON_HEADERS,
    tags: { endpoint: 'login', ...tags },
  })
  const ok = check(res, { 'login 200': (r) => r.status === 200, 'login returns a token': (r) => !!r.json('accessToken') })
  return ok ? res.json('accessToken') : null
}

export function authHeaders(token) {
  return { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } }
}

/** One token per browsing account, logged in once (see BROWSING_ACCOUNTS). */
export function browsingSessions() {
  return BROWSING_ACCOUNTS.map((account) => {
    const token = login(account.email, DEMO_PASSWORD, { phase: 'setup' })
    if (!token) fail(`setup: could not log in ${account.email}; is the stack up with db/seed-dev?`)
    return { ...account, token }
  })
}

/**
 * Accounts only the login scenario uses, created by the administrator for this run. They are not the browsing
 * accounts, so logging them in again and again never closes a session the inbox scenario is using; and one per
 * virtual user keeps each account far below the per-account login limit (5 attempts in flight).
 */
export function loginAccounts(count) {
  const admin = login('admin@unmsm.edu.pe', DEMO_PASSWORD, { phase: 'setup' })
  if (!admin) fail('setup: could not log in the administrator')
  const run = `${Date.now().toString(36)}${Math.floor(Math.random() * 1e4).toString(36)}`
  const accounts = []
  for (let i = 0; i < count; i++) {
    const email = `k6.${run}.${i}@unmsm.edu.pe`
    const res = http.post(
      `${API}/admin/users`,
      JSON.stringify({ fullName: `Carga k6 ${i}`, email, role: 'SOLICITANTE', areaId: null }),
      { ...authHeaders(admin), tags: { endpoint: 'admin-users', phase: 'setup' } },
    )
    if (res.status !== 201) fail(`setup: could not create ${email}: ${res.status} ${res.body}`)
    accounts.push({ email, password: res.json('temporaryPassword') })
  }
  return accounts
}
