// vite.config.js
import { defineConfig } from "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/node_modules/vite/dist/node/index.js";
import vue from "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/node_modules/@vitejs/plugin-vue/dist/index.mjs";
import AutoImport from "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/node_modules/unplugin-auto-import/dist/vite.js";
import Components from "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/node_modules/unplugin-vue-components/dist/vite.js";
import { ElementPlusResolver } from "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/node_modules/unplugin-vue-components/dist/resolvers.js";
import { fileURLToPath, URL } from "node:url";
var __vite_injected_original_import_meta_url = "file:///D:/AtGuiGu_Java+AI/code/schoolmate_book/schoolmate-web/vite.config.js";
var vite_config_default = defineConfig({
  plugins: [
    vue(),
    // Element Plus 按需自动引入
    AutoImport({ resolvers: [ElementPlusResolver()] }),
    Components({ resolvers: [ElementPlusResolver()] })
  ],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", __vite_injected_original_import_meta_url))
    }
  },
  server: {
    port: 5173,
    open: false,
    proxy: {
      // 开发期通过代理访问后端，规避跨域
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true
      }
    }
  }
});
export {
  vite_config_default as default
};
//# sourceMappingURL=data:application/json;base64,ewogICJ2ZXJzaW9uIjogMywKICAic291cmNlcyI6IFsidml0ZS5jb25maWcuanMiXSwKICAic291cmNlc0NvbnRlbnQiOiBbImNvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9kaXJuYW1lID0gXCJEOlxcXFxBdEd1aUd1X0phdmErQUlcXFxcY29kZVxcXFxzY2hvb2xtYXRlX2Jvb2tcXFxcc2Nob29sbWF0ZS13ZWJcIjtjb25zdCBfX3ZpdGVfaW5qZWN0ZWRfb3JpZ2luYWxfZmlsZW5hbWUgPSBcIkQ6XFxcXEF0R3VpR3VfSmF2YStBSVxcXFxjb2RlXFxcXHNjaG9vbG1hdGVfYm9va1xcXFxzY2hvb2xtYXRlLXdlYlxcXFx2aXRlLmNvbmZpZy5qc1wiO2NvbnN0IF9fdml0ZV9pbmplY3RlZF9vcmlnaW5hbF9pbXBvcnRfbWV0YV91cmwgPSBcImZpbGU6Ly8vRDovQXRHdWlHdV9KYXZhK0FJL2NvZGUvc2Nob29sbWF0ZV9ib29rL3NjaG9vbG1hdGUtd2ViL3ZpdGUuY29uZmlnLmpzXCI7aW1wb3J0IHsgZGVmaW5lQ29uZmlnIH0gZnJvbSAndml0ZSdcbmltcG9ydCB2dWUgZnJvbSAnQHZpdGVqcy9wbHVnaW4tdnVlJ1xuaW1wb3J0IEF1dG9JbXBvcnQgZnJvbSAndW5wbHVnaW4tYXV0by1pbXBvcnQvdml0ZSdcbmltcG9ydCBDb21wb25lbnRzIGZyb20gJ3VucGx1Z2luLXZ1ZS1jb21wb25lbnRzL3ZpdGUnXG5pbXBvcnQgeyBFbGVtZW50UGx1c1Jlc29sdmVyIH0gZnJvbSAndW5wbHVnaW4tdnVlLWNvbXBvbmVudHMvcmVzb2x2ZXJzJ1xuaW1wb3J0IHsgZmlsZVVSTFRvUGF0aCwgVVJMIH0gZnJvbSAnbm9kZTp1cmwnXG5cbmV4cG9ydCBkZWZhdWx0IGRlZmluZUNvbmZpZyh7XG4gIHBsdWdpbnM6IFtcbiAgICB2dWUoKSxcbiAgICAvLyBFbGVtZW50IFBsdXMgXHU2MzA5XHU5NzAwXHU4MUVBXHU1MkE4XHU1RjE1XHU1MTY1XG4gICAgQXV0b0ltcG9ydCh7IHJlc29sdmVyczogW0VsZW1lbnRQbHVzUmVzb2x2ZXIoKV0gfSksXG4gICAgQ29tcG9uZW50cyh7IHJlc29sdmVyczogW0VsZW1lbnRQbHVzUmVzb2x2ZXIoKV0gfSlcbiAgXSxcbiAgcmVzb2x2ZToge1xuICAgIGFsaWFzOiB7XG4gICAgICAnQCc6IGZpbGVVUkxUb1BhdGgobmV3IFVSTCgnLi9zcmMnLCBpbXBvcnQubWV0YS51cmwpKVxuICAgIH1cbiAgfSxcbiAgc2VydmVyOiB7XG4gICAgcG9ydDogNTE3MyxcbiAgICBvcGVuOiBmYWxzZSxcbiAgICBwcm94eToge1xuICAgICAgLy8gXHU1RjAwXHU1M0QxXHU2NzFGXHU5MDFBXHU4RkM3XHU0RUUzXHU3NDA2XHU4QkJGXHU5NUVFXHU1NDBFXHU3QUVGXHVGRjBDXHU4OUM0XHU5MDdGXHU4REU4XHU1N0RGXG4gICAgICAnL2FwaSc6IHtcbiAgICAgICAgdGFyZ2V0OiAnaHR0cDovL2xvY2FsaG9zdDo4MDgwJyxcbiAgICAgICAgY2hhbmdlT3JpZ2luOiB0cnVlXG4gICAgICB9XG4gICAgfVxuICB9XG59KVxuIl0sCiAgIm1hcHBpbmdzIjogIjtBQUE4VixTQUFTLG9CQUFvQjtBQUMzWCxPQUFPLFNBQVM7QUFDaEIsT0FBTyxnQkFBZ0I7QUFDdkIsT0FBTyxnQkFBZ0I7QUFDdkIsU0FBUywyQkFBMkI7QUFDcEMsU0FBUyxlQUFlLFdBQVc7QUFMMEwsSUFBTSwyQ0FBMkM7QUFPOVEsSUFBTyxzQkFBUSxhQUFhO0FBQUEsRUFDMUIsU0FBUztBQUFBLElBQ1AsSUFBSTtBQUFBO0FBQUEsSUFFSixXQUFXLEVBQUUsV0FBVyxDQUFDLG9CQUFvQixDQUFDLEVBQUUsQ0FBQztBQUFBLElBQ2pELFdBQVcsRUFBRSxXQUFXLENBQUMsb0JBQW9CLENBQUMsRUFBRSxDQUFDO0FBQUEsRUFDbkQ7QUFBQSxFQUNBLFNBQVM7QUFBQSxJQUNQLE9BQU87QUFBQSxNQUNMLEtBQUssY0FBYyxJQUFJLElBQUksU0FBUyx3Q0FBZSxDQUFDO0FBQUEsSUFDdEQ7QUFBQSxFQUNGO0FBQUEsRUFDQSxRQUFRO0FBQUEsSUFDTixNQUFNO0FBQUEsSUFDTixNQUFNO0FBQUEsSUFDTixPQUFPO0FBQUE7QUFBQSxNQUVMLFFBQVE7QUFBQSxRQUNOLFFBQVE7QUFBQSxRQUNSLGNBQWM7QUFBQSxNQUNoQjtBQUFBLElBQ0Y7QUFBQSxFQUNGO0FBQ0YsQ0FBQzsiLAogICJuYW1lcyI6IFtdCn0K
