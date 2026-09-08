import { NavLink, Outlet } from "react-router-dom";

const linkClass = ({ isActive }: { isActive: boolean }) =>
  [
    "px-3 py-1.5 rounded-md text-sm font-medium transition-colors",
    isActive ? "bg-surface-800 text-white" : "text-neutral-400 hover:text-neutral-200",
  ].join(" ");

export function Layout() {
  return (
    <div className="min-h-screen">
      <header className="border-b border-white/10 bg-surface-850/60 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-4xl items-center gap-2 px-4">
          <NavLink to="/" className="mr-3 flex items-center gap-2">
            <span className="grid h-6 w-6 place-items-center rounded bg-brand-500 text-sm font-bold text-surface-950">
              L
            </span>
            <span className="text-sm font-semibold tracking-wide text-neutral-300">
              label-follower
            </span>
          </NavLink>
          <nav className="flex items-center gap-1">
            <NavLink to="/" end className={linkClass}>
              Painel
            </NavLink>
            <NavLink to="/introspect" className={linkClass}>
              Explorar
            </NavLink>
            <NavLink to="/consolidate" className={linkClass}>
              Consolidar
            </NavLink>
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-4xl px-4 py-8">
        <Outlet />
      </main>
    </div>
  );
}
