import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js'

/**
 * Prints the usual summary and keeps the full one as JSON in results/ (git-ignored), named after the script and
 * the moment of the run, so every figure in the test report can be traced back to its raw data.
 */
export function summaryFor(name) {
  return (data) => {
    const stamp = new Date().toISOString().replace(/[:.]/g, '-')
    return {
      stdout: textSummary(data, { indent: ' ', enableColors: false }),
      [`/scripts/results/${name}-${stamp}.json`]: JSON.stringify(data, null, 2),
    }
  }
}
