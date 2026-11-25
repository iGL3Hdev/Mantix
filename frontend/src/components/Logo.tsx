interface LogoProps {
  inverted?: boolean;
}

export function Logo({ inverted = false }: LogoProps) {
  return (
    <span className="inline-flex items-center gap-2.5">
      <svg viewBox="0 0 32 32" className="h-8 w-8" aria-hidden="true">
        <rect
          width="32"
          height="32"
          rx="7"
          className={inverted ? "fill-primary-foreground" : "fill-primary"}
        />
        <g
          className={inverted ? "stroke-primary" : "stroke-primary-foreground"}
          strokeWidth="2.4"
          fill="none"
          strokeLinecap="round"
          strokeLinejoin="round"
        >
          <path d="M8 23V10l8 8 8-8v13" />
        </g>
        <circle cx="24" cy="23" r="2.4" className="fill-accent" />
      </svg>
      <span
        className={`font-display text-xl font-bold tracking-tight ${inverted ? "text-primary-foreground" : "text-primary"}`}
      >
        Mantix
      </span>
    </span>
  );
}
