import { Link, Outlet, useRouterState } from "@tanstack/react-router";

import { Logo } from "@/components/Logo";
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarHeader,
  SidebarInset,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarProvider,
  SidebarTrigger,
} from "@/components/ui/sidebar";
import { appNavItems } from "@/data/app-nav";

export function AppLayout() {
  const ruta = useRouterState({ select: (estado) => estado.location.pathname });

  return (
    <SidebarProvider>
      <Sidebar>
        <SidebarHeader className="px-4 py-4">
          <Link
            to="/app"
            aria-label="Mantix, inicio de la aplicación"
            className="rounded focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sidebar-ring"
          >
            <Logo />
          </Link>
        </SidebarHeader>
        <SidebarContent>
          <SidebarGroup>
            <SidebarGroupContent>
              <SidebarMenu>
                {appNavItems.map(({ label, to, icon: Icono }) => (
                  <SidebarMenuItem key={to}>
                    <SidebarMenuButton asChild isActive={ruta.startsWith(to)}>
                      <Link to={to}>
                        <Icono />
                        <span>{label}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>
        </SidebarContent>
      </Sidebar>
      <SidebarInset>
        <header className="flex h-14 items-center gap-2 border-b border-border px-4">
          <SidebarTrigger aria-label="Mostrar u ocultar el menú" />
        </header>
        <div className="flex-1 p-6">
          <Outlet />
        </div>
      </SidebarInset>
    </SidebarProvider>
  );
}
