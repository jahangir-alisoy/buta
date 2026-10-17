import { useEffect, useRef, type ReactNode } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';

interface SheetProps {
  open: boolean;
  title: string;
  onClose: () => void;
  children: ReactNode;
  footer?: ReactNode;
}

export function Sheet({ open, title, onClose, children, footer }: SheetProps) {
  const ref = useRef<HTMLDialogElement>(null);
  const { t } = useTranslation();

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) {
      return;
    }
    if (open && !dialog.open) {
      dialog.showModal();
      document.body.style.overflow = 'hidden';
    }
    if (!open && dialog.open) {
      dialog.close();
    }
    return () => {
      document.body.style.overflow = '';
    };
  }, [open]);

  return (
    <dialog
      ref={ref}
      className="sheet"
      aria-label={title}
      onClose={onClose}
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
      onClick={(event) => {
        if (event.target === ref.current) {
          onClose();
        }
      }}
    >
      {open && (
        <div className="sheet-panel">
          <div className="sheet-handle" aria-hidden="true" />
          <div className="sheet-header">
            <h2 className="section-title">{title}</h2>
            <button type="button" className="icon-btn" onClick={onClose} aria-label={t('common.close')}>
              <X size={20} />
            </button>
          </div>
          <div className="sheet-body">{children}</div>
          {footer && <div className="sheet-footer">{footer}</div>}
        </div>
      )}
    </dialog>
  );
}
