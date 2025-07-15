import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  base: '/ui/',          // браузер ловит статику именно под /ui/
  plugins: [react()]
});
