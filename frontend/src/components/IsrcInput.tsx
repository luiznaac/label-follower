import { useState } from "react";
import type { FormEvent, KeyboardEvent } from "react";
import { isValidIsrc, normalizeIsrc } from "../lib/isrc.ts";

interface Props {
  initial?: string;
  onSubmit: (isrc: string) => void;
}

export function IsrcInput({ initial = "", onSubmit }: Props) {
  const [value, setValue] = useState(initial);
  const normalized = normalizeIsrc(value);
  const valid = isValidIsrc(normalized);
  const showHint = value.trim().length > 0 && !valid;

  function run() {
    if (valid) onSubmit(normalized);
  }

  function submit(e: FormEvent) {
    e.preventDefault();
    run();
  }

  // The submit button is disabled while the ISRC is invalid, which suppresses the
  // form's implicit submission — handle Enter on the input directly.
  function onKeyDown(e: KeyboardEvent<HTMLInputElement>) {
    if (e.key === "Enter") {
      e.preventDefault();
      run();
    }
  }

  return (
    <form onSubmit={submit} className="flex flex-wrap items-start gap-2">
      <div className="flex flex-col">
        <input
          value={value}
          onChange={(e) => setValue(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="ISRC (ex.: BRABC1700001)"
          spellCheck={false}
          autoCapitalize="characters"
          className="w-64 rounded-md border border-surface-800 bg-surface-850 px-3 py-2 font-mono text-sm text-neutral-100 outline-none focus:border-brand-500"
        />
        {showHint && (
          <span className="mt-1 text-xs text-red-400">
            ISRC inválido — 12 caracteres (2 letras + 3 alfanum. + 7 dígitos).
          </span>
        )}
      </div>
      <button
        type="submit"
        disabled={!valid}
        className="rounded-md bg-brand-500 px-4 py-2 text-sm font-semibold text-surface-950 transition-colors hover:bg-brand-400 disabled:cursor-not-allowed disabled:opacity-40"
      >
        Buscar
      </button>
    </form>
  );
}
