import { Boxes, ClipboardList, TriangleAlert, type LucideIcon } from "lucide-react";

export type AppNavItem = {
  label: string;
  to: "/app/equipos" | "/app/incidencias" | "/app/ordenes";
  icon: LucideIcon;
};

export const appNavItems: AppNavItem[] = [
  { label: "Equipos", to: "/app/equipos", icon: Boxes },
  { label: "Incidencias", to: "/app/incidencias", icon: TriangleAlert },
  { label: "Órdenes", to: "/app/ordenes", icon: ClipboardList },
];
