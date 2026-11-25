import { ArrowRight } from "lucide-react";
import { ActionButton } from "@/components/ActionButton";
import { InfoCard } from "@/components/InfoCard";
import { DashboardMock } from "./DashboardMock";
import { hero, features, value, closing } from "@/data/content";

export function HeroSection() {
  return (
    <section id="inicio" className="scroll-mt-16 bg-grid">
      <div className="mx-auto grid max-w-6xl items-center gap-12 px-5 py-16 md:py-24 lg:grid-cols-2">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent-strong">
            {hero.eyebrow}
          </p>
          <h1 className="mt-4 font-display text-4xl font-bold leading-tight tracking-tight text-primary md:text-5xl">
            {hero.title}
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-relaxed text-muted-foreground">
            {hero.subtitle}
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <ActionButton href="#valor">
              {hero.primary} <ArrowRight className="h-4 w-4" />
            </ActionButton>
            <ActionButton href="#funcionalidades" variant="secondary">
              {hero.secondary}
            </ActionButton>
          </div>
        </div>
        <DashboardMock />
      </div>
    </section>
  );
}

function SectionHeading({ title, text }: { title: string; text?: string }) {
  return (
    <div className="max-w-2xl">
      <h2 className="font-display text-3xl font-bold tracking-tight text-primary md:text-4xl">
        {title}
      </h2>
      {text && <p className="mt-4 text-base leading-relaxed text-muted-foreground">{text}</p>}
    </div>
  );
}

export function FeaturesSection() {
  return (
    <section
      id="funcionalidades"
      className="scroll-mt-16 border-t border-border bg-background py-20"
    >
      <div className="mx-auto max-w-6xl px-5">
        <SectionHeading
          title="Funcionalidades"
          text="Las piezas básicas para organizar el mantenimiento de una planta."
        />
        <div className="mt-10 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f) => (
            <InfoCard key={f.title} {...f} />
          ))}
        </div>
      </div>
    </section>
  );
}

export function ValueSection() {
  return (
    <section id="valor" className="scroll-mt-16 bg-secondary py-20">
      <div className="mx-auto max-w-6xl px-5">
        <SectionHeading title={value.title} text={value.text} />
        <div className="mt-10 grid gap-5 md:grid-cols-3">
          {value.items.map((item, i) => (
            <div key={item.title} className="border-l-2 border-accent bg-card p-6">
              <span className="font-mono text-xs text-muted-foreground">0{i + 1}</span>
              <item.icon className="mt-3 h-6 w-6 text-primary" aria-hidden="true" />
              <h3 className="mt-3 font-display text-lg font-semibold text-foreground">
                {item.title}
              </h3>
              <p className="mt-2 text-sm leading-relaxed text-muted-foreground">
                {item.description}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

export function ClosingSection() {
  return (
    <section id="contacto" className="scroll-mt-16 bg-background py-20">
      <div className="mx-auto max-w-4xl rounded-xl border border-border bg-card px-6 py-14 text-center md:px-12">
        <h2 className="font-display text-3xl font-bold tracking-tight text-primary md:text-4xl">
          {closing.title}
        </h2>
        <p className="mx-auto mt-4 max-w-xl text-muted-foreground">{closing.text}</p>
        <div className="mt-8">
          <ActionButton href="#inicio">{closing.cta}</ActionButton>
        </div>
      </div>
    </section>
  );
}
