<script setup>
import { computed, reactive, ref, watch } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE || ''
const file = ref(null)
const fileName = ref('')
const loading = ref(false)
const error = ref('')
const response = ref(null)
const activePage = ref(1)
const viewMode = ref('upload')
const resultTab = ref('summary') // 'summary' | 'pages'
const progress = ref(0)
const progressStage = ref('')
let progressTimer = null

const metadata = reactive({
  applicantAge: '',
  sponsorType: 'institution',
  hasAcceptanceLetter: true,
  hasApplicantTravelDocumentCopy: true,
  hasApplicantFinancialProof: true,
  hasGuardianConsent: true,
  hasAccommodationProof: true,
  hasSponsorForm: true,
  hasSponsorIdentityCopy: true,
  hasSponsorFinancialProof: true,
  mainlandApplicationViaSchool: true,
  isMainlandChineseResident: false,
  hasDependants: false,
  dependantDocumentsComplete: true,
  documentsNeedTranslation: false
})

const declaredAttachments = [
  '取录信',
  '旅行证件副本',
  '申请人经济证明',
  '未成年人监护同意书',
  '未成年人住宿证明',
  'ID995B 保证人表格',
  '保证人身份证明',
  '保证人经济证明',
  '内地学生经院校递交申请',
  '受养人乙部材料完整'
]

const allPages = computed(() => {
  const pages = response.value?.pageSnapshots || []
  return pages.map((p) => p.page)
})

const currentSnapshot = computed(() => {
  const pages = response.value?.pageSnapshots || []
  return pages.find((p) => p.page === activePage.value) || pages[0] || null
})

const pageAnnotations = computed(() => {
  return (response.value?.annotations || []).filter((a) => a.page === activePage.value)
})

const pagesWithIssues = computed(() => {
  const pages = new Set()
  ;(response.value?.findings || []).forEach((f) => pages.add(f.page || 1))
  return Array.from(pages).sort((a, b) => a - b)
})

const currentPageFindings = computed(() => {
  return (response.value?.findings || []).filter((f) => (f.page || 1) === activePage.value)
})

const currentPageBlocking = computed(() =>
  currentPageFindings.value.filter((f) => f.severity === 'BLOCKING')
)
const currentPageManual = computed(() =>
  currentPageFindings.value.filter((f) => f.severity === 'MANUAL_REVIEW')
)

const overallVerdictClass = computed(() => {
  if (!response.value) return ''
  return response.value.passed ? 'pass' : 'fail'
})

const totalBlockingCount = computed(() =>
  (response.value?.findings || []).filter((f) => f.severity === 'BLOCKING').length
)
const totalManualCount = computed(() =>
  (response.value?.findings || []).filter((f) => f.severity === 'MANUAL_REVIEW').length
)
const totalPages = computed(() => response.value?.pageSnapshots?.length || 0)

// ID995A 页面分类：
//   1-4 页：申请人甲部（必填）
//   5-6 页：受养人乙部（仅当有随行受养人时填写）
//   7 页：表格使用说明（不需填写）
const PAGE_INSTRUCTION = [7]
const PAGE_CONDITIONAL_DEPENDANT = [5, 6]

function pageHasFindings(page) {
  return (response.value?.findings || []).some((f) => (f.page || 1) === page)
}

function pageStatus(page) {
  if (pageHasFindings(page)) return 'fail'
  if (PAGE_INSTRUCTION.includes(page)) return 'instruction'
  if (PAGE_CONDITIONAL_DEPENDANT.includes(page) && !metadata.hasDependants) {
    return 'not-applicable'
  }
  return 'pass'
}

function pageStatusLabel(page) {
  const status = pageStatus(page)
  if (status === 'pass') return '通过'
  if (status === 'instruction') return '说明页'
  if (status === 'not-applicable') return '不适用'
  return ''
}

function pageStatusDescription(page) {
  const status = pageStatus(page)
  if (status === 'instruction') return '第 7 页为表格使用说明，不需要填写或审核。'
  if (status === 'not-applicable') return `第 ${page} 页为受养人乙部，仅在有随行受养人时才需填写。本次申请未声明随行受养人，故不适用。`
  if (status === 'pass') return `第 ${page} 页未发现不通过项目。`
  return ''
}

