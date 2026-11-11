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
    <main id="main" className="grid min-h-dvh lg:grid-cols-[1.1fr_0.9fr]">
      <section className="pizarra-zona relative hidden flex-col justify-between overflow-hidden bg-pizarra px-14 py-12 text-tiza lg:flex">
        <div
          aria-hidden="true"
          className="absolute inset-0"
          style={{
            backgroundImage:
              "radial-gradient(90% 80% at 100% 0%, rgba(36, 51, 232, 0.55), transparent 62%), linear-gradient(rgba(241, 243, 255, 0.05) 1px, transparent 1px), linear-gradient(90deg, rgba(241, 243, 255, 0.05) 1px, transparent 1px)",
            backgroundSize: "auto, 32px 32px, 32px 32px",
          }}
        />

        <Wordmark className="relative text-4xl" tone="dark" />

        <div className="relative max-w-xl animate-rise">
          <p className="text-sm font-medium text-tiza/70">La función de las 8</p>
          <p className="titular mt-4 text-8xl text-tiza">
            La noche empieza en casa.
          </p>
          <p className="mt-6 max-w-md text-base leading-relaxed text-tiza/75">
            Tu cartelera, tu historial y tus clases grabadas, listos antes de
            que te acomodes en el sofá.
          </p>
        </div>

        <div className="relative">
          <div className="flex items-center" aria-hidden="true">
            <span className="h-[3px] w-24 bg-marcador" />
            <span className="h-px flex-1 bg-tiza/25" />
          </div>
          <p className="mt-3 text-xs text-tiza/70">
            Perfiles para toda la familia. Seguir viendo en cualquier pantalla.
          </p>
        </div>
      </section>

      <section className="papel flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-sm rounded-hoja border border-regla bg-hoja-2 p-8">
          <Wordmark className="text-3xl lg:hidden" />

          <h1 className="titular mt-8 text-5xl text-tinta lg:mt-0">Entrar</h1>
          <p className="mt-3 text-sm text-tinta-suave">
            Usa la cuenta principal de StreamHub.
          </p>

          <form className="mt-8 flex flex-col gap-5" onSubmit={handleSubmit}>
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
                inputClassName="pr-24"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((value) => !value)}
                className="absolute top-7 right-1 min-h-11 px-2 text-xs font-medium text-azul transition-colors duration-150 hover:text-azul-fuerte"
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

          <div className="mt-8 border-t border-regla pt-4">
            <p className="text-xs text-tinta-tenue">Cuenta de demostración</p>
            <button
              type="button"
              onClick={fillDemoAccount}
              className="mt-1 min-h-11 text-left text-sm font-medium text-tinta transition-colors duration-150 hover:text-azul"
            >
              ana@example.com · S3cret-pass
            </button>
          </div>
        </div>
      </section>
    </main>
  );
}
