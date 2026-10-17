export function BrandMark() {
  return (
    <svg className="brand-mark" viewBox="0 0 64 64" aria-hidden="true">
      <defs>
        <linearGradient id="brand-gradient" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#ff5c74" />
          <stop offset="1" stopColor="#b4233c" />
        </linearGradient>
      </defs>
      <rect width="64" height="64" rx="16" fill="var(--color-text)" />
      <path
        d="M34 12c10 4 16 13 16 23 0 10-8 17-18 17-8 0-14-6-14-13 0-6 4-10 9-10 4 0 7 3 7 6 0 3-2 5-5 5"
        fill="none"
        stroke="url(#brand-gradient)"
        strokeWidth="5"
        strokeLinecap="round"
      />
    </svg>
  );
}
