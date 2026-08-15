import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // relative asset paths so the built SPA also works when served by
  // Spring Boot under the context path /gecko
  base: './',
  server: {
    proxy: {
      // Backend: Spring Boot app with context path /gecko on port 8080
      '/gecko': {
        target: 'http://localhost:8080',
        ws: true,
      },
    },
  },
});