watch(response, (value) => {
  if (!value) {
    viewMode.value = 'upload'
    return
  }
  viewMode.value = 'result'
  const firstIssue = value.findings?.find((f) => f.page)?.page
  activePage.value = firstIssue || value.pageSnapshots?.[0]?.page || 1
})

function onFileChange(event) {
  const selected = event.target.files?.[0]
  setFile(selected)
}
function onDrop(event) {
  const selected = event.dataTransfer.files?.[0]
  setFile(selected)
}
function setFile(selected) {
  if (!selected) return
  file.value = selected
  fileName.value = selected.name
  response.value = null
  error.value = ''
}

function startProgress() {
  progress.value = 0
  progressStage.value = '准备上传…'
  const startTime = Date.now()
  const estimatedTotal = 90000
  progressTimer = setInterval(() => {
    const elapsed = Date.now() - startTime
    let p = (elapsed / estimatedTotal) * 100
    if (p > 90) p = 90 + (p - 90) * 0.1
    if (p > 99) p = 99
    progress.value = p

    if (p < 15) progressStage.value = '上传文件并解析 PDF…'
    else if (p < 50) progressStage.value = 'OCR 识别申请表内容（多页）…'
    else if (p < 80) progressStage.value = '执行规则引擎与混合检索…'
    else progressStage.value = '生成审核结论…'
  }, 100)
}

function stopProgress(success) {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  progress.value = success ? 100 : 0
  progressStage.value = success ? '完成' : ''
}

async function submitReview() {
  if (!file.value) {
    error.value = '请选择已填写申请表文件。'
    return
  }
  loading.value = true
  error.value = ''
  response.value = null
  startProgress()
  try {
    const body = new FormData()
    body.append('file', file.value)
    body.append('metadata', JSON.stringify(metadata))
    const result = await fetch(`${apiBase}/api/review`, { method: 'POST', body })
    if (!result.ok) throw new Error(`HTTP ${result.status}`)
    response.value = await result.json()
    stopProgress(true)
  } catch {
    error.value = '审核未能完成，请确认服务已启动后重试。'
    stopProgress(false)
  } finally {
    loading.value = false
  }
}

function goToPage(page) {
  activePage.value = page
}
function resetUpload() {
  file.value = null
  fileName.value = ''
  response.value = null
  error.value = ''
  viewMode.value = 'upload'
  activePage.value = 1
}

function annotationStyle(annotation) {
  const x = annotation.x * 1000
  const y = annotation.y * 1414
  const w = annotation.width * 1000
  const h = annotation.height * 1414
  return {
    cx: x + w / 2,
    cy: y + h / 2,
    rx: Math.max(w / 2, 18),
    ry: Math.max(h / 2, 18),
    lineX1: x + w,
    lineY1: y + h / 2,
    lineX2: Math.min(940, x + w + 140),
    lineY2: Math.max(40, y + h / 2 - 40),
    labelX: Math.min(800, x + w + 144),
    labelY: Math.max(30, y + h / 2 - 58)
  }
}
function severityLabel(severity) {
  if (severity === 'BLOCKING') return '不通过'
  if (severity === 'MANUAL_REVIEW') return '复核'
  return '提示'
}
function severityClass(severity) {
  if (severity === 'BLOCKING') return 'blocker'
  if (severity === 'MANUAL_REVIEW') return 'manual'
  return 'info'
}
</script>

