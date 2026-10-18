const equipment = [
  { name: "Compresor C-02", state: "Operativo", tone: "bg-accent" },
  { name: "Prensa hidráulica P-11", state: "En revisión", tone: "bg-warning" },
  { name: "Cinta transportadora T-04", state: "Operativo", tone: "bg-accent" },
];

const orders = [
  { id: "OT-0142", task: "Cambio de rodamientos", state: "En curso" },
  { id: "OT-0141", task: "Revisión eléctrica", state: "Pendiente" },
  { id: "OT-0139", task: "Lubricación general", state: "Cerrada" },
];

const indicators = [
  { label: "Equipos", value: "24" },
  { label: "Incidencias abiertas", value: "3" },
  { label: "Órdenes en curso", value: "5" },
];

export function DashboardMock() {
  return (
    <figure
      aria-label="Representación ilustrativa de un panel de mantenimiento"
      className="select-none"
    >
      <div
        aria-hidden="true"
        className="overflow-hidden rounded-xl border border-border bg-card shadow-panel"
      >
        <div className="flex items-center gap-1.5 border-b border-border bg-secondary px-4 py-2.5">
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="h-2.5 w-2.5 rounded-full bg-border" />
          <span className="ml-3 text-xs font-medium text-muted-foreground">
            Panel de mantenimiento
          </span>
        </div>
        <div className="space-y-4 p-4">
          <div className="grid grid-cols-3 gap-3">
            {indicators.map((i) => (
              <div key={i.label} className="rounded-md border border-border p-3">
                <div className="font-display text-xl font-bold text-primary">{i.value}</div>
                <div className="text-[11px] leading-tight text-muted-foreground">{i.label}</div>
              </div>
            ))}
          </div>
          <div className="rounded-md border border-border">
            <div className="border-b border-border px-3 py-2 text-xs font-semibold text-foreground">
              Equipos
            </div>
            {equipment.map((e) => (
              <div key={e.name} className="flex items-center justify-between px-3 py-2 text-xs">
                <span className="text-foreground">{e.name}</span>
                <span className="inline-flex items-center gap-1.5 text-muted-foreground">
                  <span className={`h-2 w-2 rounded-full ${e.tone}`} />
                  {e.state}
                </span>
              </div>
            ))}
          </div>
          <div className="rounded-md border border-border">
            <div className="border-b border-border px-3 py-2 text-xs font-semibold text-foreground">
              Órdenes de trabajo
            </div>
            {orders.map((o) => (
              <div key={o.id} className="grid grid-cols-[auto_1fr_auto] gap-3 px-3 py-2 text-xs">
                <span className="font-mono text-primary">{o.id}</span>
                <span className="truncate text-foreground">{o.task}</span>
                <span className="text-muted-foreground">{o.state}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      <figcaption className="mt-3 text-center text-xs text-muted-foreground">
        Representación ilustrativa. Datos de ejemplo.
      </figcaption>
    </figure>
  );
}
