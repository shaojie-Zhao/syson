import react from '@vitejs/plugin-react';
import { defineConfig, loadEnv } from 'vite';
import fs from 'fs';

const commitHash = require('child_process').execSync('git rev-parse --short HEAD').toString();

// Patch Sirius Gantt bundle after Vite pre-bundling: Chinese date-fns locale + enable context menu
function patchGanttBundle(filePath) {
  if (!fs.existsSync(filePath)) return;
  let code = fs.readFileSync(filePath, 'utf8');
  const original = code;

  const patches = [
    // Chinese date-fns locale
    ['code:"en-US"','code:"zh-CN"'],
    ['"Jan"','"1月"'],['"Feb"','"2月"'],['"Mar"','"3月"'],['"Apr"','"4月"'],
    ['"May"','"五月"'],['"Jun"','"六月"'],['"Jul"','"七月"'],['"Aug"','"八月"'],
    ['"Sep"','"九月"'],['"Oct"','"十月"'],['"Nov"','"十一月"'],['"Dec"','"十二月"'],
    ['"January"','"一月"'],['"February"','"二月"'],['"March"','"三月"'],['"April"','"四月"'],
    ['"June"','"六月"'],['"July"','"七月"'],['"August"','"八月"'],
    ['"September"','"九月"'],['"October"','"十月"'],['"November"','"十一月"'],['"December"','"十二月"'],
    ['"Sun"','"周日"'],['"Mon"','"周一"'],['"Tue"','"周二"'],['"Wed"','"周三"'],
    ['"Thu"','"周四"'],['"Fri"','"周五"'],['"Sat"','"周六"'],
    ['"Sunday"','"星期日"'],['"Monday"','"星期一"'],['"Tuesday"','"星期二"'],
    ['"Wednesday"','"星期三"'],['"Thursday"','"星期四"'],['"Friday"','"星期五"'],['"Saturday"','"星期六"'],
    ['"Su"','"日"'],['"Mo"','"一"'],['"Tu"','"二"'],['"We"','"三"'],['"Th"','"四"'],['"Fr"','"五"'],['"Sa"','"六"'],
    // Enable Gantt context menu
    ['enableTableListContextMenu = false','enableTableListContextMenu = true'],
  ];

  for (const [s, r] of patches) {
    while (code.includes(s) && !code.includes(r)) code = code.replace(s, r);
  }

  if (code !== original) {
    fs.writeFileSync(filePath, code);
    console.log('[gantt-patch] Applied', patches.length, 'transformations');
  }
}

function siriusGanttPatchPlugin() {
  const bundlePath = 'node_modules/.vite/deps/@eclipse-sirius_sirius-web-application.js';
  return {
    name: 'sirius-gantt-patch',
    configureServer() {
      const doPatch = () => {
        setTimeout(() => patchGanttBundle(bundlePath), 3000);
      };
      doPatch();
      fs.watchFile(bundlePath, () => setTimeout(() => patchGanttBundle(bundlePath), 500));
    },
  };
}

export default defineConfig(({ mode }) => ({
  plugins: [siriusGanttPatchPlugin(), react()],
  build: {
    minify: mode !== 'development',
  },
  test: {
    environment: 'jsdom',
    coverage: {
      reporter: ['text', 'html'],
    },
  },
  server: {
    proxy: {
      '/api': {
        target: 'http://192.168.3.111:8080',
        changeOrigin: true,
      },
      '/images': {
        target: 'http://192.168.3.111:8080',
        changeOrigin: true,
      },
      '/icons': {
        target: 'http://192.168.3.111:8080',
        changeOrigin: true,
      },
      '/subscriptions': {
        target: 'ws://192.168.3.111:8080',
        ws: true,
        changeOrigin: true,
      },
    },
  },
  //We define the process.env to avoid 'Uncaught ReferenceError: process is not defined'.
  //Dependencies (such as react-trello) might expect environment variables to be defined (REDUX_LOGGING in this case).
  define: {
    'process.env': { ...process.env, ...loadEnv(mode, process.cwd()) },
    'import.meta.env.VITE_APP_VERSION': JSON.stringify(process.env.npm_package_version),
    'import.meta.env.COMMIT_HASH': JSON.stringify(commitHash.trim()),
  },
}));
