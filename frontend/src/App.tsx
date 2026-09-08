import type { RouteObject } from "react-router-dom";
import { Layout } from "./components/Layout.tsx";
import { Dashboard } from "./pages/Dashboard.tsx";
import { Explorer } from "./pages/Explorer.tsx";
import { Consolidate } from "./pages/Consolidate.tsx";
import { SpotifyCallback } from "./pages/SpotifyCallback.tsx";

export const routes: RouteObject[] = [
  {
    path: "/",
    element: <Layout />,
    children: [
      { index: true, element: <Dashboard /> },
      { path: "introspect", element: <Explorer /> },
      { path: "consolidate", element: <Consolidate /> },
    ],
  },
  // OAuth redirect target — no chrome; exchanges the code then bounces to /consolidate.
  { path: "/callback", element: <SpotifyCallback /> },
];
