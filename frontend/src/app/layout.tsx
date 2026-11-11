import type { Metadata, Viewport } from "next";
import { Fraunces, Instrument_Sans } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

const fraunces = Fraunces({
  subsets: ["latin"],
  variable: "--font-fraunces",
  display: "swap",
});

const instrument = Instrument_Sans({
  subsets: ["latin"],
  variable: "--font-instrument",
  display: "swap",
});

export const viewport: Viewport = {
  themeColor: "#0b0f14",
  colorScheme: "dark",
};

export const metadata: Metadata = {
  title: {
    default: "StreamHub — Primetime",
    template: "%s — StreamHub",
  },
  description:
    "StreamHub reúne a la familia frente a la pantalla: perfiles, historial y recomendaciones listas cuando llega la noche.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="es"
      data-scroll-behavior="smooth"
      className={`${fraunces.variable} ${instrument.variable} h-full antialiased`}
    >
      <body
        suppressHydrationWarning
        className="flex min-h-dvh flex-col bg-ink-950 text-paper"
      >
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-card focus:bg-brass focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-ink-950"
        >
          Saltar al contenido
        </a>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
