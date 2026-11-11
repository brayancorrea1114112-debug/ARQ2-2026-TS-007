import type { Metadata, Viewport } from "next";
import { Archivo, Geist } from "next/font/google";
import { Providers } from "./providers";
import "./globals.css";

const archivo = Archivo({
  subsets: ["latin"],
  axes: ["wdth"],
  variable: "--font-archivo",
  display: "swap",
});

const geist = Geist({
  subsets: ["latin"],
  variable: "--font-geist",
  display: "swap",
});

export const viewport: Viewport = {
  themeColor: "#f2f4f8",
  colorScheme: "light",
};

export const metadata: Metadata = {
  title: {
    default: "StreamHub — Primetime",
    template: "%s — StreamHub",
  },
  description:
    "StreamHub reúne a la familia frente a la pantalla: perfiles, historial, recomendaciones y tus clases grabadas.",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="es"
      data-scroll-behavior="smooth"
      className={`${archivo.variable} ${geist.variable} h-full antialiased`}
    >
      <body
        suppressHydrationWarning
        className="flex min-h-dvh flex-col bg-hoja text-tinta"
      >
        <a
          href="#main"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-hoja focus:bg-azul focus:px-4 focus:py-2 focus:text-sm focus:font-semibold focus:text-white"
        >
          Saltar al contenido
        </a>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
