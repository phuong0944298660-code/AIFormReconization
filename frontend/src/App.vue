<script setup>
import { computed, reactive, ref, watch } from 'vue'

const apiBase = import.meta.env.VITE_API_BASE || ''
const file = ref(null)
const fileName = ref('')
const loading = ref(false)
const error = ref('')
const response = ref(null)
const activePage = ref(1)
const activeTab = ref('upload')
const activeResultTab = ref('failures')
const activeUploadSection = ref('basic')

const metadata = reactive({
  applicantAge: '',
  sponsorType: 'institution',
  hasAcceptanceLetter: false,
  hasApplicantTravelDocumentCopy: false,
  hasApplicantFinancialProof: false,
  hasGuardianConsent: false,
  hasAccommodationProof: false,
  hasSponsorForm: false,
  hasSponsorIdentityCopy: false,
  hasSponsorFinancialProof: false,
  mainlandApplicationViaSchool: false,
  isMainlandChineseResident: false,
  hasDependants: false,
  dependantDocumentsComplete: false,
  documentsNeedTranslation: false
})

const currentSnapshot = computed(() => {
  const pages = response.value?.pageSnapshots || []
  return pages.find((page) => page.page === activePage.value) || pages[0] || null
})

const pageAnnotations = computed(() => {
  return (response.value?.annotations || []).filter((annotation) => annotation.page === activePage.value)
})

const blockingFindings = computed(() =>
  (response.value?.findings || []).filter((finding) => finding.severity === 'BLOCKING')
)

const manualFindings = computed(() =>
  (response.value?.findings || []).filter((finding) => finding.severity === 'MANUAL_REVIEW')
)

const visiblePages = computed(() => {
  const pages = response.value?.pageSnapshots || []
  return pages.map((page) => ({
    page: page.page,
    count: (response.value?.annotations || []).filter((annotation) => annotation.page === page.page).length
  }))
})

const fileReadSummary = computed(() => {
  if (!response.value) return ''
  const pageCount = response.value.pageSnapshots?.length || 0
  return pageCount > 0
    ? `已读取 ${pageCount} 页申请表，并完成 ID995A 审核规则比对。`
    : '已完成 ID995A 审核规则比对。'
})

const recognizedFieldCount = computed(() =>
  (response.value?.extractedFields || []).filter((field) => field.present).length
)

const totalFieldCount = computed(() => response.value?.extractedFields?.length || 0)

const ruleEvidenceCount = computed(() => response.value?.retrievedRules?.length || 0)

const resultSections = computed(() => [
  { key: 'failures', label: '不通过理由', count: blockingFindings.value.length },
  { key: 'manual', label: '需人工复核', count: manualFindings.value.length },
  { key: 'rules', label: '规则比对依据', count: ruleEvidenceCount.value },
  { key: 'fields', label: '申请表项目检查', count: recognizedFieldCount.value }
])

