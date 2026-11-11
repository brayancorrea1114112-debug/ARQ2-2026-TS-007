"use client";

import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { login } from "@/features/auth/api";
import { useSessionStore } from "@/features/auth/session";
import { isApiError } from "@/shared/api/errors";
import { Button } from "@/shared/ui/Button";
import { ErrorNotice } from "@/shared/ui/ErrorNotice";
import { Field } from "@/shared/ui/Field";
import { Wordmark } from "@/shared/ui/Wordmark";

interface FormError {
  message: string;
  correlationId: string | null;
}

export function LoginScreen() {
  const router = useRouter();
  const setAccount = useSessionStore((state) => state.setAccount);
  const accountToken = useSessionStore((state) => state.accountToken);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<FormError | null>(null);

  useEffect(() => {
    if (accountToken) {
      router.replace("/profiles");
    }
  }, [accountToken, router]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    setPending(true);
    try {
      const response = await login(email.trim(), password);
      setAccount(response.access_token, response.account);
      router.push("/profiles");
    } catch (caught) {
      if (isApiError(caught)) {
        setError({
          message: caught.message,
          correlationId: caught.correlationId,
        });
      } else {
        setError({
          message: "Ocurrió un error inesperado. Inténtalo de nuevo.",
          correlationId: null,
        });
      }
    } finally {
      setPending(false);
    }
  };

  const fillDemoAccount = () => {
    setEmail("ana@example.com");
    setPassword("S3cret-pass");
    setError(null);
  };

  return (
    <main
      id="main"
      className="grid min-h-dvh lg:grid-cols-[1.08fr_0.92fr]"
    >
      <section className="screen-grain relative hidden flex-col justify-between overflow-hidden border-r border-line px-14 py-12 lg:flex">
        <Wordmark className="text-3xl" />

        <div className="relative max-w-lg animate-rise">
          <p className="text-sm text-brass">La función de las 8</p>
          <p className="mt-4 font-display text-6xl leading-[0.94] text-paper">
            La noche empieza en casa.
          </p>
          <p className="mt-6 max-w-md text-base leading-relaxed text-paper-muted">
            Tu cartelera, tu historial y lo que sigue, listos antes de que te
            acomodes en el sofá.
          </p>
        </div>

        <div className="flex items-center gap-6 text-xs text-paper-muted">
          <span>Perfiles para toda la familia</span>
          <span className="h-px w-8 bg-line" aria-hidden="true" />
          <span>Seguir viendo en cualquier pantalla</span>
        </div>

        <span
          aria-hidden="true"
          className="pointer-events-none absolute -right-16 bottom-10 font-display text-[18rem] leading-none text-paper/[0.03]"
        >
          8
        </span>
      </section>

      <section className="flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm">
          <Wordmark className="text-2xl lg:hidden" />

          <h1 className="mt-8 font-display text-4xl text-paper lg:mt-0">
            Entrar
          </h1>
          <p className="mt-2 text-sm text-paper-muted">
            Usa la cuenta principal de StreamHub.
          </p>

          <form className="mt-8 flex flex-col gap-6" onSubmit={handleSubmit}>
            <Field
              label="Correo electrónico"
              type="email"
              name="email"
              autoComplete="email"
              autoCapitalize="none"
              spellCheck={false}
              inputMode="email"
              placeholder="ana@example.com"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              required
            />

            <div className="relative">
              <Field
                label="Contraseña"
                type={showPassword ? "text" : "password"}
                name="password"
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                inputClassName="pr-20"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((value) => !value)}
                className="absolute top-7 right-0 min-h-11 text-xs font-medium text-paper-muted transition-colors hover:text-paper"
                aria-pressed={showPassword}
              >
                {showPassword ? "Ocultar" : "Mostrar"}
              </button>
            </div>

            {error ? (
              <ErrorNotice
                message={error.message}
                correlationId={error.correlationId}
              />
            ) : null}

            <Button type="submit" disabled={pending} className="w-full">
              {pending ? "Entrando…" : "Entrar"}
            </Button>
          </form>

          <div className="mt-8 border-t border-line pt-4">
            <p className="text-xs text-paper-muted">Cuenta de demostración</p>
            <button
              type="button"
              onClick={fillDemoAccount}
              className="mt-1 min-h-11 text-left text-sm text-paper transition-colors hover:text-brass"
            >
              ana@example.com · S3cret-pass
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}