<template>
  <main class="app-shell">
    <!-- ===== 顶部导航栏（结果模式） ===== -->
    <header v-if="viewMode === 'result'" class="topbar">
      <div class="brand">
        <h1>ID995A 预审结果</h1>
        <span class="verdict-badge" :class="overallVerdictClass">
          {{ response?.verdict }}
        </span>
      </div>

      <nav class="tab-nav" aria-label="结果视图切换">
        <button
          type="button"
          :class="{ active: resultTab === 'summary' }"
          @click="resultTab = 'summary'"
        >
          整体结论
        </button>
        <button
          type="button"
          :class="{ active: resultTab === 'pages' }"
          @click="resultTab = 'pages'"
        >
          逐页浏览
        </button>
      </nav>

      <button type="button" class="btn-ghost" @click="resetUpload">重新上传</button>
    </header>

    <!-- ===== 上传模式 ===== -->
    <section v-if="viewMode === 'upload'" class="upload-view">
      <div class="upload-card">
        <div class="upload-header">
          <p class="eyebrow">香港入境处业务演示</p>
          <h1>ID995A 来港就读申请表预审</h1>
        </div>

        <label class="dropzone" @drop.prevent="onDrop" @dragover.prevent>
          <input type="file" accept="application/pdf" @change="onFileChange" />
          <span class="drop-icon">
            <svg viewBox="0 0 24 24"><path d="M7 18h10a4 4 0 0 0 .7-7.94A6 6 0 0 0 6.5 8.1 4.5 4.5 0 0 0 7 18Z"/><path d="M12 12v6M9.5 14.5 12 12l2.5 2.5"/></svg>
          </span>
          <strong>{{ fileName || '选择或拖入已填写申请表 (PDF)' }}</strong>
        </label>

        <div class="declared-info">
          <div class="declared-header">
            <svg viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5"/></svg>
            <h3>已声明的补充材料</h3>
            <span class="declared-hint">本 Demo 假定以下材料已齐备</span>
          </div>
          <ul class="declared-list">
            <li v-for="item in declaredAttachments" :key="item">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M20 6 9 17l-5-5"/></svg>
              {{ item }}
            </li>
          </ul>
          <p class="auto-extract-note">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2v6m0 8v6M2 12h6m8 0h6"/></svg>
            申请人年龄 / 保证人类型 将从 PDF 自动识别
          </p>
        </div>

        <div v-if="loading" class="progress-block">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: progress + '%' }"></div>
          </div>
          <div class="progress-meta">
            <span class="progress-stage">{{ progressStage }}</span>
            <strong class="progress-percent">{{ progress.toFixed(1) }}%</strong>
          </div>
        </div>
        <button v-else class="btn-primary" type="button" :disabled="!file" @click="submitReview">
          开始审核
        </button>
        <p v-if="error" class="error-text">{{ error }}</p>
      </div>
    </section>

    <!-- ===== 结果浏览模式 ===== -->
    <section v-if="viewMode === 'result'" class="result-view">
      <!-- Tab 1: 整体结论 -->
      <div v-if="resultTab === 'summary'" class="summary-view">
        <div class="summary-card">
          <div class="verdict-section" :class="overallVerdictClass">
            <h2>{{ response?.verdict }}</h2>
            <p v-if="response?.passed">申请表预审通过，未发现不通过项目。</p>
            <p v-else>申请表预审未通过，请根据以下问题逐页核对并修正。</p>
          </div>

          <div class="stats-grid">
            <div class="stat-item">
              <strong>{{ totalPages }}</strong>
              <span>总页数</span>
            </div>
            <div class="stat-item blocker">
              <strong>{{ totalBlockingCount }}</strong>
              <span>不通过项</span>
            </div>
            <div class="stat-item manual">
              <strong>{{ totalManualCount }}</strong>
              <span>需复核项</span>
            </div>
          </div>

          <div v-if="totalBlockingCount > 0 || totalManualCount > 0" class="issue-pages">
            <h3>问题分布</h3>
            <div class="page-issue-list">
              <button
                v-for="page in allPages"
                :key="page"
                type="button"
                :class="{
                  issue: pagesWithIssues.includes(page),
                  'page-pass': pageStatus(page) === 'pass',
                  'page-na': pageStatus(page) === 'not-applicable' || pageStatus(page) === 'instruction'
                }"
                @click="resultTab = 'pages'; goToPage(page)"
              >
                第{{ page }}页
                <span v-if="pagesWithIssues.includes(page)" class="issue-count">
                  {{ (response?.findings || []).filter(f => (f.page || 1) === page).length }} 项
                </span>
                <span v-else-if="pageStatus(page) === 'instruction'" class="na-tag">说明页</span>
                <span v-else-if="pageStatus(page) === 'not-applicable'" class="na-tag">不适用</span>
                <span v-else class="pass-tag">通过</span>
              </button>
            </div>
          </div>

          <button type="button" class="btn-primary" @click="resultTab = 'pages'">
            查看逐页审核结果 →
          </button>
        </div>
      </div>

      <!-- Tab 2: 逐页浏览 -->
      <div v-if="resultTab === 'pages'" class="pages-view">
        <!-- 页码导航 -->
        <nav class="page-nav-bar" aria-label="页面切换">
          <button
            v-for="page in allPages"
            :key="page"
            type="button"
            :class="{
              active: page === activePage,
              issue: pagesWithIssues.includes(page)
            }"
            @click="goToPage(page)"
          >
            第{{ page }}页
            <span v-if="pagesWithIssues.includes(page)" class="dot" />
          </button>
        </nav>

        <!-- 左右分屏 -->
        <div class="split-layout">
          <!-- 左侧：快照 + 标注 -->
          <div class="snapshot-panel">
            <div v-if="currentSnapshot" class="snapshot-wrap">
              <img :src="currentSnapshot.dataUrl" alt="ID995A 页面快照" />
              <svg class="annotation-layer" viewBox="0 0 1000 1414" preserveAspectRatio="none">
                <g v-for="annotation in pageAnnotations" :key="annotation.id" class="annotation-mark">
                  <ellipse
                    :cx="annotationStyle(annotation).cx"
                    :cy="annotationStyle(annotation).cy"
                    :rx="annotationStyle(annotation).rx"
                    :ry="annotationStyle(annotation).ry"
                  />
                  <line
                    :x1="annotationStyle(annotation).lineX1"
                    :y1="annotationStyle(annotation).lineY1"
                    :x2="annotationStyle(annotation).lineX2"
                    :y2="annotationStyle(annotation).lineY2"
                  />
                  <foreignObject
                    :x="annotationStyle(annotation).labelX"
                    :y="annotationStyle(annotation).labelY"
                    width="160"
                    height="60"
                  >
                    <div class="annotation-label">
                      <strong>{{ annotation.label }}</strong>
                      <span>{{ severityLabel(annotation.severity) }}</span>
                    </div>
                  </foreignObject>
                </g>
              </svg>
            </div>
            <div v-else class="snapshot-empty">无页面快照</div>
          </div>

          <!-- 右侧：原因列表 -->
          <div class="findings-panel">
            <div class="findings-header">
              <h2>第 {{ activePage }} 页</h2>
              <span v-if="currentPageBlocking.length" class="count-badge blocker">
                {{ currentPageBlocking.length }} 项不通过
              </span>
              <span v-else-if="currentPageManual.length" class="count-badge manual">
                {{ currentPageManual.length }} 项需复核
              </span>
              <span v-else-if="pageStatus(activePage) === 'instruction'" class="count-badge info">
                说明页
              </span>
              <span v-else-if="pageStatus(activePage) === 'not-applicable'" class="count-badge info">
                不适用
              </span>
              <span v-else class="count-badge pass">通过</span>
            </div>

            <div v-if="currentPageFindings.length === 0" class="findings-empty" :class="pageStatus(activePage)">
              <svg v-if="pageStatus(activePage) === 'pass'" viewBox="0 0 24 24"><path d="M20 6 9 17l-5-5"/></svg>
              <svg v-else viewBox="0 0 24 24"><circle cx="12" cy="12" r="10"/><path d="M12 8v4M12 16h.01"/></svg>
              <p>{{ pageStatusDescription(activePage) }}</p>
            </div>

            <div v-else class="findings-list">
              <article
                v-for="finding in currentPageFindings"
                :key="finding.ruleId"
                class="finding-card"
                :class="severityClass(finding.severity)"
              >
                <div class="finding-meta">
                  <strong class="finding-id">{{ finding.ruleId }}</strong>
                  <span class="finding-severity">{{ severityLabel(finding.severity) }}</span>
                </div>
                <h3 class="finding-title">{{ finding.title }}</h3>
                <p class="finding-message">{{ finding.message }}</p>
              </article>
            </div>
          </div>
        </div>
      </div>
    </section>
  </main>
