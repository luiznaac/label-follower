import type { ReactNode } from "react";

interface Props {
  label: string;
  value: ReactNode;
  tone?: "default" | "brand" | "muted";
}

const toneClass: Record<NonNullable<Props["tone"]>, string> = {
  default: "text-neutral-100",
  brand: "text-brand-400",
  muted: "text-neutral-500",
};

export function StatTile({ label, value, tone = "default" }: Props) {
  return (
    <div className="rounded-lg border border-surface-800 bg-surface-850 px-4 py-3">
      <p className="text-xs uppercase tracking-wide text-neutral-500">{label}</p>
      <p className={`mt-1 text-lg font-semibold tabular-nums ${toneClass[tone]}`}>{value}</p>
    </div>
  );
}
