import type { NextConfig } from "next";

const kongUrl = (
  process.env.KONG_INTERNAL_URL ?? "http://localhost:8000"
).replace(/\/+$/, "");

const nextConfig: NextConfig = {
  output: "standalone",
  allowedDevOrigins: ["172.27.16.1", "127.0.0.1"],
  async rewrites() {
    return [
      {
        source: "/api/v1/:path*",
        destination: `${kongUrl}/api/v1/:path*`,
      },
    ];
  },
  turbopack: {
    root: process.cwd(),
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
};

export default nextConfig;
