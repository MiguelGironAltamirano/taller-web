// Spike test of the inbox: a sudden jump from 10 to 150 virtual users in 10 seconds (for example, when a class ends
// and many students report the same problem), held one minute, then back to 10. It checks that a burst does not
// knock the system over and that it serves normally again right after.
import { sleep } from 'k6'
import exec from 'k6/execution'
import { think } from './lib/config.js'
import { listRequests, requestCounts } from './lib/inbox.js'
import { browsingSessions } from './lib/session.js'
import { summaryFor } from './lib/summary.js'

const SECOND = 1000
const BANDS = [
  [60 * SECOND, 'baseline-10'],
  [130 * SECOND, 'spike-150'],
  [Infinity, 'after-10'],
]

export const options = {
  setupTimeout: '2m',
  scenarios: {
    spike: {
      executor: 'ramping-vus',
      exec: 'inboxFlow',
      startVUs: 10,
      stages: [
        { duration: '1m', target: 10 },
        { duration: '10s', target: 150 },
        { duration: '1m', target: 150 },
        { duration: '10s', target: 10 },
        { duration: '1m30s', target: 10 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    'http_req_failed{band:spike-150}': ['rate<0.05'],
    'http_req_duration{endpoint:list,band:baseline-10}': ['p(95)<500'],
    'http_req_duration{endpoint:list,band:spike-150}': ['p(95)<2000'],
    'http_req_duration{endpoint:list,band:after-10}': ['p(95)<500'],
    'http_req_failed{band:after-10}': ['rate<0.01'],
  },
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
  sleep(think(0.5, 1.5))
}

export const handleSummary = summaryFor('spike')
