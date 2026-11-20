// Stress test of the inbox (docs/calidad/plan-de-pruebas.md §6): ramp from 0 to 200 virtual users in 10 minutes
// to find where the list stops meeting its targets, then drop to 20 to check that the system recovers.
//
// Every request is tagged with the load band it ran in. The per-band thresholds are the reading of the test: the
// first band that fails (p95 > 1 s or more than 5 % errors) is the breaking point. Only the "recovery" band must
// pass for the system to be considered healthy; failing bands under 200 users are a finding, not a broken script.
import { sleep } from 'k6'
import exec from 'k6/execution'
import { think } from './lib/config.js'
import { listRequests, requestCounts } from './lib/inbox.js'
import { browsingSessions } from './lib/session.js'
import { summaryFor } from './lib/summary.js'

const MINUTE = 60_000
/** [end of the band in ms since the start, label]: ramp in four 2-minute steps, hold, then recover. */
const BANDS = [
  [2 * MINUTE, 'ramp-0-50'],
  [4 * MINUTE, 'ramp-50-100'],
  [6 * MINUTE, 'ramp-100-150'],
  [8 * MINUTE, 'ramp-150-200'],
  [10 * MINUTE, 'hold-200'],
  [11 * MINUTE, 'drop-200-20'],
  [Infinity, 'recovery-20'],
]

export const options = {
  setupTimeout: '2m',
  scenarios: {
    stress: {
      executor: 'ramping-vus',
      exec: 'inboxFlow',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 50 },
        { duration: '2m', target: 100 },
        { duration: '2m', target: 150 },
        { duration: '2m', target: 200 },
        { duration: '2m', target: 200 },
        { duration: '1m', target: 20 },
        { duration: '2m', target: 20 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: Object.fromEntries(
    BANDS.flatMap(([, band]) => [
      [`http_req_duration{endpoint:list,band:${band}}`, band === 'recovery-20' ? ['p(95)<500'] : ['p(95)<1000']],
      [`http_req_failed{band:${band}}`, band === 'recovery-20' ? ['rate<0.01'] : ['rate<0.05']],
    ]),
  ),
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
}

function currentBand() {
  const elapsed = exec.instance.currentTestRunDuration
  return BANDS.find(([end]) => elapsed < end)[1]
}

export function setup() {
  return { sessions: browsingSessions() }
}

export function inboxFlow(data) {
  const session = data.sessions[(exec.vu.idInTest - 1) % data.sessions.length]
  const tags = { band: currentBand() }
  listRequests(session.token, tags)
  requestCounts(session.token, tags)
  // Busier users than in the load test: less reading between clicks pushes more requests per user.
  sleep(think(0.5, 1.5))
  listRequests(session.token, { band: currentBand() })
  sleep(think(0.5, 1.5))
}

export const handleSummary = summaryFor('stress')
