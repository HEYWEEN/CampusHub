/**
 * 极简 + 安全的 markdown 渲染（用于 AI 助手气泡）。
 *
 * 安全策略：先把原文 HTML 实体转义（中和任何 <script> 等注入），再仅注入我们自己生成的
 * 白名单标签（p/br/strong/em/code/ul/ol/li），不产生任何属性 / href / 事件，
 * 因此可安全用于 dangerouslySetInnerHTML。不支持链接（避免 javascript: 协议风险）。
 *
 * 支持子集：**粗体** *斜体* `行内代码`、- / * 无序列表、1. 有序列表、换行。
 */
function escapeHtml(s: string): string {
  return s
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function inline(s: string): string {
  return s
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/(?<![\w*])\*([^*\n]+)\*(?![\w*])/g, '<em>$1</em>')
}

export function miniMarkdown(src: string): string {
  const lines = escapeHtml(src ?? '').split('\n')
  const out: string[] = []
  let listType: 'ul' | 'ol' | null = null
  const closeList = () => {
    if (listType) { out.push(`</${listType}>`); listType = null }
  }
  for (const line of lines) {
    const ul = /^\s*[-*]\s+(.*)$/.exec(line)
    const ol = /^\s*\d+\.\s+(.*)$/.exec(line)
    if (ul) {
      if (listType !== 'ul') { closeList(); out.push('<ul>'); listType = 'ul' }
      out.push(`<li>${inline(ul[1])}</li>`)
    } else if (ol) {
      if (listType !== 'ol') { closeList(); out.push('<ol>'); listType = 'ol' }
      out.push(`<li>${inline(ol[1])}</li>`)
    } else {
      closeList()
      if (line.trim() !== '') out.push(`<p>${inline(line)}</p>`)
    }
  }
  closeList()
  return out.join('')
}
