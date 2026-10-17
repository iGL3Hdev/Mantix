import { Logo } from "./Logo";
import { navItems, footer } from "@/data/content";

export function Footer() {
  return (
    <footer className="bg-primary text-primary-foreground">
      <div className="mx-auto grid max-w-6xl gap-8 px-5 py-12 md:grid-cols-[2fr_1fr]">
        <div>
          <Logo inverted />
          <p className="mt-4 max-w-sm text-sm text-primary-foreground/75">{footer.description}</p>
        </div>
        <nav aria-label="Pie de página">
          <ul className="flex flex-col gap-2 text-sm">
            {navItems.map((item) => (
              <li key={item.href}>
                <a href={item.href} className="text-primary-foreground/80 hover:text-primary-foreground hover:underline focus-visible:underline focus-visible:outline-none">
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
        </nav>
      </div>
      <div className="border-t border-primary-foreground/15">
        <div className="mx-auto flex max-w-6xl items-center gap-2 px-5 py-4 text-xs text-primary-foreground/70">
          <span className="h-2 w-2 rounded-full bg-accent" aria-hidden="true" />
          {footer.status} · © {new Date().getFullYear()} Mantix
        </div>
      </div>
    </footer>
  );
}
