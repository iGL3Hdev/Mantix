import type { ReactNode } from "react";

type Variant = "primary" | "secondary" | "ghost";

const styles: Record<Variant, string> = {
  primary: "bg-primary text-primary-foreground hover:bg-primary/90",
  secondary: "border border-border bg-card text-foreground hover:border-primary hover:text-primary",
  ghost:
    "text-primary-foreground border border-primary-foreground/30 hover:bg-primary-foreground/10",
};

interface ActionButtonProps {
  children: ReactNode;
  variant?: Variant;
  href?: string;
  onClick?: () => void;
}

export function ActionButton({ children, variant = "primary", href, onClick }: ActionButtonProps) {
  const className = `inline-flex items-center justify-center gap-2 rounded-md px-5 py-2.5 text-sm font-semibold transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 ${styles[variant]}`;
  if (href)
    return (
      <a href={href} className={className}>
        {children}
      </a>
    );
  return (
    <button type="button" onClick={onClick} className={className}>
      {children}
    </button>
  );
}
