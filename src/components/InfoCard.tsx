import type { InfoItem } from "@/data/content";

export function InfoCard({ icon: Icon, title, description }: InfoItem) {
  return (
    <article className="rounded-lg border border-border bg-card p-6 transition-colors hover:border-primary/40">
      <div className="mb-4 inline-flex h-10 w-10 items-center justify-center rounded-md bg-secondary text-primary">
        <Icon className="h-5 w-5" aria-hidden="true" />
      </div>
      <h3 className="font-display text-lg font-semibold text-foreground">{title}</h3>
      <p className="mt-2 text-sm leading-relaxed text-muted-foreground">{description}</p>
    </article>
  );
}
