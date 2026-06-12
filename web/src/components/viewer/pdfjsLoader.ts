let pdfjsPromise: Promise<typeof import('pdfjs-dist')> | null = null

/**
 * Lazily loads pdf.js as an async chunk and registers its worker — the main
 * bundle never includes pdf.js; non-PDF users never pay for it.
 */
export function loadPdfjs(): Promise<typeof import('pdfjs-dist')> {
  pdfjsPromise ??= Promise.all([
    import('pdfjs-dist'),
    import('pdfjs-dist/build/pdf.worker.min.mjs?url')
  ]).then(([pdfjs, worker]) => {
    pdfjs.GlobalWorkerOptions.workerSrc = worker.default
    return pdfjs
  })
  return pdfjsPromise
}
