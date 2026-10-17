import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Sheet } from './Sheet';

interface ConfirmSheetProps {
  open: boolean;
  title: string;
  children?: ReactNode;
  confirmLabel: string;
  danger?: boolean;
  busy?: boolean;
  onConfirm: () => void;
  onClose: () => void;
}

export function ConfirmSheet({ open, title, children, confirmLabel, danger, busy, onConfirm, onClose }: ConfirmSheetProps) {
  const { t } = useTranslation();
  return (
    <Sheet
      open={open}
      title={title}
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </button>
          <button type="button" className={`btn btn-block ${danger ? 'btn-danger' : 'btn-primary'}`} onClick={onConfirm} disabled={busy}>
            {busy ? t('common.loading') : confirmLabel}
          </button>
        </>
      }
    >
      <div className="muted">{children}</div>
    </Sheet>
  );
}
