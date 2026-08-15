import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
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
