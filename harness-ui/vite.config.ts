import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';
export default defineConfig({plugins:[vue()],build:{lib:{entry:'src/index.ts',formats:['es'],fileName:'harness-ui'},rollupOptions:{external:['vue'],output:{globals:{vue:'Vue'}}}},test:{environment:'jsdom'}});
