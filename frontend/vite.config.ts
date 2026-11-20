import react from "@vitejs/plugin-react";
import { defineConfig } from "vitest/config";

// Im Entwicklungsmodus läuft das Frontend auf Port 5173 und leitet
// /api und /ws an das Spring-Boot-Backend auf Port 8080 weiter.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": { target: "http://localhost:8080", changeOrigin: true },
      "/ws": { target: "http://localhost:8080", ws: true, changeOrigin: true },
    },
  },
  test: {
    environment: "node",
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