</template>

<style>
/* ===== Reset & Base ===== */
* { box-sizing: border-box; margin: 0; padding: 0; }
html, body, #app { height: 100%; }
body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f3f4f6;
  color: #1f2937;
}

/* ===== Upload View ===== */
.upload-view {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.upload-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 40px;
  width: 100%;
  max-width: 560px;
}
.upload-header { margin-bottom: 24px; }
.eyebrow { font-size: 12px; color: #6b7280; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
.upload-header h1 { font-size: 22px; font-weight: 700; color: #111827; }

.dropzone {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 32px;
  border: 2px dashed #d1d5db;
  border-radius: 12px;
  cursor: pointer;
  transition: border-color 0.2s, background 0.2s;
  margin-bottom: 20px;
}
.dropzone:hover { border-color: #3b82f6; background: #eff6ff; }
.dropzone input { display: none; }
.drop-icon { width: 48px; height: 48px; color: #9ca3af; }
.drop-icon svg { width: 100%; height: 100%; fill: none; stroke: currentColor; stroke-width: 1.5; stroke-linecap: round; stroke-linejoin: round; }
.dropzone strong { font-size: 15px; color: #374151; }

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 16px;
}
.meta-grid label, .meta-checks label { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #4b5563; }
.meta-grid input, .meta-grid select {
  padding: 8px 10px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px;
}
.meta-checks {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px 16px;
  margin-bottom: 20px;
}
.meta-checks label { flex-direction: row; align-items: center; gap: 6px; }
.meta-checks input[type="checkbox"] { width: 16px; height: 16px; }

/* Declared attachments (static, no interaction) */
.declared-info {
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  border-radius: 12px;
  padding: 16px 18px;
  margin-bottom: 20px;
}
.declared-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.declared-header svg {
  width: 18px; height: 18px;
  fill: none; stroke: #0284c7; stroke-width: 2.5;
  stroke-linecap: round; stroke-linejoin: round;
}
.declared-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #075985;
}
.declared-hint {
  margin-left: auto;
  font-size: 11px;
  color: #0369a1;
  background: #e0f2fe;
  padding: 2px 8px;
  border-radius: 12px;
}
.declared-list {
  list-style: none;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 12px;
  padding: 0;
  margin: 0;
}
.declared-list li {
  font-size: 13px;
  color: #0c4a6e;
  display: flex;
  align-items: center;
  gap: 6px;
}
.declared-list svg {
  width: 14px; height: 14px;
  fill: none; stroke: #10b981; stroke-width: 3;
  stroke-linecap: round; stroke-linejoin: round;
  flex-shrink: 0;
}
.auto-extract-note {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px dashed #bae6fd;
  font-size: 12px;
  color: #0369a1;
  display: flex;
  align-items: center;
  gap: 6px;
}
.auto-extract-note svg {
  width: 14px; height: 14px;
  fill: none; stroke: #0284c7; stroke-width: 2;
  stroke-linecap: round; stroke-linejoin: round;
  flex-shrink: 0;
}

.btn-primary {
  width: 100%; padding: 12px; border: none; border-radius: 10px;
  background: #2563eb; color: #fff; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: background 0.2s;
}
.btn-primary:hover:not(:disabled) { background: #1d4ed8; }
.btn-primary:disabled { background: #9ca3af; cursor: not-allowed; }
.error-text { color: #dc2626; font-size: 13px; margin-top: 10px; text-align: center; }

/* Progress bar (during review) */
.progress-block {
  width: 100%;
  padding: 16px 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
}
.progress-bar {
  width: 100%;
  height: 10px;
  background: #e2e8f0;
  border-radius: 999px;
  overflow: hidden;
  position: relative;
}
.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3b82f6 0%, #2563eb 100%);
  border-radius: 999px;
  transition: width 0.12s linear;
  position: relative;
  overflow: hidden;
}
.progress-fill::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(90deg,
    transparent 0%,
    rgba(255,255,255,0.4) 50%,
    transparent 100%);
  animation: shimmer 1.6s linear infinite;
}
@keyframes shimmer {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}
.progress-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
}
.progress-stage {
  font-size: 13px;
  color: #475569;
}
.progress-percent {
  font-size: 16px;
  font-weight: 700;
  color: #2563eb;
  font-variant-numeric: tabular-nums;
}

/* ===== Top Bar (Result Mode) ===== */
.topbar {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  gap: 16px;
  flex-shrink: 0;
}
.brand { display: flex; align-items: center; gap: 12px; }
.brand h1 { font-size: 16px; font-weight: 700; color: #111827; white-space: nowrap; }
.verdict-badge {
  padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;
}
.verdict-badge.pass { background: #dcfce7; color: #166534; }
.verdict-badge.fail { background: #fee2e2; color: #991b1b; }

.tab-nav { display: flex; gap: 4px; }
.tab-nav button {
  padding: 6px 16px; border: none; border-radius: 8px;
  background: transparent; font-size: 14px; cursor: pointer;
  color: #6b7280; font-weight: 500;
  transition: all 0.15s;
}
.tab-nav button:hover { color: #374151; background: #f3f4f6; }
.tab-nav button.active { color: #2563eb; background: #eff6ff; font-weight: 600; }

.btn-ghost {
  padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 8px;
  background: #fff; font-size: 13px; cursor: pointer; white-space: nowrap;
}
.btn-ghost:hover { border-color: #9ca3af; }

/* ===== Result View ===== */
.app-shell { height: 100vh; display: flex; flex-direction: column; overflow: hidden; }
.result-view {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

/* --- Summary Tab --- */
.summary-view {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  overflow-y: auto;
}
.summary-card {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.08);
  padding: 40px;
  width: 100%;
  max-width: 520px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.verdict-section { text-align: center; padding: 24px; border-radius: 12px; }
.verdict-section.pass { background: #dcfce7; }
.verdict-section.fail { background: #fee2e2; }
.verdict-section h2 { font-size: 28px; font-weight: 700; margin-bottom: 8px; }
.verdict-section.pass h2 { color: #166534; }
.verdict-section.fail h2 { color: #991b1b; }
.verdict-section p { font-size: 14px; color: #4b5563; }

.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}
.stat-item {
  text-align: center;
  padding: 16px;
  background: #f9fafb;
  border-radius: 10px;
}
.stat-item strong { display: block; font-size: 28px; font-weight: 700; color: #111827; }
.stat-item span { font-size: 12px; color: #6b7280; }
.stat-item.blocker strong { color: #dc2626; }
.stat-item.manual strong { color: #f59e0b; }

.issue-pages h3 { font-size: 14px; font-weight: 600; color: #374151; margin-bottom: 10px; }
.page-issue-list { display: flex; flex-wrap: wrap; gap: 8px; }
.page-issue-list button {
  padding: 8px 14px; border: 1px solid #e5e7eb; border-radius: 8px;
  background: #fff; font-size: 13px; cursor: pointer;
  display: flex; align-items: center; gap: 6px;
  transition: all 0.15s;
}
.page-issue-list button:hover { border-color: #3b82f6; }
.page-issue-list button.issue { border-color: #fca5a5; color: #dc2626; }
.page-issue-list button.issue:hover { background: #fef2f2; }
.page-issue-list button.page-na { color: #6b7280; }
.page-issue-list button.page-na:hover { background: #f3f4f6; }
.issue-count { font-size: 11px; font-weight: 600; background: #fee2e2; padding: 1px 6px; border-radius: 4px; }
.pass-tag { font-size: 11px; color: #16a34a; }
.na-tag { font-size: 11px; color: #6b7280; background: #f3f4f6; padding: 1px 6px; border-radius: 4px; }

/* --- Pages Tab --- */
.pages-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.page-nav-bar {
  display: flex;
  gap: 6px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  overflow-x: auto;
  flex-shrink: 0;
}
.page-nav-bar button {
  padding: 6px 14px; border: 1px solid #e5e7eb; border-radius: 8px;
  background: #fff; font-size: 13px; cursor: pointer; white-space: nowrap;
  display: flex; align-items: center; gap: 4px;
  transition: all 0.15s;
}
.page-nav-bar button:hover { border-color: #3b82f6; color: #2563eb; }
.page-nav-bar button.active { background: #2563eb; color: #fff; border-color: #2563eb; }
.page-nav-bar button.issue { border-color: #fca5a5; color: #dc2626; }
.page-nav-bar button.issue.active { background: #dc2626; color: #fff; border-color: #dc2626; }
.page-nav-bar button .dot { width: 6px; height: 6px; border-radius: 50%; background: #ef4444; }
.page-nav-bar button.active .dot { background: #fff; }

.split-layout {
  flex: 1;
  display: flex;
  overflow: hidden;
  min-height: 0;
}

/* Left: Snapshot */
.snapshot-panel {
  flex: 3;
  background: #1a1a2e;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  overflow: auto;
  min-width: 0;
}
.snapshot-wrap {
  position: relative;
  max-width: 100%;
  max-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.snapshot-wrap img {
  max-width: 100%;
  max-height: calc(100vh - 88px);
  object-fit: contain;
  border-radius: 4px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.4);
}
.annotation-layer {
  position: absolute;
  top: 0; left: 0;
  width: 100%; height: 100%;
  pointer-events: none;
}
.annotation-mark ellipse {
  fill: none;
  stroke: #ef4444;
  stroke-width: 2.5;
  stroke-dasharray: 6 4;
  animation: dashFlow 1.2s linear infinite;
}
.annotation-mark line {
  stroke: #ef4444;
  stroke-width: 1.5;
}
.annotation-label {
  background: rgba(239, 68, 68, 0.92);
  color: #fff;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.3;
  display: flex;
  flex-direction: column;
  box-shadow: 0 2px 8px rgba(0,0,0,0.3);
}
.annotation-label strong { font-weight: 600; }
.annotation-label span { font-size: 11px; opacity: 0.85; }

@keyframes dashFlow {
  to { stroke-dashoffset: -20; }
}

.snapshot-empty {
  color: #9ca3af;
  font-size: 15px;
}

/* Right: Findings */
.findings-panel {
  flex: 2;
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  min-width: 320px;
  max-width: 420px;
  overflow: hidden;
}
.findings-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}
.findings-header h2 { font-size: 16px; font-weight: 700; }
.count-badge {
  padding: 2px 10px; border-radius: 20px; font-size: 12px; font-weight: 600;
}
.count-badge.blocker { background: #fee2e2; color: #991b1b; }
.count-badge.manual { background: #fef3c7; color: #92400e; }
.count-badge.pass { background: #dcfce7; color: #166534; }
.count-badge.info { background: #f3f4f6; color: #4b5563; }

.findings-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.finding-card {
  padding: 14px;
  border-radius: 10px;
  border-left: 4px solid #d1d5db;
  background: #f9fafb;
}
.finding-card.blocker { border-left-color: #dc2626; background: #fef2f2; }
.finding-card.manual { border-left-color: #f59e0b; background: #fffbeb; }
.finding-card.info { border-left-color: #3b82f6; background: #eff6ff; }

.finding-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.finding-id { font-size: 11px; color: #6b7280; font-weight: 500; }
.finding-severity {
  font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px;
}
.blocker .finding-severity { background: #fee2e2; color: #991b1b; }
.manual .finding-severity { background: #fef3c7; color: #92400e; }
.info .finding-severity { background: #dbeafe; color: #1e40af; }

.finding-title { font-size: 14px; font-weight: 600; color: #1f2937; margin-bottom: 6px; }
.finding-message { font-size: 13px; color: #4b5563; line-height: 1.5; }

.findings-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #6b7280;
  padding: 0 24px;
  text-align: center;
}
.findings-empty p { font-size: 13px; line-height: 1.6; }
.findings-empty svg { width: 40px; height: 40px; fill: none; stroke: #22c55e; stroke-width: 2; }
.findings-empty.instruction svg,
.findings-empty.not-applicable svg { stroke: #9ca3af; }
</style>
