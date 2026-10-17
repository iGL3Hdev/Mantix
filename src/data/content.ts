import type { LucideIcon } from "lucide-react";
import { Cog, AlertTriangle, ClipboardList, History, Database, Activity, Eye } from "lucide-react";

export interface NavItem {
  label: string;
  href: `#${string}`;
}

export interface InfoItem {
  icon: LucideIcon;
  title: string;
  description: string;
}

export const navItems: NavItem[] = [
  { label: "Inicio", href: "#inicio" },
  { label: "Funcionalidades", href: "#funcionalidades" },
  { label: "Contacto", href: "#contacto" },
];

export const accessMessage = "El acceso a Mantix estará disponible próximamente.";

export const hero = {
  eyebrow: "Gestión del mantenimiento industrial",
  title: "El mantenimiento industrial, bajo control.",
  subtitle:
    "Organiza equipos, incidencias y órdenes de trabajo desde un único lugar. Mantén la información centralizada y mejora la trazabilidad de las actuaciones.",
  primary: "Descubrir Mantix",
  secondary: "Explorar funcionalidades",
};

export const features: InfoItem[] = [
  { icon: Cog, title: "Gestión de equipos", description: "Centraliza la información de máquinas e instalaciones." },
  { icon: AlertTriangle, title: "Incidencias", description: "Registra problemas y facilita su seguimiento." },
  { icon: ClipboardList, title: "Órdenes de trabajo", description: "Organiza las tareas de mantenimiento y su estado." },
  { icon: History, title: "Historial y trazabilidad", description: "Consulta las actuaciones realizadas y su evolución." },
];

export const value = {
  title: "Menos improvisación. Más control.",
  text: "Cuando la información de mantenimiento está estructurada, planificar, hacer seguimiento y decidir deja de depender de la memoria o de hojas dispersas.",
  items: [
    { icon: Database, title: "Información centralizada", description: "Equipos, incidencias y órdenes en un mismo sistema, con un único punto de consulta." },
    { icon: Activity, title: "Seguimiento de actuaciones", description: "Cada intervención queda registrada, con su responsable, su estado y su historial." },
    { icon: Eye, title: "Visibilidad del estado", description: "Una visión clara de qué está pendiente, qué está en curso y qué se ha resuelto." },
  ] satisfies InfoItem[],
};

export const closing = {
  title: "Prepara el siguiente paso en la gestión del mantenimiento.",
  text: "Mantix nace para convertir la información de mantenimiento en una herramienta útil para el trabajo diario.",
  cta: "Volver al inicio",
};

export const footer = {
  description: "Aplicación web para la gestión del mantenimiento industrial: equipos, incidencias y órdenes de trabajo.",
  status: "Proyecto en desarrollo",
};
