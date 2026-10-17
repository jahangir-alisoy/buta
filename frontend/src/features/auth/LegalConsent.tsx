import { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { Download } from 'lucide-react';
import { Sheet } from '../../design-system/components/Sheet';
import { downloadLegalDocument, legalDocument, type LegalKind } from '../info/legal';

function LegalSheet({ kind, onClose }: { kind: LegalKind | null; onClose: () => void }) {
  const { t } = useTranslation();
  const document = kind ? legalDocument(kind, t) : null;
  return (
    <Sheet
      open={document !== null}
      title={document?.title ?? ''}
      onClose={onClose}
      footer={
        document && (
          <button type="button" className="btn btn-secondary btn-block" onClick={() => downloadLegalDocument(document)}>
            <Download size={18} aria-hidden="true" />
            {t('auth.downloadDocument')}
          </button>
        )
      }
    >
      {document && (
        <div className="stack muted">
          {document.paragraphs.map((paragraph) => (
            <p key={paragraph}>{paragraph}</p>
          ))}
        </div>
      )}
    </Sheet>
  );
}

function LegalText({ i18nKey, onOpen }: { i18nKey: string; onOpen: (kind: LegalKind) => void }) {
  return (
    <Trans
      i18nKey={i18nKey}
      components={{
        terms: <button type="button" className="legal-link" onClick={() => onOpen('terms')} />,
        privacy: <button type="button" className="legal-link" onClick={() => onOpen('privacy')} />,
      }}
    />
  );
}

export function ConsentCheckbox({ checked, onChange }: { checked: boolean; onChange: (checked: boolean) => void }) {
  const [open, setOpen] = useState<LegalKind | null>(null);
  return (
    <>
      <label className="checkbox legal-consent">
        <input type="checkbox" checked={checked} onChange={(event) => onChange(event.target.checked)} />
        <span>
          <LegalText i18nKey="auth.consentAgree" onOpen={setOpen} />
        </span>
      </label>
      <LegalSheet kind={open} onClose={() => setOpen(null)} />
    </>
  );
}

export function ConsentNote() {
  const [open, setOpen] = useState<LegalKind | null>(null);
  return (
    <>
      <p className="legal-note">
        <LegalText i18nKey="auth.consentNote" onOpen={setOpen} />
      </p>
      <LegalSheet kind={open} onClose={() => setOpen(null)} />
    </>
  );
}
