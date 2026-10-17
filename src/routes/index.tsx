import { createFileRoute } from "@tanstack/react-router";
import { Toaster } from "@/components/ui/sonner";
import { Header } from "@/components/Header";
import { Footer } from "@/components/Footer";
import { HeroSection, FeaturesSection, ValueSection, ClosingSection } from "@/sections/Sections";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Mantix — Gestión del mantenimiento industrial" },
      { name: "description", content: "Organiza equipos, incidencias y órdenes de trabajo desde un único lugar con Mantix." },
      { property: "og:title", content: "Mantix — Gestión del mantenimiento industrial" },
      { property: "og:description", content: "Equipos, incidencias y órdenes de trabajo, centralizados y trazables." },
      { property: "og:type", content: "website" },
      { name: "twitter:card", content: "summary_large_image" },
    ],
  }),
  component: Index,
});

function Index() {
  return (
    <div className="min-h-screen bg-background font-sans text-foreground">
      <Header />
      <main>
        <HeroSection />
        <FeaturesSection />
        <ValueSection />
        <ClosingSection />
      </main>
      <Footer />
      <Toaster position="top-center" />
    </div>
  );
}
