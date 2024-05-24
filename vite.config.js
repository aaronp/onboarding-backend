import { defineConfig } from "vite";
import scalaJSPlugin from "@scala-js/vite-plugin-scalajs";

export default defineConfig({
  base: './',
  plugins: [scalaJSPlugin()],
  build: {
    outDir: 'dist',
    lib: {
      entry: './js/target/scala-3.4.1/app-opt/main.js',
      name: 'Kind-Backend',
      fileName: (format) => `kind.${format}.js`
    }
  }
});