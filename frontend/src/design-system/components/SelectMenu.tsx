import { useEffect, useId, useRef, useState, type KeyboardEvent, type ReactNode } from 'react';
import { Check, ChevronDown } from 'lucide-react';

interface SelectMenuProps<T extends string> {
  options: { value: T; label: string }[];
  value: T;
  onChange: (value: T) => void;
  label: string;
  icon?: ReactNode;
}

export function SelectMenu<T extends string>({ options, value, onChange, label, icon }: SelectMenuProps<T>) {
  const id = useId();
  const root = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const list = useRef<HTMLUListElement>(null);
  const [open, setOpen] = useState(false);
  const selected = Math.max(0, options.findIndex((option) => option.value === value));
  const [active, setActive] = useState(selected);

  useEffect(() => {
    if (!open) {
      return;
    }
    list.current?.focus();
    const closeOutside = (event: PointerEvent) => {
      if (!root.current?.contains(event.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('pointerdown', closeOutside);
    return () => document.removeEventListener('pointerdown', closeOutside);
  }, [open]);

  useEffect(() => {
    if (open) {
      list.current?.children[active]?.scrollIntoView({ block: 'nearest' });
    }
  }, [open, active]);

  const show = (index: number) => {
    setActive(index);
    setOpen(true);
  };

  const close = () => {
    setOpen(false);
    trigger.current?.focus();
  };

  const choose = (index: number) => {
    close();
    if (options[index].value !== value) {
      onChange(options[index].value);
    }
  };

  const onTriggerKey = (event: KeyboardEvent<HTMLButtonElement>) => {
    if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault();
      show(selected);
    }
  };

  const onListKey = (event: KeyboardEvent<HTMLUListElement>) => {
    const last = options.length - 1;
    const moves: Record<string, () => void> = {
      ArrowDown: () => setActive((current) => Math.min(last, current + 1)),
      ArrowUp: () => setActive((current) => Math.max(0, current - 1)),
      Home: () => setActive(0),
      End: () => setActive(last),
      Enter: () => choose(active),
      ' ': () => choose(active),
      Escape: close,
      Tab: () => setOpen(false),
    };
    const move = moves[event.key];
    if (move) {
      if (event.key !== 'Tab') {
        event.preventDefault();
      }
      move();
    }
  };

  return (
    <div className={`select-menu${open ? ' is-open' : ''}`} ref={root}>
      <button
        ref={trigger}
        type="button"
        className="select-menu-trigger"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={`${id}-list`}
        aria-label={`${label}: ${options[selected]?.label ?? ''}`}
        onClick={() => (open ? setOpen(false) : show(selected))}
        onKeyDown={onTriggerKey}
      >
        {icon}
        <span className="select-menu-value">{options[selected]?.label}</span>
        <ChevronDown size={16} className="select-menu-chevron" aria-hidden="true" />
      </button>
      {open && (
        <ul
          ref={list}
          id={`${id}-list`}
          className="select-menu-list"
          role="listbox"
          aria-label={label}
          tabIndex={-1}
          aria-activedescendant={`${id}-${active}`}
          onKeyDown={onListKey}
        >
          {options.map((option, index) => (
            <li
              key={option.value}
              id={`${id}-${index}`}
              role="option"
              aria-selected={index === selected}
              className={index === active ? 'is-active' : undefined}
              onPointerMove={() => setActive(index)}
              onClick={() => choose(index)}
            >
              <span>{option.label}</span>
              {index === selected && <Check size={16} aria-hidden="true" />}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
