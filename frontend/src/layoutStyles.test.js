import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import assert from 'node:assert/strict'

const css = readFileSync(new URL('./styles.css', import.meta.url), 'utf8')

test('result panes keep independent scroll regions while the page can still scroll', () => {
  assert.match(css, /\.split-workspace\s*\{[^}]*height:\s*calc\(100vh -/s)
  assert.match(css, /\.source-pane,[\s\S]*?\.ocr-pane\s*\{[^}]*overflow:\s*hidden/s)
  assert.match(css, /\.document-canvas,[\s\S]*?\.json-panel\s*\{[^}]*overflow-y:\s*auto/s)
  assert.doesNotMatch(css, /body\s*\{[^}]*overflow:\s*hidden/s)
})
