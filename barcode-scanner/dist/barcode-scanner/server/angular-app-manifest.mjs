
export default {
  bootstrap: () => import('./main.server.mjs').then(m => m.default),
  inlineCriticalCss: true,
  baseHref: '/',
  locale: undefined,
  routes: undefined,
  entryPointToBrowserMapping: {},
  assets: {
    'index.csr.html': {size: 34733, hash: 'dc002d92dc86d5c6bf09afe24bdcf15144026510ceba0cacd07025327e5ca851', text: () => import('./assets-chunks/index_csr_html.mjs').then(m => m.default)},
    'index.server.html': {size: 17119, hash: '0be53f4e9745e59fc4ad317b5ec1cca7913acc81c6051a8455ade7d21a536a1e', text: () => import('./assets-chunks/index_server_html.mjs').then(m => m.default)},
    'styles-ZD2BD4Z6.css': {size: 21621, hash: 'oZV9VMBmvcM', text: () => import('./assets-chunks/styles-ZD2BD4Z6_css.mjs').then(m => m.default)}
  },
};