watch(response, (value) => {
  if (!value) return
  const firstAnnotated = value.annotations?.find((annotation) => annotation.severity === 'BLOCKING') || value.annotations?.[0]
  activePage.value = firstAnnotated?.page || value.pageSnapshots?.[0]?.page || 1
  const blockerCount = value.findings?.filter((finding) => finding.severity === 'BLOCKING').length || 0
  const manualCount = value.findings?.filter((finding) => finding.severity === 'MANUAL_REVIEW').length || 0
  const ruleCount = value.retrievedRules?.length || 0
  activeResultTab.value = blockerCount > 0 ? 'failures' : manualCount > 0 ? 'manual' : ruleCount > 0 ? 'rules' : 'fields'
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
  activeTab.value = 'upload'
  activeResultTab.value = 'failures'
  activeUploadSection.value = 'basic'
}

async function submitReview() {
  if (!file.value) {
    error.value = '请选择已填写申请表文件。'
    return
  }

  loading.value = true
  error.value = ''
  response.value = null

  try {
    const body = new FormData()
    body.append('file', file.value)
    body.append('metadata', JSON.stringify(metadata))

    const result = await fetch(`${apiBase}/api/review`, {
      method: 'POST',
      body
    })

    if (!result.ok) {
      throw new Error(`HTTP ${result.status}`)
    }

    response.value = await result.json()
    activeTab.value = 'summary'
  } catch {
    error.value = '审核未能完成，请确认服务已启动后重试。'
  } finally {
    loading.value = false
  }
}

function annotationStyle(annotation) {
  const x = annotation.x * 1000
  const y = annotation.y * 1414
  const width = annotation.width * 1000
  const height = annotation.height * 1414
  return {
    cx: x + width / 2,
    cy: y + height / 2,
    rx: Math.max(width / 2, 18),
    ry: Math.max(height / 2, 18),
    lineX1: x + width,
    lineY1: y + height / 2,
    lineX2: Math.min(970, x + width + 110),
    lineY2: Math.max(36, y + height / 2 - 28),
    labelX: Math.min(830, x + width + 114),
    labelY: Math.max(30, y + height / 2 - 46)
  }
}

function severityLabel(severity) {
  if (severity === 'BLOCKING') return '不通过'
  if (severity === 'MANUAL_REVIEW') return '复核'
  return '提示'
}
</script>

<template>
  <main class="app-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">香港入境处业务演示</p>
        <h1>ID995A 来港就读申请表预审</h1>
      </div>
      <a class="rules-link" href="/api/rules/id995a" target="_blank" rel="noreferrer">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 3h9l3 3v15H6z" />
          <path d="M14 3v4h4" />
          <path d="M8.5 11h7M8.5 15h7M8.5 18h4" />
        </svg>
        审核规则
      </a>
    </header>

    <nav class="review-tabs" aria-label="审核流程">
      <button type="button" :class="{ active: activeTab === 'upload' }" @click="activeTab = 'upload'">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3v12" />
          <path d="m7 8 5-5 5 5" />
          <path d="M5 15v4h14v-4" />
        </svg>
        <span>上传</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'summary' }" @click="activeTab = 'summary'">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M20 6 9 17l-5-5" />
          <path d="M4 20h16" />
        </svg>
        <span>整体结论+文件快照</span>
      </button>
      <button type="button" :class="{ active: activeTab === 'details' }" @click="activeTab = 'details'">
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 3h9l3 3v15H6z" />
          <path d="M14 3v4h4" />
          <path d="M8.5 11h7M8.5 15h7M8.5 18h4" />
        </svg>
        <span>审核结果</span>
      </button>
    </nav>

    <section class="workspace">
      <section v-show="activeTab === 'upload'" class="tab-panel upload-tab">
        <aside class="panel intake upload-card">
          <div class="section-title">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M12 3v12" />
              <path d="m7 8 5-5 5 5" />
              <path d="M5 15v4h14v-4" />
            </svg>
            <h2>上传申请表</h2>
          </div>

          <label class="dropzone" @drop.prevent="onDrop" @dragover.prevent>
            <input type="file" accept="application/pdf" @change="onFileChange" />
            <span class="drop-icon">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M7 18h10a4 4 0 0 0 .7-7.94A6 6 0 0 0 6.5 8.1 4.5 4.5 0 0 0 7 18Z" />
                <path d="M12 12v6M9.5 14.5 12 12l2.5 2.5" />
              </svg>
            </span>
            <strong>{{ fileName || '选择或拖入已填写申请表' }}</strong>
          </label>

          <nav class="upload-sections" aria-label="补充资料分类">
            <button type="button" :class="{ active: activeUploadSection === 'basic' }" @click="activeUploadSection = 'basic'">
              基本资料
            </button>
            <button type="button" :class="{ active: activeUploadSection === 'attachments' }" @click="activeUploadSection = 'attachments'">
              随附材料
            </button>
            <button type="button" :class="{ active: activeUploadSection === 'conditions' }" @click="activeUploadSection = 'conditions'">
              申请条件
            </button>
          </nav>

          <section v-show="activeUploadSection === 'basic'" class="upload-subpanel">
            <h3>基本资料</h3>
            <div class="field-row">
              <label>
                <span>申请人年龄</span>
                <input v-model="metadata.applicantAge" inputmode="numeric" placeholder="例如 19" />
              </label>
              <label>
                <span>保证人</span>
                <select v-model="metadata.sponsorType">
                  <option value="institution">取录院校</option>
                  <option value="individual">个人</option>
                  <option value="unknown">未知</option>
                </select>
              </label>
            </div>
          </section>

          <section v-show="activeUploadSection === 'attachments'" class="upload-subpanel check-group">
            <h3>随附材料</h3>
            <label><input v-model="metadata.hasAcceptanceLetter" type="checkbox" /> 取录信</label>
            <label><input v-model="metadata.hasApplicantTravelDocumentCopy" type="checkbox" /> 旅行证件副本</label>
            <label><input v-model="metadata.hasApplicantFinancialProof" type="checkbox" /> 申请人经济证明</label>
            <label><input v-model="metadata.hasGuardianConsent" type="checkbox" /> 未成年人监护同意书</label>
            <label><input v-model="metadata.hasAccommodationProof" type="checkbox" /> 未成年人住宿证明</label>
            <label><input v-model="metadata.hasSponsorForm" type="checkbox" /> ID995B 保证人表格</label>
            <label><input v-model="metadata.hasSponsorIdentityCopy" type="checkbox" /> 保证人身份证明</label>
            <label><input v-model="metadata.hasSponsorFinancialProof" type="checkbox" /> 保证人经济证明</label>
          </section>

          <section v-show="activeUploadSection === 'conditions'" class="upload-subpanel check-group">
            <h3>申请条件</h3>
            <label><input v-model="metadata.isMainlandChineseResident" type="checkbox" /> 内地中国居民</label>
            <label><input v-model="metadata.mainlandApplicationViaSchool" type="checkbox" /> 经取录院校递交</label>
            <label><input v-model="metadata.hasDependants" type="checkbox" /> 有随行受养人</label>
            <label><input v-model="metadata.dependantDocumentsComplete" type="checkbox" /> 受养人材料完整</label>
            <label><input v-model="metadata.documentsNeedTranslation" type="checkbox" /> 存在非中英文文件</label>
          </section>

          <button class="primary-action" type="button" :disabled="loading" @click="submitReview">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M5 12h14" />
              <path d="m13 6 6 6-6 6" />
            </svg>
            {{ loading ? '审核中' : '开始审核' }}
          </button>

          <p v-if="error" class="error-text">{{ error }}</p>
        </aside>
      </section>

      <section v-show="activeTab === 'summary'" class="tab-panel summary-tab">
        <div class="summary-layout">
          <aside class="panel summary-panel">
            <div class="section-title">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M20 6 9 17l-5-5" />
              </svg>
              <h2>整体结论</h2>
            </div>

            <div v-if="response" class="verdict" :class="{ pass: response.passed, fail: !response.passed }">
              <span>整体结论</span>
              <strong>{{ response.verdict }}</strong>
            </div>

            <section v-if="response" class="summary-note">
              <h3>文件读取情况</h3>
              <p>{{ fileReadSummary }}</p>
              <p v-if="response.passed">未发现不通过项目。</p>
              <p v-else>不通过项目已在文件快照中用红圈标示。</p>
              <button class="secondary-action" type="button" @click="activeTab = 'details'">
                查看审核结果
              </button>
            </section>

            <div v-if="!response" class="placeholder-result compact">
              <p>上传申请表后，这里会显示整体结论和文件定位。</p>
            </div>
          </aside>

          <section class="panel document-panel">
            <div class="section-title">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M4 5h16v14H4z" />
                <path d="M8 9h8M8 13h5" />
              </svg>
              <h2>文件快照</h2>
            </div>

            <div v-if="response" class="page-tabs" aria-label="页面切换">
              <button
                v-for="page in visiblePages"
                :key="page.page"
                type="button"
                :class="{ active: page.page === activePage }"
                @click="activePage = page.page"
              >
                第 {{ page.page }} 页
                <span v-if="page.count">{{ page.count }}</span>
              </button>
            </div>

            <div v-if="currentSnapshot" class="snapshot-wrap">
              <img :src="currentSnapshot.dataUrl" alt="ID995A uploaded page snapshot" />
              <svg class="annotation-layer" viewBox="0 0 1000 1414" preserveAspectRatio="none" aria-hidden="true">
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
                    width="150"
                    height="54"
                  >
                    <div class="annotation-label">
                      <strong>{{ annotation.label }}</strong>
                      <span>{{ severityLabel(annotation.severity) }}</span>
                    </div>
                  </foreignObject>
                </g>
              </svg>
            </div>

            <div v-else class="empty-state">
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 3h8l4 4v14H6z" />
                <path d="M14 3v5h4" />
                <path d="M9 13h6M9 17h4" />
              </svg>
              <p>等待审核结果</p>
            </div>
          </section>
        </div>
      </section>

      <section v-show="activeTab === 'details'" class="tab-panel details-tab">
        <aside class="panel results">
          <div class="section-title">
            <svg viewBox="0 0 24 24" aria-hidden="true">
              <path d="M6 3h9l3 3v15H6z" />
              <path d="M14 3v4h4" />
              <path d="M8.5 11h7M8.5 15h7M8.5 18h4" />
            </svg>
            <h2>审核结果</h2>
          </div>

          <div v-if="response" class="result-workspace">
            <nav class="result-menu" aria-label="审核结果分类">
              <button
                v-for="section in resultSections"
                :key="section.key"
                type="button"
                :class="{ active: activeResultTab === section.key }"
                @click="activeResultTab = section.key"
              >
                <span>{{ section.label }}</span>
                <strong>{{ section.count }}</strong>
              </button>
            </nav>

            <div class="result-content">
              <section v-show="activeResultTab === 'failures'" class="finding-list">
                <h3>不通过理由</h3>
                <article v-for="finding in blockingFindings" :key="finding.ruleId" class="finding blocker">
                  <small>{{ finding.ruleId }} · 第 {{ finding.page }} 页</small>
                  <strong>{{ finding.title }}</strong>
                  <p>{{ finding.message }}</p>
                </article>
                <p v-if="!blockingFindings.length" class="detail-empty">未发现不通过项目。</p>
              </section>

              <section v-show="activeResultTab === 'manual'" class="finding-list">
                <h3>需人工复核</h3>
                <article v-for="finding in manualFindings" :key="finding.ruleId" class="finding manual">
                  <small>{{ finding.ruleId }} · 第 {{ finding.page }} 页</small>
                  <strong>{{ finding.title }}</strong>
                  <p>{{ finding.message }}</p>
                </article>
                <p v-if="!manualFindings.length" class="detail-empty">暂无需人工复核事项。</p>
              </section>

              <section v-show="activeResultTab === 'rules'" class="rule-check-box">
                <h3>规则比对依据</h3>
                <article v-for="item in response.retrievedRules" :key="item.chunkId">
                  <strong>{{ item.chunkId }} · {{ item.title }}</strong>
                  <p>{{ item.content }}</p>
                  <small>适用审核依据</small>
                </article>
                <p v-if="!response.retrievedRules?.length" class="detail-empty">暂无规则比对依据。</p>
                <p v-else class="rule-check-status">已按 ID995A 审核规则完成比对。</p>
              </section>

              <section v-show="activeResultTab === 'fields'" class="field-grid">
                <h3>申请表项目检查</h3>
                <p class="field-summary">申请表已填写项目 {{ recognizedFieldCount }} / {{ totalFieldCount }} 项。</p>
                <div>
                  <span v-for="field in response.extractedFields" :key="field.key" :class="{ ok: field.present }">
                    {{ field.label }}
                  </span>
                </div>
              </section>
            </div>
          </div>

          <div v-if="!response" class="placeholder-result">
            <p>上传后会显示不通过理由、需复核事项和规则比对依据。</p>
          </div>
        </aside>
      </section>
    </section>
  </main>
</template>
