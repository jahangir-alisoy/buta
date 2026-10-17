import type { TFunction } from 'i18next';

export type LegalKind = 'terms' | 'privacy';

export interface LegalDocument {
  title: string;
  paragraphs: string[];
  fileName: string;
}

export function legalDocument(kind: LegalKind, t: TFunction): LegalDocument {
  return kind === 'terms'
    ? { title: t('info.termsTitle'), paragraphs: [t('info.termsText')], fileName: 'naxis-terms.txt' }
    : { title: t('info.privacyTitle'), paragraphs: [t('info.privacyText'), t('account.dataInfo')], fileName: 'naxis-privacy-policy.txt' };
}

export function downloadLegalDocument(document: LegalDocument): void {
  const blob = new Blob([[document.title, ...document.paragraphs].join('\n\n')], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const link = window.document.createElement('a');
  link.href = url;
  link.download = document.fileName;
  window.document.body.appendChild(link);
  link.click();
  link.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}
