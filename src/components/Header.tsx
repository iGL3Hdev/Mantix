import { useState } from "react";
import { Menu, X } from "lucide-react";
import { toast } from "sonner";
import { Logo } from "./Logo";
import { ActionButton } from "./ActionButton";
import { navItems, accessMessage } from "@/data/content";

const showAccessMessage = () => toast(accessMessage);

export function Header() {
  const [open, setOpen] = useState(false);
  const close = () => setOpen(false);

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/95 backdrop-blur">
      <div className="mx-auto flex h-16 max-w-6xl items-center justify-between px-5">
        <a href="#inicio" aria-label="Mantix, inicio" className="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring">
          <Logo />
        </a>
        <nav className="hidden items-center gap-8 md:flex" aria-label="Principal">
          {navItems.map((item) => (
            <a key={item.href} href={item.href} className="text-sm font-medium text-foreground/80 transition-colors hover:text-primary focus-visible:text-primary focus-visible:outline-none">
              {item.label}
            </a>
          ))}
          <ActionButton onClick={showAccessMessage}>Acceder</ActionButton>
        </nav>
        <button
          type="button"
          className="rounded-md p-2 text-foreground hover:bg-secondary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring md:hidden"
          aria-label={open ? "Cerrar menú" : "Abrir menú"}
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          {open ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>
      {open && (
        <nav className="border-t border-border bg-background px-5 py-4 md:hidden" aria-label="Móvil">
          <ul className="flex flex-col gap-1">
            {navItems.map((item) => (
              <li key={item.href}>
                <a href={item.href} onClick={close} className="block rounded-md px-3 py-2 text-sm font-medium text-foreground hover:bg-secondary">
                  {item.label}
                </a>
              </li>
            ))}
          </ul>
          <div className="mt-3">
            <ActionButton onClick={() => { close(); showAccessMessage(); }}>Acceder</ActionButton>
          </div>
        </nav>
      )}
    </header>
  );
}
