import DOMPurify from 'dompurify'

// Tags the RichTextEditor toolbar can produce. Anything else (spans, divs,
// MS-Office wrappers, images, scripts, …) is stripped at display and paste
// time. Keeping the allow-list narrow is what stops Word / Google Docs /
// Chrome pastes from polluting saved content with inline color/background
// styles that show up as raw text on cards that render plain text.
const ALLOWED_TAGS = ['p', 'br', 'strong', 'b', 'em', 'i', 'u', 'ul', 'ol', 'li']

// No attributes at all — kills `style=""`, `class=""`, `data-*`, `href`, etc.
const ALLOWED_ATTR: string[] = []

export function sanitizeHtml(html: string | null | undefined): string {
  if (!html) return '-'
  return DOMPurify.sanitize(html, { ALLOWED_TAGS, ALLOWED_ATTR })
}

// Same allow-list but returns an empty string for empty input — for places
// (the rich-text editor's paste pipeline, plain-text projections) where the
// "-" placeholder is wrong.
export function sanitizeRichText(html: string | null | undefined): string {
  if (!html) return ''
  return DOMPurify.sanitize(html, { ALLOWED_TAGS, ALLOWED_ATTR })
}

// Project rich-text HTML to plain text — used for collapsed previews where
// we want a single truncated string rather than rendered formatting.
export function htmlToPlainText(html: string | null | undefined): string {
  if (!html) return ''
  const sanitized = sanitizeRichText(html)
  const withBreaks = sanitized.replace(/<\/(p|li)>/gi, '</$1> ')
  const doc = new DOMParser().parseFromString(withBreaks, 'text/html')
  return doc.body.textContent?.replace(/\s+/g, ' ').trim() ?? ''
}
